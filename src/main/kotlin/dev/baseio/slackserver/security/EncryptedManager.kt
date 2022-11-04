package dev.baseio.slackserver.security

interface EncryptedManager {
    fun loadPublicKey(publicKey: ByteArray?)
    fun clearPublicKey()
    fun encrypt(toByteArray: ByteArray?): ByteArray
}
