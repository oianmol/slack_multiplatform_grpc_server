package dev.baseio.slackserver.security

import dev.baseio.slackdata.securepush.KeyAlgorithm

interface EncryptedManager {
    fun loadPublicKey(publicKey: ByteArray?)
    fun clearPublicKey()
    fun encrypt(toByteArray: ByteArray?): ByteArray

    interface Factory {
        fun create(keyAlgorithm: KeyAlgorithm): EncryptedManager
    }
}

class EncryptedManagerFactory : EncryptedManager.Factory {
    override fun create(keyAlgorithm: KeyAlgorithm): EncryptedManager {
        return when (keyAlgorithm) {
            KeyAlgorithm.RSA_ECDSA -> {
                RsaEcdsaEncryptedManager()
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
