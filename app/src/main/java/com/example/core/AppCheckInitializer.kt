package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"
private const val META_PREFS_NAME = "amar_hisab_appcheck_meta"
private const val KEY_VERIFIED_INTENT_TOKEN = "verified_intent_debug_token"

/**
 * Configures Firebase App Check without blocking sideloaded / real-device debug installs.
 *
 * - When the harness supplies `FIREBASE_APPCHECK_DEBUG_TOKEN` via launch Intent extras,
 *   it persists the secret synchronously via `.commit()` and installs [DebugAppCheckProviderFactory].
 * - When a debug APK runs directly on a user's phone WITHOUT a registered debug token in
 *   Intent extras, we intentionally skip installing [DebugAppCheckProviderFactory] so the SDK
 *   does not fabricate an unregistered random UUID that triggers "Firebase App Check token is invalid."
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val debugStorePrefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val metaPrefs = context.getSharedPreferences(META_PREFS_NAME, Context.MODE_PRIVATE)
            val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim().orEmpty()

            if (intentDebugToken.isNotEmpty()) {
                // Synchronous commit() ensures the debug secret is persisted before DebugAppCheckProviderFactory reads it.
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, intentDebugToken)
                    .commit()
                metaPrefs.edit()
                    .putString(KEY_VERIFIED_INTENT_TOKEN, intentDebugToken)
                    .commit()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "App Check debug secret configured from launch Intent.")
            } else {
                val savedVerifiedToken = metaPrefs.getString(KEY_VERIFIED_INTENT_TOKEN, "").orEmpty()
                val storedSecret = debugStorePrefs.getString(DEBUG_SECRET_KEY, "").orEmpty()
                if (savedVerifiedToken.isNotEmpty() && savedVerifiedToken == storedSecret) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d(TAG, "DebugAppCheckProviderFactory installed using previously verified token.")
                } else {
                    // Clear any stale auto-generated unregistered UUID so Firebase AI calls aren't rejected
                    if (storedSecret.isNotEmpty()) {
                        debugStorePrefs.edit().remove(DEBUG_SECRET_KEY).commit()
                    }
                    Log.i(
                        TAG,
                        "Skipping DebugAppCheckProviderFactory on standalone device (no registered debug token provided)."
                    )
                }
            }
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.w(TAG, "recaptcha_site_key resource string is blank; skipping App Check provider.")
                }
            } else {
                Log.i(TAG, "recaptcha_site_key not configured; skipping App Check provider.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
