package com.sonarfiles.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createSecretKey()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv // 12 bytes para GCM
        val encryptedData = cipher.doFinal(plainBytes)

        // Formato: [Tamanho IV (1 byte)] + [IV (12 bytes)] + [Texto Cifrado]
        val output = ByteArray(1 + iv.size + encryptedData.size)
        output[0] = iv.size.toByte()
        System.arraycopy(iv, 0, output, 1, iv.size)
        System.arraycopy(encryptedData, 0, output, 1 + iv.size, encryptedData.size)
        return output
    }

    fun decrypt(encryptedBytes: ByteArray): ByteArray {
        if (encryptedBytes.size < 13) {
            throw IllegalArgumentException("Dados cifrados inválidos ou corrompidos")
        }
        val ivSize = encryptedBytes[0].toInt() and 0xFF
        val iv = ByteArray(ivSize)
        System.arraycopy(encryptedBytes, 1, iv, 0, ivSize)

        val cipherTextSize = encryptedBytes.size - 1 - ivSize
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(encryptedBytes, 1 + ivSize, cipherText, 0, cipherTextSize)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), gcmSpec)
        return cipher.doFinal(cipherText)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "SonarFiles_MasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
