package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import dev.baseio.slackdata.protos.SKByteArrayElement
import dev.baseio.slackdata.protos.sKByteArrayElement
import dev.baseio.slackdata.securepush.*
import dev.baseio.slackserver.data.models.SKUserPublicKey
import java.io.InputStream

abstract class EncryptedManager {
    private var encrypter: HybridEncrypt? = null
    private var isLoaded: Boolean = false
    var slackPublicKey: SlackPublicKey? = null
    fun loadPublicKey(publicKey: SKUserPublicKey) {
        kotlin.runCatching {
            slackPublicKey = slackPublicKey {
                this.isauth = publicKey.isAuth
                this.keybytes.addAll(publicKey.keyBytes.map { it ->
                    sKByteArrayElement {
                        this.byte = it.toInt()
                    }
                })
            }
        }
        slackPublicKey?.let {
            encrypter = rawLoadPublicKey(publicKey.keyBytes)
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
            this@slackCiphertext.keychainuniqueid = slackPublicKey!!.keychainuniqueid
            this@slackCiphertext.keyserialnumber = slackPublicKey!!.serialnumber
            this@slackCiphertext.isauthkey = slackPublicKey!!.isauth
            this@slackCiphertext.ciphertext.addAll(ciphertext.toTypedArray().map { mapByte ->
                sKByteArrayElement {
                    byte = mapByte.toInt()
                }
            })
        }.toByteArray()
    }

    abstract fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt

    interface Factory {
        fun create(keyAlgorithm: KeyAlgorithm, ins: InputStream?): EncryptedManager
    }
}

class EncryptedManagerFactory : EncryptedManager.Factory {
    override fun create(keyAlgorithm: KeyAlgorithm, ins: InputStream?): EncryptedManager {
        return when (keyAlgorithm) {
            KeyAlgorithm.RSA_ECDSA -> {
                RsaEcdsaEncryptedManager(ins!!)
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
