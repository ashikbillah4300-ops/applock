package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val secureRandom = SecureRandom()

    fun isSetupComplete(): Boolean =
        prefs.getBoolean(KEY_SETUP_COMPLETE, false) && hasPin()

    fun hasPin(): Boolean =
        !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()

    fun hasPattern(): Boolean =
        !prefs.getString(KEY_PATTERN_HASH, null).isNullOrEmpty()

    fun getPinLength(): Int =
        prefs.getInt(KEY_PIN_LENGTH, DEFAULT_PIN_LENGTH).coerceIn(4, 6)

    fun getSecurityQuestion(): String =
        prefs.getString(KEY_SECURITY_QUESTION, DEFAULT_SECURITY_QUESTION)
            ?: DEFAULT_SECURITY_QUESTION

    fun saveInitialSetup(
        pin: String,
        securityQuestion: String,
        securityAnswer: String,
        patternNodes: List<Int>? = null
    ) {
        savePin(pin)
        saveSecurityQuestion(securityQuestion, securityAnswer)
        if (!patternNodes.isNullOrEmpty()) {
            savePattern(patternNodes)
        }
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    fun savePin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) {
            "PIN must be 4 to 6 digits"
        }
        val salt = generateSalt()
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .apply()
    }

    fun verifyPin(candidate: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val candidateHash = pbkdf2(candidate, salt)
        return MessageDigest.isEqual(expectedHash, candidateHash)
    }

    fun savePattern(nodes: List<Int>) {
        require(nodes.size >= 4) { "Pattern must connect at least 4 nodes" }
        val serialized = nodes.joinToString("-")
        val salt = generateSalt()
        val hash = pbkdf2(serialized, salt)
        prefs.edit()
            .putString(KEY_PATTERN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PATTERN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPattern(nodes: List<Int>): Boolean {
        val saltB64 = prefs.getString(KEY_PATTERN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val candidateHash = pbkdf2(nodes.joinToString("-"), salt)
        return MessageDigest.isEqual(expectedHash, candidateHash)
    }

    fun saveSecurityQuestion(question: String, answer: String) {
        val normalized = normalizeAnswer(answer)
        val salt = generateSalt()
        val hash = pbkdf2(normalized, salt)
        prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question.trim())
            .putString(KEY_ANSWER_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_ANSWER_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val saltB64 = prefs.getString(KEY_ANSWER_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_ANSWER_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val candidateHash = pbkdf2(normalizeAnswer(answer), salt)
        return MessageDigest.isEqual(expectedHash, candidateHash)
    }

    private fun normalizeAnswer(answer: String): String =
        answer.trim().lowercase()

    private fun generateSalt(): ByteArray =
        ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }

    private fun pbkdf2(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "glasslock_secure_credentials"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_PATTERN_SALT = "pattern_salt"
        private const val KEY_PATTERN_HASH = "pattern_hash"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_ANSWER_SALT = "answer_salt"
        private const val KEY_ANSWER_HASH = "answer_hash"
        private const val DEFAULT_PIN_LENGTH = 4
        private const val DEFAULT_SECURITY_QUESTION = "What was the name of your first pet?"
        private const val SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256

        val PRESET_QUESTIONS = listOf(
            "What was the name of your first pet?",
            "What city were you born in?",
            "What is your mother's maiden name?",
            "What was the name of your elementary school?",
            "What is your favorite childhood book?"
        )
    }
}
