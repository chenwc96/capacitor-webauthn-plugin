    /*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.manulife.mim.plugins.webauthn.repository

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.getcapacitor.JSObject
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.tasks.Tasks
import com.manulife.mim.plugins.webauthn.decodeBase64
import org.json.JSONArray
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Works with the API, the local data store, and FIDO2 API.
 */
class AuthRepository(
    private val executor: Executor
) {

    companion object {
        private const val TAG = "AuthRepository"

        // Keys for SharedPreferences
        private const val PREFS_NAME = "auth"

        private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(
                    Executors.newFixedThreadPool(64)
                ).also { instance = it }
            }
        }
    }

    private var fido2ApiClient: Fido2ApiClient? = null
    fun setFido2APiClient(client: Fido2ApiClient?) {
        fido2ApiClient = client
    }

    /**
     * Create credential
     */
    fun createCredential(payload: JSObject): LiveData<PendingIntent?> {
        val result = MutableLiveData<PendingIntent>()
        executor.execute {
            fido2ApiClient?.let { client ->
                val data = parsePublicKeyCredentialCreationOptions(payload)
                val task = client.getRegisterPendingIntent(data)
                result.postValue(Tasks.await(task))
            }
        }
        return result
    }

    /**
     * Get Assertion
     */
    fun getAssertion(payload: JSObject): LiveData<PendingIntent?> {
        val result = MutableLiveData<PendingIntent?>()
        executor.execute {
            fido2ApiClient?.let { client ->
                val data = parsePublicKeyCredentialRequestOptions(payload);
                val task = client.getSignPendingIntent(data)
                result.postValue(Tasks.await(task))
            }
        }
        return result
    }

    private fun parsePublicKeyCredentialRequestOptions(
        payload: JSObject
    ): PublicKeyCredentialRequestOptions {
        var timeoutInSec = payload.getDouble("timeout")
        if (timeoutInSec != null) {
            timeoutInSec = timeoutInSec / 1000
        } else {
            timeoutInSec = 60.0
        }

        val builder = PublicKeyCredentialRequestOptions.Builder()
        builder.setChallenge(payload.getString("challenge")!!.decodeBase64())
        builder.setAllowList(parseCredentialDescriptors(payload.getJSONArray("allowCredentials")))
        builder.setRpId(payload.getString("rpId")!!)
        builder.setTimeoutSeconds(timeoutInSec)
        return builder.build()
    }

    private fun parseCredentialDescriptors(
        payloads: JSONArray
    ): List<PublicKeyCredentialDescriptor> {
        val list = mutableListOf<PublicKeyCredentialDescriptor>()
        for (i in 0 until payloads.length()) {
            val payload = payloads.getJSONObject(i)
            list.add(
                PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.PUBLIC_KEY.toString(),
                    payload.getString("id").decodeBase64(),
                    /* transports */ null
                )
            )
        }
        return list
    }

    private fun parsePublicKeyCredentialCreationOptions(
        payload: JSObject
    ): PublicKeyCredentialCreationOptions {
        var timeoutInSec = payload.getDouble("timeout")
        if (timeoutInSec == null) {
            timeoutInSec = timeoutInSec / 1000
        } else {
            timeoutInSec = 60.0
        }

        val builder = PublicKeyCredentialCreationOptions.Builder()
        builder.setUser(parseUser(payload.getJSObject("user")!!))
        builder.setChallenge(payload.getString("challenge")!!.decodeBase64())
        builder.setParameters(parseParameters(payload.getJSONArray("pubKeyCredParams")))
        builder.setTimeoutSeconds(timeoutInSec)
        builder.setExcludeList(parseCredentialDescriptors(payload.getJSONArray("excludeCredentials")))
        builder.setAuthenticatorSelection(parseSelection(payload.getJSObject("authenticatorSelection")!!))
        builder.setRp(parseRp(payload.getJSObject("rp")!!))
        return builder.build()
    }

    private fun parseRp(payload: JSObject): PublicKeyCredentialRpEntity {
        return PublicKeyCredentialRpEntity(payload.getString("id")!!,
            payload.getString("name")!!, /* icon */ null)
    }

    private fun parseSelection(payload: JSObject): AuthenticatorSelectionCriteria {
        val builder = AuthenticatorSelectionCriteria.Builder()
        val attachment = payload.getString("authenticatorAttachment")
        if (attachment != null) {
            builder.setAttachment(Attachment.fromString(attachment))
        }
        return builder.build()
    }

    private fun parseUser(payload: JSObject): PublicKeyCredentialUserEntity {
        return PublicKeyCredentialUserEntity(
            payload.getString("id")!!.decodeBase64(),
            payload.getString("name"),
            null, // icon
            payload.getString("displayName"),
        )
    }

    private fun parseParameters(payloads: JSONArray): List<PublicKeyCredentialParameters> {
        val parameters = mutableListOf<PublicKeyCredentialParameters>()
        for (i in 0 until payloads.length()) {
            val payload = payloads.getJSONObject(i)
            val type = payload.getString("type")
            val algo = payload.getInt("alg")
            try {
                parameters.add(PublicKeyCredentialParameters(type, algo))
            } catch (e: Exception) {
                Log.w(TAG, "unsupported algo is found for webauthn. skipping this algo ($algo)" )
            }

        }
        return parameters
    }
}
