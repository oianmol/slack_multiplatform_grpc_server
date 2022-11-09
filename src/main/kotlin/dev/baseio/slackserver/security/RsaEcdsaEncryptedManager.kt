package dev.baseio.slackserver.security

import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.signature.PublicKeySignFactory
import com.google.protobuf.InvalidProtocolBufferException
import capillary.kmp.*
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec


class RsaEcdsaEncryptedManager(senderSigningKey: InputStream) : EncryptedManager() {
    private var senderSigner: PublicKeySign? = null

    /**
     * Constructs a new RSA-ECDSA EncrypterManager.
     */
    init {
        val signingKeyHandle = CleartextKeysetHandle
            .read(BinaryKeysetReader.withInputStream(senderSigningKey))
        senderSigner = PublicKeySignFactory.getPrimitive(signingKeyHandle)
        senderSigningKey.close()
    }

    override fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt {
        val wrappedRsaEcdsaPublicKey: WrappedRsaEcdsaPublicKey = try {
            WrappedRsaEcdsaPublicKey.parseFrom(publicKey)
        } catch (e: InvalidProtocolBufferException) {
            throw GeneralSecurityException("unable to parse public key", e)
        }
        val recipientPublicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(wrappedRsaEcdsaPublicKey.keybytesList.map { it.byte.toByte() }.toByteArray())
        )
        return RsaEcdsaHybridEncrypt.Builder()
            .withSenderSigner(senderSigner)
            .withRecipientPublicKey(recipientPublicKey)
            .withPadding(RsaEcdsaConstants.Padding.OAEP)
            .build()
    }

}
