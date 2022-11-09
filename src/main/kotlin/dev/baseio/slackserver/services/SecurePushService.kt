package dev.baseio.slackserver.services

import dev.baseio.slackdata.common.*
import capillary.kmp.*
import dev.baseio.slackserver.data.models.PLATFORM_ANDROID
import dev.baseio.slackserver.data.models.SKUserPublicKey
import dev.baseio.slackserver.data.models.SKUserPushToken
import dev.baseio.slackserver.data.sources.UserPublicKeysSource
import dev.baseio.slackserver.data.sources.UserPushTokenDataSource
import dev.baseio.slackserver.services.interceptors.AUTH_CONTEXT_KEY
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class SecurePushService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val userPushTokenDataSource: UserPushTokenDataSource,
    private val userPublicKeysSource: UserPublicKeysSource,
) : SecurePushServiceGrpcKt.SecurePushServiceCoroutineImplBase(coroutineContext) {
    override suspend fun addOrUpdatePublicKey(request: AddOrUpdatePublicKeyRequest): capillary.kmp.Empty {
        val authData = AUTH_CONTEXT_KEY.get()
        userPublicKeysSource.saveUserPublicKey(request.toSKUserPublicKey(authData.userId))
        return capillary.kmp.empty { }
    }

    override suspend fun addOrUpdateUser(request: AddOrUpdateUserRequest): capillary.kmp.Empty {
        val authData = AUTH_CONTEXT_KEY.get()
        if (userPushTokenDataSource.checkIfTokenExists(authData.userId) == 0L) {
            userPushTokenDataSource.saveUserToken(request.toSKUserPushToken(authData.userId))
        }
        return capillary.kmp.empty { }
    }

    /*override suspend fun sendMessage(request: SendMessageRequest): Empty {
        try {
            // Get public key.
            val recipientPublicKey: SKUserPublicKey? =
                userPublicKeysSource.getKeyBytes(request.userId, request.keyAlgorithm.name, request.isAuthKey)
            val message = request.dataList.map { it.byte.toByte() }.toByteArray()

            // To create a ciphertext.
            val encryptedManager: EncryptedManager = getKoin().get(named(request.keyAlgorithm.name))
            recipientPublicKey?.let { encryptedManager.loadPublicKey(it) }
            val ciphertext = encryptedManager.encrypt(message)
            encryptedManager.clearPublicKey()
            val ciphertextString = Base64.encode(ciphertext)
            // Get FCM token.
            val token: String? = userPushTokenDataSource.getToken(request.userId)

            // Create the data map to be sent as a JSON object.
            val dataMap: MutableMap<String, String> = HashMap()
            dataMap[SlackConstants.CIPHERTEXT_KEY] = ciphertextString
            dataMap[SlackConstants.KEY_ALGORITHM_KEY] = request.keyAlgorithm.name

            // send push message here to token with dataMap and
            return empty {
                this.nothing = ciphertextString
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw ex
        }
    }*/
}

private fun AddOrUpdatePublicKeyRequest.toSKUserPublicKey(userId: String): SKUserPublicKey {
    return SKUserPublicKey(userId, this.algorithm.name, this.keyBytesList.map { it.byte.toByte() }.toByteArray())
}

private fun AddOrUpdateUserRequest.toSKUserPushToken(userId: String): SKUserPushToken {
    return SKUserPushToken(userId, token = this.token, platform = PLATFORM_ANDROID)
}
