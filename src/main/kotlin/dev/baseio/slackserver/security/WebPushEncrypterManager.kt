package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.apps.webpush.WebPushHybridEncrypt
import com.google.protobuf.InvalidProtocolBufferException
import dev.baseio.slackdata.securepush.WrappedWebPushPublicKey
import java.security.GeneralSecurityException

/**
 * An implementation of [EncryptedManager] that supports Web Push encryption.
 */
class WebPushEncryptedManager : EncryptedManager() {
    @Throws(GeneralSecurityException::class)
    override fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt {
        val wrappedWebPushPublicKey: WrappedWebPushPublicKey = try {
            WrappedWebPushPublicKey.parseFrom(publicKey)
        } catch (e: InvalidProtocolBufferException) {
            throw GeneralSecurityException("unable to parse public key", e)
        }
        return WebPushHybridEncrypt.Builder()
            .withAuthSecret(wrappedWebPushPublicKey.authSecretList.map { it.byte.toByte() }.toByteArray())
            .withRecipientPublicKey(wrappedWebPushPublicKey.keyBytesList.map { it.byte.toByte() }.toByteArray())
            .build()
    }
}