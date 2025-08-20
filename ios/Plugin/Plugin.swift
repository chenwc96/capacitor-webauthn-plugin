import Foundation
import Capacitor
import PromiseKit
import LocalAuthentication

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(WebAuthn)
public class WebAuthn: CAPPlugin {

    var authClient: AuthClient?

    @objc override public func load() {
    }

    @objc func initialize(_ call: CAPPluginCall) {
        WAKLogger.debug("[initialize] start initializing auth handler")

        guard let authServerBaseURL = call.options["authServerBaseURL"] as? String else {
            WAKLogger.debug("Parameter [authServerBaseURL] must be provided")
            call.reject("Parameter [authServerBaseURL] must be provided", "500")
            return
        }

        guard let viewController = self.bridge?.viewController else {
            WAKLogger.debug("View controller not found")
            call.reject("View controller not found", "500")
            return
        }

        DispatchQueue.main.async {
            self.authClient = AuthClient(viewController: viewController,
                                         authServerBaseURL: authServerBaseURL)
            WAKLogger.debug("[initialize] finish initializing auth handler")
            call.success()
        }
    }
    
    private func mapWAKErrorToCode(_ wakError: WAKError) -> String {
        var code = "1000"
        switch wakError {
        case .authenticationFailed:
            code = "1001"
        case .userCancel:
            code = "1002"
        case .userFallback:
            code = "1003"
        case .systemCancel:
            code = "1004"
        case .passcodeNotSet:
            code = "1005"
        case .touchIDNotAvailable:
            code = "1006"
        case .touchIDNotEnrolled:
            code = "1007"
        case .touchIDLockout:
            code = "1008"
        case .appCancel:
            code = "1009"
        case .invalidContext:
            code = "1010"
        case .notInteractive:
            code = "1011"
        case .biometryLockout:
            code = "1012"
        case .biometryNotAvailable:
            code = "1013"
        case .biometryNotEnrolled:
            code = "1014"
        case .badData:
            code = "2001"
        case .badOperation:
            code = "2002"
        case .invalidState:
            code = "2003"
        case .constraint:
            code = "2004"
        case .cancelled:
            code = "2005"
        case .timeout:
            code = "2006"
        case .notAllowed:
            code = "2007"
        case .unsupported:
            code = "2008"
        case .unknown:
            code = "1000"
        }
        return code
    }
    
    private func mapLAErrorToCode(_ laError: LAError) -> String {
        var code = "1000"
        switch laError.code {
        case .authenticationFailed:
            code = "1001"
        case .userCancel:
            code = "1002"
        case .userFallback:
            code = "1003"
        case .systemCancel:
            code = "1004"
        case .passcodeNotSet:
            code = "1005"
        case .touchIDNotAvailable:
            code = "1006"
        case .touchIDNotEnrolled:
            code = "1007"
        case .touchIDLockout:
            code = "1008"
        case .appCancel:
            code = "1009"
        case .invalidContext:
            code = "1010"
        case .notInteractive:
            code = "1011"
        case .biometryLockout:
            code = "1012"
        case .biometryNotAvailable:
            code = "1013"
        case .biometryNotEnrolled:
            code = "1014"
        @unknown default:
            code = "1000"
        }
        return code
    }
    
    private func getBiometricsStateFromLA() throws -> String? {
        var error: NSError?
        let context = LAContext()
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            if let domainState = context.evaluatedPolicyDomainState {
                return domainState.base64EncodedString(options: Data.Base64EncodingOptions(rawValue: 0))
            }
        } else {
            if let err = error{
                throw err
            }
        }
        return nil
    }

    @objc func getBiometricsState(_ call: CAPPluginCall) {
        WAKLogger.debug("[biometricState] getting biometrics")
        var code = "1000"
        do {
            if let state = try getBiometricsStateFromLA() {
                call.success([ "state": state])
            } else {
                WAKLogger.debug("[getBiometricsState] unable to get biometrics state \(code)")
                call.reject("Unable to get biometrics state", code)
            }
        } catch {
            
            if let laError = error as? LAError {
                code = mapLAErrorToCode(laError)
                WAKLogger.debug("[getBiometricsState] unable to get biometrics state \(code) \(laError.localizedDescription)")
                call.reject(laError.localizedDescription, code, laError)
            } else {
                WAKLogger.debug("[getBiometricsState] unable to get biometrics state \(code)")
                call.reject(error.localizedDescription, code, error)
            }
        }
    }


    @objc func isEligible(_ call: CAPPluginCall) {
        WAKLogger.debug("[isEligible] checking for biometics enrollment")
        var code = "1000"
        do {
            if (try getBiometricsStateFromLA()) != nil {
                call.success([ "isEligible": true ])
            } else {
                WAKLogger.debug("[isEligible] unable to get biometrics state \(code)")
                call.reject("Unable to get biometrics state", code)
            }
        } catch {
            
            if let laError = error as? LAError {
                code = mapLAErrorToCode(laError)
                WAKLogger.debug("[isEligible] unable to get biometrics state \(code) \(laError.localizedDescription)")
                call.success([
                    "isEligible": false,
                    "error": [
                        "errorCode": code,
                        "errorMessage": laError.localizedDescription
                    ]
                ])
            } else {
                WAKLogger.debug("[isEligible] unable to get biometrics state \(code)")
                call.success([
                    "isEligible": false,
                    "error": [
                        "errorCode": code,
                        "errorMessage": error.localizedDescription
                    ]
                ])
            }
        }
    }

    @objc func createCredential(_ call: CAPPluginCall) {
        guard let payload = call.options["payload"] as? [String: Any] else {
            WAKLogger.debug("Parameter [payload] must be provided")
            call.reject("Parameter [payload] must be provided")
            return
        }

        guard let authClient = self.authClient else {
            WAKLogger.debug("Auth client is not found")
            call.reject("Auth client is not found", "500")
            return
        }

        authClient.createCredential(payload).done {
            call.success($0)
        }.catch { error in
            var code = "1000"
            if let wakError = error as? WAKError {
                code = self.mapWAKErrorToCode(wakError)
                WAKLogger.debug("Error ocurring when registering the user \(code) \(wakError.localizedDescription)")
                call.reject(wakError.localizedDescription, code, wakError)
            } else {
                WAKLogger.debug("Error ocurring when registering the user \(code) \(error.localizedDescription)")
                call.reject("Error ocurring when registering the user", code)
            }
        }
    }

    @objc func getAssertion(_ call: CAPPluginCall) {
        guard let payload = call.options["payload"] as? [String: Any] else {
            WAKLogger.debug("Parameter [payload] must be provided")
            call.reject("Parameter [payload] must be provided", "500")
            return
        }

        guard let authClient = self.authClient else {
            WAKLogger.debug("Auth client is not found")
            call.reject("Auth client is not found", "500")
            return
        }

        authClient.getAssertion(payload).done {
            call.success($0)
        }.catch { error in
            var code = "1000"
            if let wakError = error as? WAKError {
                code = self.mapWAKErrorToCode(wakError)
                WAKLogger.debug("Error ocurring when authenticating the user \(code) \(wakError.localizedDescription)")
                call.reject(wakError.localizedDescription, code, wakError)
            } else {
                WAKLogger.debug("Error ocurring when authenticating the user \(code) \(error.localizedDescription)")
                call.reject("Error ocurring when authenticating the user", code)
            }
        }
    }
}
