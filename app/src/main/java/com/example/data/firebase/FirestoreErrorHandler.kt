package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?
): RuntimeException {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerInfo = JSONArray().apply {
        currentUser?.providerData?.forEach { info ->
            put(
                JSONObject().apply {
                    put("providerId", info.providerId)
                    put("displayName", info.displayName ?: JSONObject.NULL)
                    put("email", info.email ?: JSONObject.NULL)
                    put("photoUrl", info.photoUrl?.toString() ?: JSONObject.NULL)
                }
            )
        }
    }
    val errInfo = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid ?: JSONObject.NULL)
                put("email", currentUser?.email ?: JSONObject.NULL)
                put("emailVerified", currentUser?.isEmailVerified ?: false)
                put("isAnonymous", currentUser?.isAnonymous ?: false)
                put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
                put("providerInfo", providerInfo)
            }
        )
    }
    val jsonString = errInfo.toString()
    Log.e("FirestoreError", jsonString)
    return RuntimeException(jsonString, error)
}
