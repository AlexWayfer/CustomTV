package name.alexwayfer.customtv.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.ProviderException
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.Locale
import kotlin.time.Duration.Companion.days

/** Default hardware attestation, without device IDs, StrongBox requirements, or authentication prompts. */
internal class ActivityKeyStore : ActivitySigningKeys {
    private val store by lazy { KeyStore.getInstance(KEY_STORE).apply { load(null) } }

    override fun create(challenge: ByteArray): ActivitySigningKey {
        val alias = "customtv.activity.${UUID.randomUUID()}"
        try {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEY_STORE).apply {
                initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAttestationChallenge(challenge)
                    .build())
            }.generateKeyPair()
            return evidence(alias)
        } catch (error: ProviderException) {
            store.deleteEntry(alias)
            throw GeneralSecurityException("Hardware attestation unavailable", error)
        } catch (error: GeneralSecurityException) {
            store.deleteEntry(alias)
            throw error
        }
    }

    override fun exists(key: ActivitySigningKey): Boolean = store.getKey(key.alias, null) is PrivateKey

    override fun sign(key: ActivitySigningKey, message: String): String {
        val privateKey = store.getKey(key.alias, null) as? PrivateKey
            ?: throw GeneralSecurityException("Activity key unavailable")
        val bytes = Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey)
            update(message.toByteArray(Charsets.UTF_8))
            sign()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    override fun delete(key: ActivitySigningKey) {
        store.deleteEntry(key.alias)
    }

    private fun evidence(alias: String): ActivitySigningKey {
        val chain = store.getCertificateChain(alias)?.map { it as X509Certificate }.orEmpty()
        if (chain.size !in MIN_CERTIFICATES..MAX_CERTIFICATES) {
            throw GeneralSecurityException("No hardware attestation chain")
        }
        val keyId = MessageDigest.getInstance("SHA-256").digest(chain.first().publicKey.encoded)
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        val now = Instant.now().epochSecond
        // Old factory chains may already be expired under Google's factory-root policy. A freshly generated
        // key with such a chain still refreshes monthly; a future RKP expiration refreshes a day before it.
        val expiry = chain.minOf { it.notAfter.toInstant().epochSecond }
        val monthly = now + KEY_REFRESH.inWholeSeconds
        val renewAt = if (expiry > now) minOf(monthly, expiry - EXPIRY_MARGIN.inWholeSeconds) else monthly
        return ActivitySigningKey(alias, keyId, chain.map { Base64.getEncoder().encodeToString(it.encoded) }, renewAt)
    }

    private companion object {
        const val KEY_STORE = "AndroidKeyStore"
        const val MIN_CERTIFICATES = 2
        const val MAX_CERTIFICATES = 8
        val KEY_REFRESH = 30.days
        val EXPIRY_MARGIN = 1.days
    }
}
