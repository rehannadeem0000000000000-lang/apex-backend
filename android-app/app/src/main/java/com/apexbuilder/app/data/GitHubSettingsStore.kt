package com.apexbuilder.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.apexbuilder.app.model.GitHubSettings
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GitHubSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)

    fun load(): GitHubSettings {
        val encryptedToken = preferences.getString(KEY_TOKEN, "").orEmpty()
        val token = runCatching { decrypt(encryptedToken) }.getOrDefault("")
        return GitHubSettings(
            owner = preferences.getString(KEY_OWNER, "").orEmpty(),
            repository = preferences.getString(KEY_REPOSITORY, "").orEmpty(),
            branch = preferences.getString(KEY_BRANCH, "main").orEmpty(),
            workflowFile = preferences.getString(KEY_WORKFLOW, "build-apk.yml").orEmpty(),
            token = token,
        )
    }

    fun save(settings: GitHubSettings) {
        preferences.edit()
            .putString(KEY_OWNER, settings.owner.trim())
            .putString(KEY_REPOSITORY, settings.repository.trim())
            .putString(KEY_BRANCH, settings.branch.trim())
            .putString(KEY_WORKFLOW, settings.workflowFile.trim())
            .putString(KEY_TOKEN, encrypt(settings.token.trim()))
            .apply()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > IV_LENGTH) { "Stored token is invalid" }
        val iv = payload.copyOfRange(0, IV_LENGTH)
        val ciphertext = payload.copyOfRange(IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "apexbuilder.github-token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val KEY_OWNER = "owner"
        const val KEY_REPOSITORY = "repository"
        const val KEY_BRANCH = "branch"
        const val KEY_WORKFLOW = "workflow"
        const val KEY_TOKEN = "token"
    }
}