package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import dev.baseio.slackdata.common.*
import capillary.kmp.*
import dev.baseio.slackserver.data.models.SKUserPublicKey
import java.io.InputStream

abstract class EncryptedManager {
    private var encrypter: HybridEncrypt? = null
    private var isLoaded: Boolean = false
    fun loadPublicKey(publicKey: SKUserPublicKey) {
        encrypter = rawLoadPublicKey(publicKey.keyBytes)
        isLoaded = true
    }

    fun clearPublicKey() {
        isLoaded = false
        encrypter = null
    }

    fun encrypt(data: ByteArray?): ByteArray {
        if (!isLoaded) {
            throw Exception("public key is not loaded")
        }
        val ciphertext: ByteArray = encrypter!!.encrypt(data, null)

        return slackCiphertext {
            this@slackCiphertext.ciphertext.addAll(ciphertext.map { mapByte ->
                byteArrayElement {
                    byte = mapByte.toInt()
                }
            })
        }.toByteArray()
    }

    abstract fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt

    interface Factory {
        fun create(keyAlgorithm: capillary.kmp.KeyAlgorithm, ins: InputStream?): EncryptedManager
    }
}

class EncryptedManagerFactory : EncryptedManager.Factory {
    override fun create(keyAlgorithm: capillary.kmp.KeyAlgorithm, ins: InputStream?): EncryptedManager {
        return when (keyAlgorithm) {
            capillary.kmp.KeyAlgorithm.RSA_ECDSA -> {
                RsaEcdsaEncryptedManager(ins!!)
            }

            capillary.kmp.KeyAlgorithm.UNRECOGNIZED -> {
                throw RuntimeException("Requested and UNRECOGNIZED EncryptedManager")
            }
        }
    }

}
