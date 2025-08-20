//
//  AuthenicationHandler.swift
//  Plugin
//
//  Created by Sing Chan （陳振聲） on 8/7/2021.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation
import PromiseKit

extension String {

    func fromBase64URL() -> [UInt8]? {
        var base64 = self
        base64 = base64.replacingOccurrences(of: "-", with: "+")
        base64 = base64.replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 {
            base64 = base64.appending("=")
        }
        if let data = Data(base64Encoded: base64) {
            return data.bytes
        }
        return nil
    }
}

class AuthClient {
    let authenticator: InternalAuthenticator!
    let webAuthnClient: WebAuthnClient!
    let defaultTimeout: UInt64 = 60
    let minTimeout: UInt64 = 15

    init(viewController: UIViewController,
         authServerBaseURL: String) {

        let userConsentUI = UserConsentUI(viewController: viewController)
        self.authenticator = InternalAuthenticator(ui: userConsentUI)
        self.authenticator.customKeyName = "com.manulife.login"
        self.webAuthnClient = WebAuthnClient(origin: authServerBaseURL,
                                             authenticator: self.authenticator)
    }

    func createCredential(_ payload: [String: Any]) -> Promise<[String: Any]> {
        return self.parseRegisterRequestOptions(payload).then {
            self.webAuthnClient.create($0)
        }.then { credential in
            return self.parseRegisterResponse(credential)
        }
    }

    func getAssertion(_ payload: [String: Any]) -> Promise<[String: Any]> {
        return self.parseSigninRequestOptions(payload).then {
            self.webAuthnClient.get($0)
        }.then { assertion in
            return self.parseSigninResponse(assertion)
        }
    }

    private func parseRegisterRequestOptions(_ publicKey: [String: Any]) -> Promise<PublicKeyCredentialCreationOptions> {
        return Promise { seal in

            var options = PublicKeyCredentialCreationOptions()

            if let challenge = publicKey["challenge"] as? String {
                if let challengeBytes = challenge.fromBase64URL() {
                    options.challenge = challengeBytes
                }
            }
            if let user = publicKey["user"] as? [String: String] {
                options.user.id = Bytes.fromString(user["id"] ?? "")
                options.user.name = user["name"] ?? ""
                options.user.displayName = user["displayName"] ?? ""
            }

            if let rp = publicKey["rp"] as? [String: String] {
                options.rp.id = rp["id"] ?? ""
                options.rp.name = rp["name"] ?? ""
            }


            options.attestation = AttestationConveyancePreference(rawValue: publicKey["attestation"] as! String)!
            for params in publicKey["pubKeyCredParams"] as! [[String: Any]] {
                if let algo = COSEAlgorithmIdentifier(rawValue: params["alg"] as! Int) {
                    options.addPubKeyCredParam(alg: algo)
                }
            }

            if let authenticatorSelection = publicKey["authenticatorSelection"] as? [String: Any] {
                var authenticatorAttachment: AuthenticatorAttachment? = nil
                if let authenticatorAttachmentRawValue = authenticatorSelection["authenticatorAttachment"] as? String {
                    authenticatorAttachment = AuthenticatorAttachment.init(rawValue: authenticatorAttachmentRawValue)
                }

                var requireResidentKey = false
                if let requireResidentKeyValue = authenticatorSelection["requireResidentKey"] as? Bool {
                    requireResidentKey = requireResidentKeyValue
                }
                options.authenticatorSelection = AuthenticatorSelectionCriteria(
                    authenticatorAttachment: authenticatorAttachment,
                    requireResidentKey: requireResidentKey,
                    userVerification: UserVerificationRequirement(rawValue: authenticatorSelection["userVerification"] as! String)!)
            }

            if let timeout = publicKey["timeout"] as? UInt64 {
                let timeoutInSec = timeout / 1000
                options.timeout = timeoutInSec
                self.webAuthnClient.minTimeout = timeoutInSec
                self.webAuthnClient.defaultTimeout = timeoutInSec
            } else {
                self.webAuthnClient.minTimeout = minTimeout
                self.webAuthnClient.defaultTimeout = defaultTimeout
            }

            WAKLogger.debug("==========================================")
            WAKLogger.debug("rp.id: " + (options.rp.id ?? "nil"))
            WAKLogger.debug("user.id: " + Base64.encodeBase64URL(options.user.id))
            WAKLogger.debug("challenge: " + Base64.encodeBase64URL(options.challenge))
            WAKLogger.debug("==========================================")

            seal.fulfill(options)
        }
    }

    private func parseRegisterResponse(_ credential: WebAuthnClient.CreateResponse) -> Promise<[String: Any]> {
        return Promise { seal in

            WAKLogger.debug("==========================================")
            WAKLogger.debug("credentialId: " + credential.id)
            WAKLogger.debug("rawId: " + Base64.encodeBase64URL(credential.rawId))
            WAKLogger.debug("attestationObject: " + Base64.encodeBase64URL(credential.response.attestationObject))
            WAKLogger.debug("clientDataJSON: " + Base64.encodeBase64URL(credential.response.clientDataJSON.data(using: .utf8)!))
            WAKLogger.debug("==========================================")

            let params = [
                "type": "public-key",
                "id": credential.id,
                "rawId": Base64.encodeBase64URL(credential.rawId),
                "response": [
                    "attestationObject": Base64.encodeBase64URL(credential.response.attestationObject),
                    "clientDataJSON": Base64.encodeBase64URL(credential.response.clientDataJSON.data(using: .utf8)!)
                ]
            ] as [String: Any]

            seal.fulfill(params)
        }
    }

    private func parseSigninRequestOptions(_ publicKey: [String: Any]) -> Promise<PublicKeyCredentialRequestOptions> {
        return Promise { seal in
            var options = PublicKeyCredentialRequestOptions()

            if let challenge = publicKey["challenge"] as? String {
                if let challengeBytes = challenge.fromBase64URL() {
                    options.challenge = challengeBytes
                }
            }

            if let rpId = publicKey["rpId"] as? String {
                options.rpId = rpId
            }

            for params in publicKey["allowCredentials"] as! [[String: Any]] {
                if let credentialId = params["id"] as? String {
                    if let credentialIdBytes = credentialId.fromBase64URL() {
                        options.addAllowCredential(credentialId: credentialIdBytes, transports: [.internal_])
                    }
                }
            }

            if let timeout = publicKey["timeout"] as? UInt64 {
                let timeoutInSec = timeout / 1000
                options.timeout = timeoutInSec
                self.webAuthnClient.minTimeout = timeoutInSec
                self.webAuthnClient.defaultTimeout = timeoutInSec
            } else {
                self.webAuthnClient.minTimeout = minTimeout
                self.webAuthnClient.defaultTimeout = defaultTimeout
            }

            WAKLogger.debug("==========================================")
            WAKLogger.debug("challenge: " + Base64.encodeBase64URL(options.challenge))
            WAKLogger.debug("==========================================")

            seal.fulfill(options)
        }
    }

    private func parseSigninResponse(_ assertion: WebAuthnClient.GetResponse) -> Promise<[String: Any]> {
        return Promise { seal in

            WAKLogger.debug("==========================================")
            WAKLogger.debug("credentialId: " + assertion.id)
            WAKLogger.debug("rawId: " + Base64.encodeBase64URL(assertion.rawId))
            WAKLogger.debug("authenticatorData: " + Base64.encodeBase64URL(assertion.response.authenticatorData))
            WAKLogger.debug("signature: " + Base64.encodeBase64URL(assertion.response.signature))
            WAKLogger.debug("userHandle: " + Base64.encodeBase64URL(assertion.response.userHandle!))
            WAKLogger.debug("clientDataJSON: " + Base64.encodeBase64URL(assertion.response.clientDataJSON.data(using: .utf8)!))
            WAKLogger.debug("==========================================")

            let params = [
                "type": "public-key",
                "id": assertion.id,
                "rawId": Base64.encodeBase64URL(assertion.rawId),
                "response": [
                    "authenticatorData": Base64.encodeBase64URL(assertion.response.authenticatorData),
                    "clientDataJSON": Base64.encodeBase64URL(assertion.response.clientDataJSON.data(using: .utf8)!),
                    "signature": Base64.encodeBase64URL(assertion.response.signature),
                    "userHandle": Base64.encodeBase64URL(assertion.response.userHandle!)
                ]
            ] as [String: Any]

            seal.fulfill(params)
        }
    }
}
