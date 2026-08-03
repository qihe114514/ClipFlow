package com.qihe.clipflow.data.bilibili

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface BilibiliSessionRepository {
    fun session(): BilibiliSession?
    fun save(session: BilibiliSession)
    fun clear()
}

/** The Cookie and account cache share one encrypted value and never enter DataStore. */
object BilibiliSessionStore : BilibiliSessionRepository {
    private const val keyAlias = "clipflow_bilibili_session"
    private const val preferencesName = "clipflow_bilibili_secure_session"
    private const val sessionKey = "session"
    private const val ivSize = 12
    private val gson = Gson()

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    override fun session(): BilibiliSession? {
        val context = appContext ?: return null
        val encoded = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(sessionKey, null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            if (bytes.size <= ivSize) return null
            val plain = cipher(Cipher.DECRYPT_MODE, bytes.copyOfRange(0, ivSize))
                .doFinal(bytes.copyOfRange(ivSize, bytes.size))
            gson.fromJson(String(plain, Charsets.UTF_8), BilibiliSession::class.java)
                ?.takeIf { it.cookie.isNotBlank() }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override fun save(session: BilibiliSession) {
        require(session.cookie.isNotBlank()) { "A Bilibili session requires a cookie" }
        val context = requireNotNull(appContext) { "BilibiliSessionStore is not initialized" }
        val encryptor = cipher(Cipher.ENCRYPT_MODE)
        val encrypted = encryptor.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(encryptor.iv + encrypted, Base64.NO_WRAP)
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().putString(sessionKey, value).apply()
    }

    override fun clear() {
        appContext?.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            ?.edit()?.remove(sessionKey)?.apply()
    }

    private fun cipher(mode: Int, iv: ByteArray? = null): Cipher {
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            if (iv == null) init(mode, secretKey()) else init(mode, secretKey(), GCMParameterSpec(128, iv))
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }
}
