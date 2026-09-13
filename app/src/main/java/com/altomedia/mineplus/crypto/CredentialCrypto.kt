package com.altomedia.mineplus.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts/decrypts NiceHash credentials before they touch disk.
 *
 * ```
 * Android Keystore             <- AES-256 key held by the OS keystore
 *        |
 * Encrypted configuration      <- AES/GCM ciphertext (base64, "enc:v1:" prefix)
 *        |
 * DataStore                    <- only ciphertext is persisted
 * ```
 *
 * Keys never leave the AndroidKeyStore, so an attacker with file access
 * cannot recover the plaintext credentials without the device keystore.
 */
object CredentialCrypto {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "mineplus_credential_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PREFIX = "enc:v1:"

    /** Encrypts [plaintext] for storage. Empty values are left untouched. */
    @Synchronized
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val blob = ByteArray(1 + iv.size + cipherText.size)
        blob[0] = iv.size.toByte()
        iv.copyInto(blob, 1)
        cipherText.copyInto(blob, 1 + iv.size)
        return PREFIX + Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    /**
     * Decrypts a value persisted by [encrypt]. Values without the "enc:v1:"
     * prefix are returned unchanged so legacy plaintext data (written before
     * this feature) still reads back correctly and gets re-encrypted on the
     * next save.
     */
    @Synchronized
    fun decrypt(value: String): String {
        if (!value.startsWith(PREFIX)) return value
        val blob = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        if (blob.size < 2) return value
        val ivSize = blob[0].toInt() and 0xFF
        if (ivSize < 12 || 1 + ivSize >= blob.size) return value
        val iv = blob.copyOfRange(1, 1 + ivSize)
        val cipherText = blob.copyOfRange(1 + ivSize, blob.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            // Corrupted/tampered blob or key unavailable — fall back to raw value.
            value
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}