package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.PublicKeySign
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.PublicKey
import javax.crypto.spec.OAEPParameterSpec

class RsaEcdsaHybridEncrypt private constructor(builder: Builder) : HybridEncrypt {
    private val senderSigner: PublicKeySign?
    private val recipientPublicKey: PublicKey?
    private val padding: RsaEcdsaConstants.Padding?
    private val oaepParameterSpec: OAEPParameterSpec?

    /**
     * Builder for [RsaEcdsaHybridEncrypt].
     */
    class Builder
    /**
     * Create a new builder.
     */
    {
        var senderSigner: PublicKeySign? = null
        var recipientPublicKey: PublicKey? = null
        var padding: RsaEcdsaConstants.Padding? = null
        var oaepParameterSpec : OAEPParameterSpec? = RsaEcdsaConstants.OAEP_PARAMETER_SPEC

        /**
         * Sets the ECDSA signature creation primitive of the sender.
         *
         * @param val the Tink ECDSA signer.
         * @return the builder.
         */
        fun withSenderSigner(`val`: PublicKeySign?): Builder {
            senderSigner = `val`
            return this
        }

        /**
         * Sets the RSA public key of the receiver.
         *
         * @param val the RSA public key.
         * @return the builder.
         */
        fun withRecipientPublicKey(`val`: PublicKey?): Builder {
            recipientPublicKey = `val`
            return this
        }

        /**
         * Sets the RSA padding scheme to use.
         *
         * @param val the RSA padding scheme.
         * @return the builder.
         */
        fun withPadding(`val`: RsaEcdsaConstants.Padding?): Builder {
            padding = `val`
            return this
        }

        /**
         * Sets the [OAEPParameterSpec] for RSA OAEP padding.
         *
         *
         * Setting this parameter is optional. If it is not specified, `RsaEcdsaConstants.OAEP_PARAMETER_SPEC` will be used.
         *
         * @param val the [OAEPParameterSpec] instance.
         * @return the builder.
         */
        fun withOaepParameterSpec(spec: OAEPParameterSpec?): Builder {
            oaepParameterSpec = spec
            return this
        }

        /**
         * Creates the [RsaEcdsaHybridEncrypt] instance for this builder.
         *
         * @return the created [RsaEcdsaHybridEncrypt] instance.
         */
        fun build(): RsaEcdsaHybridEncrypt {
            return RsaEcdsaHybridEncrypt(this)
        }
    }

    init {
        requireNotNull(builder.senderSigner) { "must set sender's signer with Builder.withSenderSigner" }
        senderSigner = builder.senderSigner
        requireNotNull(builder.recipientPublicKey) { "must set recipient's public key with Builder.withRecipientPublicKeyBytes" }
        recipientPublicKey = builder.recipientPublicKey
        requireNotNull(builder.padding) { "must set padding with Builder.withPadding" }
        padding = builder.padding
        require(!(padding === RsaEcdsaConstants.Padding.OAEP && builder.oaepParameterSpec == null)) { "must set OAEP parameter spec with Builder.withOaepParameterSpec" }
        oaepParameterSpec = builder.oaepParameterSpec
    }

    @Throws(GeneralSecurityException::class)
    override fun encrypt(plaintext: ByteArray, contextInfo: ByteArray?): ByteArray {
        if (contextInfo != null) {
            throw GeneralSecurityException("contextInfo must be null because it is unused")
        }
        return try {
            val unsignedCiphertext = HybridRsaUtils.encrypt(plaintext, recipientPublicKey, padding, oaepParameterSpec)
            signAndSerialize(unsignedCiphertext)
        } catch (e: IOException) {
            throw GeneralSecurityException("encryption failed", e)
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun signAndSerialize(payload: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // Generate signature.
        val signature = senderSigner!!.sign(payload)
        // Write signature length.
        val signatureLengthBytes = ByteBuffer.allocate(RsaEcdsaConstants.SIGNATURE_LENGTH_BYTES_LENGTH)
        signatureLengthBytes.putInt(signature.size)
        outputStream.write(signatureLengthBytes.array())
        // Write signature.
        outputStream.write(signature)
        // Write payload.
        outputStream.write(payload)
        // Return serialized bytes.
        return outputStream.toByteArray()
    }
}
