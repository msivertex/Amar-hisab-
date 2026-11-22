package com.example.data.backup

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class DecryptedBackupPayload(
    val plainJson: String,
    val updatedAtMillis: Long
)

/**
 * Authenticated AES-256-GCM encryption for Amar Hisab Google Drive backups.
 *
 * Keys are derived deterministically from the signed-in user's normalized Gmail address
 * combined with an application-scoped domain context and a per-upload random 16-byte salt
 * using PBKDF2WithHmacSHA256 (120,000 iterations). This guarantees:
 * 1. Backups stored in Google Drive are always opaque ciphertext (never plaintext JSON).
 * 2. The same Gmail user can seamlessly auto-restore after reinstall or switching phones
 *    without needing a separate device-bound hardware Keystore key.
 * 3. Ciphertext is cryptographically bound to the owner's email via GCM Associated Data (AAD)
 *    so tampering or cross-account substitution fails authentication immediately.
 */
object BackupCrypto {
    private const val FORMAT_VERSION = "amar_hisab_vault_v1"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val MAX_PAYLOAD_BYTES = 15 * 1024 * 1024 // 15 MB safety ceiling

    // Domain separation pepper (not an API credential; binds derivation to Amar Hisab vault format).
    private const val APP_VAULT_DOMAIN = "com.aistudio.amarhisab.vault.drive.v1::aes256gcm"

    private val secureRandom = SecureRandom()

    fun normalizeEmail(userEmail: String): String =
        userEmail.trim().lowercase(Locale.ROOT)

    fun encrypt(
        plainJson: String,
        userEmail: String,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val normalizedEmail = normalizeEmail(userEmail)
        require(normalizedEmail.isNotBlank() && normalizedEmail.contains("@")) {
            "A valid Gmail address is required to encrypt Google Drive backups."
        }
        val plaintextBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        require(plaintextBytes.size <= MAX_PAYLOAD_BYTES) {
            "Backup payload exceeds maximum supported size."
        }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val secretKey = deriveAesKey(normalizedEmail, salt, KDF_ITERATIONS)
        val aad = buildAad(normalizedEmail)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintextBytes)

        val envelope = JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("alg", CIPHER_TRANSFORMATION)
            put("kdf", KDF_ALGORITHM)
            put("iterations", KDF_ITERATIONS)
            put("updatedAt", updatedAtMillis)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
        return envelope.toString()
    }

    fun decrypt(
        envelopeJson: String,
        userEmail: String
    ): DecryptedBackupPayload {
        val normalizedEmail = normalizeEmail(userEmail)
        require(normalizedEmail.isNotBlank() && normalizedEmail.contains("@")) {
            "A valid Gmail address is required to decrypt Google Drive backups."
        }
        require(envelopeJson.length <= MAX_PAYLOAD_BYTES * 2) {
            "Encrypted backup payload exceeds maximum supported size."
        }

        val root = JSONObject(envelopeJson)
        val format = root.optString("format")
        if (format != FORMAT_VERSION) {
            throw GeneralSecurityException("Unsupported encrypted backup format.")
        }
        val iterations = root.optInt("iterations", KDF_ITERATIONS)
        if (iterations !in 60_000..500_000) {
            throw GeneralSecurityException("Invalid key derivation parameters.")
        }

        val salt = Base64.decode(root.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(root.getString("ciphertext"), Base64.NO_WRAP)
        if (salt.size < 16 || iv.size != IV_BYTES || ciphertext.size < 16) {
            throw GeneralSecurityException("Corrupted encrypted backup envelope.")
        }

        val secretKey = deriveAesKey(normalizedEmail, salt, iterations)
        val aad = buildAad(normalizedEmail)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        val plaintextBytes = cipher.doFinal(ciphertext)
        val plainJson = String(plaintextBytes, StandardCharsets.UTF_8)
        val updatedAt = root.optLong("updatedAt", 0L)
        return DecryptedBackupPayload(plainJson = plainJson, updatedAtMillis = updatedAt)
    }

    private fun deriveAesKey(
        normalizedEmail: String,
        salt: ByteArray,
        iterations: Int
    ): SecretKeySpec {
        val secretInput = "$APP_VAULT_DOMAIN::$normalizedEmail".toCharArray()
        val spec = PBEKeySpec(secretInput, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
            secretInput.fill('\u0000')
        }
    }

    private fun buildAad(normalizedEmail: String): ByteArray =
        "amar-hisab-drive-v1:$normalizedEmail".toByteArray(StandardCharsets.UTF_8)
}
