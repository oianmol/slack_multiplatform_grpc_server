package dev.baseio.slackserver.security

import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadFactory
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import dev.baseio.slackdata.securepush.HybridRsaCiphertext
import dev.baseio.slackdata.securepush.byteArrayElement
import dev.baseio.slackdata.securepush.hybridRsaCiphertext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec

object HybridRsaUtils {
    private val SYMMETRIC_KEY_TEMPLATE = AeadKeyTemplates.AES128_GCM
    private val emptyEad = ByteArray(0)

    /**
     * Encrypts the given plaintext using RSA hybrid encryption.
     *
     * @param plaintext the plaintext to encrypt.
     * @param publicKey the RSA public key.
     * @param padding the RSA padding to use.
     * @param oaepParams the [OAEPParameterSpec] to use for OAEP padding.
     * @return the ciphertext.
     * @throws GeneralSecurityException if encryption fails.
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(
        plaintext: ByteArray?,
        publicKey: PublicKey?,
        padding: RsaEcdsaConstants.Padding?,
        oaepParams: OAEPParameterSpec?
    ): ByteArray {
        // Initialize RSA encryption cipher.
        val rsaCipher = Cipher.getInstance(padding?.transformation)
        if (padding === RsaEcdsaConstants.Padding.OAEP) {
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        } else {
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        }

        // Generate symmetric key and its ciphertext.
        val symmetricKeyHandle = KeysetHandle.generateNew(SYMMETRIC_KEY_TEMPLATE)
        val symmetricKeyOutputStream = ByteArrayOutputStream()
        try {
            CleartextKeysetHandle.write(
                symmetricKeyHandle, BinaryKeysetWriter.withOutputStream(symmetricKeyOutputStream)
            )
        } catch (e: IOException) {
            throw GeneralSecurityException("hybrid rsa encryption failed: ", e)
        }
        val symmetricKeyBytes = symmetricKeyOutputStream.toByteArray()
        val symmetricKeyCiphertext = rsaCipher.doFinal(symmetricKeyBytes)

        // Generate payload ciphertext.
        val aead = symmetricKeyHandle.getPrimitive(Aead::class.java)
        val payloadCiphertext = aead.encrypt(plaintext, emptyEad)
        return hybridRsaCiphertext {
            this@hybridRsaCiphertext.symmetricKeyCiphertext.addAll(symmetricKeyCiphertext.map { mapByte ->
                byteArrayElement {
                    this.byte = mapByte.toInt()
                }
            })
            this@hybridRsaCiphertext.payloadCiphertext.addAll(payloadCiphertext.map { mapByte ->
                byteArrayElement {
                    this.byte = mapByte.toInt()
                }
            })
        }.toByteArray()
    }

    /**
     * Decrypts the given ciphertext using RSA hybrid decryption.
     *
     * @param ciphertext the ciphertext to decrypt.
     * @param privateKey the RSA private key.
     * @param padding the RSA padding to use.
     * @param oaepParams the [OAEPParameterSpec] to use for OAEP padding.
     * @return the plaintext.
     * @throws GeneralSecurityException if decryption fails.
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(
        ciphertext: ByteArray?,
        privateKey: PrivateKey?,
        padding: RsaEcdsaConstants.Padding,
        oaepParams: OAEPParameterSpec?
    ): ByteArray {
        // Parse encrypted payload bytes.
        val hybridRsaCiphertext: HybridRsaCiphertext = try {
            HybridRsaCiphertext.parseFrom(ciphertext)
        } catch (e: InvalidProtocolBufferException) {
            throw GeneralSecurityException("hybrid rsa decryption failed: ", e)
        }

        // Initialize RSA decryption cipher.
        val rsaCipher = Cipher.getInstance(padding.transformation)
        if (padding === RsaEcdsaConstants.Padding.OAEP) {
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams)
        } else {
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        }

        // Retrieve symmetric key.
        val symmetricKeyCiphertext: ByteArray = hybridRsaCiphertext.symmetricKeyCiphertextList.map { it.byte.toByte() }.toByteArray()
        val symmetricKeyBytes = rsaCipher.doFinal(symmetricKeyCiphertext)
        val symmetricKeyHandle: KeysetHandle = try {
            CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(symmetricKeyBytes))
        } catch (e: IOException) {
            throw GeneralSecurityException("hybrid rsa decryption failed: ", e)
        }

        // Retrieve and return plaintext.
        val aead = symmetricKeyHandle.getPrimitive(Aead::class.java)
        val payloadCiphertext: ByteArray = hybridRsaCiphertext.payloadCiphertextList.map { it.byte.toByte() }.toByteArray()
        return aead.decrypt(payloadCiphertext, emptyEad)
    }
}