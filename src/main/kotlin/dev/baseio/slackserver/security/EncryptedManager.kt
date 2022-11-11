package dev.baseio.slackserver.security

import com.google.crypto.tink.HybridEncrypt
import dev.baseio.slackdata.common.*
import dev.baseio.slackdata.protos.slackPublicKey
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

        return slackPublicKey {
            this@slackPublicKey.keybytes.addAll(ciphertext.map { mapByte ->
                sKByteArrayElement {
                    byte = mapByte.toInt()
                }
            })
        }.toByteArray()
    }

    abstract fun rawLoadPublicKey(publicKey: ByteArray): HybridEncrypt

    interface Factory {
        fun create( ins: InputStream?): EncryptedManager
    }
}

class EncryptedManagerFactory : EncryptedManager.Factory {
    override fun create(ins: InputStream?): EncryptedManager {
        return RsaEcdsaEncryptedManager(ins!!)
    }

}
