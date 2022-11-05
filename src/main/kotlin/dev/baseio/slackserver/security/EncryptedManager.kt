package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import dev.baseio.slackdata.protos.sKByteArrayElement
import dev.baseio.slackdata.securepush.*
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.getKoin

abstract class EncryptedManager {
    private var encrypter: HybridEncrypt? = null
    private var isLoaded: Boolean = false
    var slackPublicKey: SlackPublicKey? = null
    fun loadPublicKey(publicKey: ByteArray?) {
        kotlin.runCatching {
            slackPublicKey = SlackPublicKey.parseFrom(publicKey)
        }
        slackPublicKey?.let {
            encrypter = rawLoadPublicKey(slackPublicKey!!.keyBytesList.map { it.byte.toByte() }.toByteArray())
            isLoaded = true
        }
    }

    fun clearPublicKey() {
        isLoaded = false
        slackPublicKey = null
        encrypter = null
    }

    fun encrypt(data: ByteArray?): ByteArray {
        if (!isLoaded) {
            throw Exception("public key is not loaded")
        }
        val ciphertext: ByteArray = encrypter!!.encrypt(data, null)

        return slackCiphertext {
            this@slackCiphertext.keychainUniqueId = slackPublicKey!!.keychainUniqueId
            this@slackCiphertext.keySerialNumber = slackPublicKey!!.serialNumber
            this@slackCiphertext.isAuthKey = slackPublicKey!!.isAuth
            this@slackCiphertext.ciphertext.addAll(ciphertext.toTypedArray().map { mapByte ->
                sKByteArrayElement {
                    byte = mapByte.toInt()
                }
            })
        }.toByteArray()
    }

    abstract fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt

    interface Factory {
        fun create(keyAlgorithm: KeyAlgorithm): EncryptedManager
    }
}

class EncryptedManagerFactory : EncryptedManager.Factory {
    override fun create(keyAlgorithm: KeyAlgorithm): EncryptedManager {
        return when (keyAlgorithm) {
            KeyAlgorithm.RSA_ECDSA -> {
                RsaEcdsaEncryptedManager(getKoin().get(named(RsaEcdsaConstants.FILE_INPUT_STREAM)))
            }

            KeyAlgorithm.WEB_PUSH -> {
                WebPushEncryptedManager()
            }

            KeyAlgorithm.UNRECOGNIZED -> {
                throw RuntimeException("Requested and UNRECOGNIZED EncryptedManager")
            }
        }
    }

}
