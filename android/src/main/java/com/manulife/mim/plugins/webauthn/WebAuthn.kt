package com.manulife.mim.plugins.webauthn

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.getcapacitor.NativePlugin
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.JSObject
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.*
import com.manulife.mim.plugins.webauthn.repository.AuthRepository
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


@NativePlugin(
    requestCodes = [ WebAuthn.REQUEST_FIDO2_REGISTER, WebAuthn.REQUEST_FIDO2_SIGNIN ]
)
class WebAuthn: Plugin() {

    companion object {
        private const val TAG = "WebAuthnPlugin"
        private const val KEY_ALIAS = "com.manulife.webauthn"
        private const val KEY_STORE = "AndroidKeyStore"
        private const val ENCRYPTION_ALGO = "AES/CBC/PKCS7Padding"
        const val REQUEST_FIDO2_REGISTER = 1000
        const val REQUEST_FIDO2_SIGNIN = 1001
    }

    override fun load() {
        // Called when the plugin is first constructed in the bridge
        Log.d(TAG, "Loading Android webauthn native plugin")
    }

    @PluginMethod
    fun initialize(call: PluginCall) {
        Log.d(TAG, "[Android][initialize] initialize")

        val repository = AuthRepository.getInstance(this.context)
        Log.d(TAG, "[Android][initialize] Setting api for the fido2 api client")
        repository.setFido2APiClient(Fido.getFido2ApiClient(this.context))
        call.success()
    }

    @PluginMethod
    fun isEligible(call: PluginCall) {
        Log.d(TAG, "[Android][isEligible] checking for fingerprint enrollment")

        val managerCompat = FingerprintManagerCompat.from(this.context)
        when {
            !managerCompat.isHardwareDetected -> {
                Log.e(TAG, "There is no fingerprint sensor hardware found.")
                call.reject("There is no fingerprint sensor hardware found.", "1006")
            }

            !managerCompat.hasEnrolledFingerprints() -> {
                Log.e(TAG, "There is no fingerprint registered on this device.")
                call.reject("There is no fingerprint registered on this device.", "1007")
            }

            else -> {
                val ret = JSObject()
                ret.put("isEligible", true)
                call.success(ret)
            }
        }
    }

    private fun createKeyAlias(): String {
        return KEY_ALIAS + "." + System.currentTimeMillis().toString()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun createKey(keyAlias: String): SecretKey {

        val KEY_SIZE_IN_BITS = 256
        val keySpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_IN_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        val keygen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE).apply {
            init(keySpec)
        }
        return keygen.generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun regenerateKeyPair(keystore: KeyStore, keyAlias: String?): String {
        try {
            keystore.deleteEntry(keyAlias)
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }

        val keyAlias = createKeyAlias()
        Log.d(TAG, "key alias: $keyAlias")
        createKey(keyAlias)
        return keyAlias!!.toByteArray().toBase64()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @PluginMethod
    fun getBiometricsState(call: PluginCall) {
        Log.d(TAG, "[Android][getBiometricsState] getting biometrics state")

        var previousState = call.getString("previousState")

        val state = JSObject()
        val keystore = KeyStore.getInstance(KEY_STORE)
            .apply { load(null) }
        var key: SecretKey? = null
        var keyAlias: String? = null
        try {
            if (previousState != null && !previousState.isEmpty()) {
                keyAlias = String(previousState.decodeBase64())
                Log.d(TAG, "key alias: $keyAlias")
                key = keystore.getKey(keyAlias, null) as SecretKey?
            }

            if (key == null) {
                keyAlias = createKeyAlias()
                Log.d(TAG, "key alias: $keyAlias")
                key = createKey(keyAlias)
            }

            Cipher.getInstance(ENCRYPTION_ALGO)
                .apply { init(Cipher.ENCRYPT_MODE, key) }

            val managerCompat = FingerprintManagerCompat.from(this.context)
            when {
                !managerCompat.hasEnrolledFingerprints() -> {
                    call.reject("There is no fingerprint registered on this device.", "1007")
                    return
                } else -> {
                    state.put("state", keyAlias!!.toByteArray().toBase64())
                }
            }

        } catch (e: UnrecoverableKeyException) {
            Log.d(TAG, "key is not recoverable")
            try {
                val keyState = regenerateKeyPair(keystore, keyAlias!!)
                state.put("state", keyState)
            } catch (e: InvalidAlgorithmParameterException) {
                call.reject(e.message, "1007")
                return
            } catch (e: Exception) {
                call.reject(e.message, "1000")
                return
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.d(TAG, "deleting existing and regenerating a new one")

            try {
                val keyState = regenerateKeyPair(keystore, keyAlias!!)
                state.put("state", keyState)
            } catch (e: InvalidAlgorithmParameterException) {
                call.reject(e.message, "1007")
                return
            } catch (e: Exception) {
                call.reject(e.message, "1000")
                return
            }

        } catch (e: InvalidAlgorithmParameterException) {
            call.reject(e.message, "1007")
            return
        } catch (e: Exception) {
            call.reject(e.message, "1000")
            return
        }
        call.success(state)
    }

    @PluginMethod
    fun createCredential(call: PluginCall) {

        saveCall(call)

        val payload = call.getObject("payload")
        Log.d(TAG, "[Android] [createCredential] start creating credential $payload")
        if (payload == null) {
            call.reject("Payload must be required", "2001")
            return
        }
        activity.runOnUiThread {
            val repository = AuthRepository.getInstance(this.context)
            repository.createCredential(payload).observeOnce(activity) { intent ->
                activity.startIntentSenderForResult(
                    intent.intentSender,
                    REQUEST_FIDO2_REGISTER,
                    null,
                    0,
                    0,
                    0,
                    null)
            }
        }
    }

    @PluginMethod
    fun getAssertion(call: PluginCall) {
        saveCall(call)

        val payload = call.getObject("payload")
        Log.d(TAG, "[Android] [getAssertion] start getting assertion $payload")
        if (payload == null) {
            call.reject("Payload must be required", "2001")
            return
        }

        activity.runOnUiThread {
            val repository = AuthRepository.getInstance(this.context)
            repository.getAssertion(payload).observeOnce(activity) { intent ->
                activity.startIntentSenderForResult(
                    intent.intentSender,
                    REQUEST_FIDO2_SIGNIN,
                    null,
                    0,
                    0,
                    0,
                    null)

            }
        }
    }

    override fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "[Android][handleOnActivityResult] getting activity result")
        if (requestCode == REQUEST_FIDO2_REGISTER) {
            if (savedCall == null) {
                Log.w(TAG, "saved call not find. unable to return response back the app")
                return
            }

            val errorExtra = data?.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
            when {
                errorExtra != null -> {
                    val error = AuthenticatorErrorResponse.deserializeFromBytes(errorExtra)
                    Log.e(TAG, "[$error.errorCode]$error.errorMessage")

                    when (error.errorCode) {
                        ErrorCode.NOT_ALLOWED_ERR -> {
                            savedCall.reject(error.errorMessage, "2005")
                            return
                        }
                        ErrorCode.TIMEOUT_ERR -> {
                            savedCall.reject(error.errorMessage, "2006")
                            return
                        }
                        else -> {
                            // Generic fallback for unhandled error codes
                            savedCall.reject("Unhandled error: ${error.errorMessage}", "1000")
                        }
                    }
                }
                resultCode != Activity.RESULT_OK -> {
                    savedCall.reject("Cancelled", "2005")
                }
                data != null -> {

                    val response = AuthenticatorAttestationResponse.deserializeFromBytes(
                        data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)!!
                    )

                    val rawId = response.keyHandle.toBase64()

                    val credential = JSObject()
                    credential.put("id", rawId)
                    credential.put("rawId", rawId)
                    credential.put("type", PublicKeyCredentialType.PUBLIC_KEY.toString())

                    val responseObj = JSObject()
                    responseObj.put("clientDataJSON", response.clientDataJSON.toBase64())
                    responseObj.put("attestationObject", response.attestationObject.toBase64())
                    credential.put("response", responseObj)

                    savedCall.success(credential)
                }
            }
        } else if (requestCode == REQUEST_FIDO2_SIGNIN) {
            if (savedCall == null) {
                Log.w(TAG, "saved call not find. unable to return response back the app")
                return
            }

            val errorExtra = data?.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
            when {
                errorExtra != null -> {
                    val error = AuthenticatorErrorResponse.deserializeFromBytes(errorExtra)
                    Log.e(TAG, "[$error.errorCode]$error.errorMessage")
                    when (error.errorCode) {
                        ErrorCode.NOT_ALLOWED_ERR -> {
                            savedCall.reject(error.errorMessage, "2005")
                            return
                        }
                        ErrorCode.TIMEOUT_ERR -> {
                            savedCall.reject(error.errorMessage, "2006")
                            return
                        }
                        else -> {
                            savedCall.reject(error.errorMessage, "1000")
                            return
                        }
                    }
                }

                resultCode != Activity.RESULT_OK -> {
                    savedCall.reject("Cancelled", "2005")
                    return
                }

                data != null -> {

                    val response = AuthenticatorAssertionResponse.deserializeFromBytes(
                        data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)

                    )
                    val rawId = response.keyHandle.toBase64()

                    val assertion = JSObject()
                    assertion.put("id", rawId)
                    assertion.put("rawId", rawId)
                    assertion.put("type", PublicKeyCredentialType.PUBLIC_KEY.toString())

                    val responseObj = JSObject()
                    responseObj.put("clientDataJSON", response.clientDataJSON.toBase64())
                    responseObj.put("authenticatorData", response.authenticatorData.toBase64())
                    responseObj.put("signature", response.signature.toBase64())
                    responseObj.put("userHandle", response.userHandle?.toBase64() ?: "")
                    assertion.put("response", responseObj)

                    savedCall.success(assertion)
                }
            }
        } else {
            super.handleOnActivityResult(requestCode, resultCode, data)
        }
    }
}
