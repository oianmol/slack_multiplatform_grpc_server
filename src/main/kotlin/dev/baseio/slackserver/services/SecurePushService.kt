package dev.baseio.slackserver.services

import com.google.crypto.tink.subtle.Base64
import dev.baseio.slackdata.protos.Empty
import dev.baseio.slackdata.protos.empty
import dev.baseio.slackdata.securepush.*
import dev.baseio.slackserver.data.impl.UserPublicKeysSourceImpl
import dev.baseio.slackserver.data.impl.UserPushTokenDataSourceImpl
import dev.baseio.slackserver.data.models.PLATFORM_ANDROID
import dev.baseio.slackserver.data.models.SKUserPublicKey
import dev.baseio.slackserver.data.models.SKUserPushToken
import dev.baseio.slackserver.security.EncryptedManager
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.coroutines.CoroutineContext

class SecurePushService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val userPushTokenDataSource: UserPushTokenDataSourceImpl,
    private val userPublicKeysSource: UserPublicKeysSourceImpl,
) : SecurePushServiceGrpcKt.SecurePushServiceCoroutineImplBase(coroutineContext) {
    override suspend fun addOrUpdatePublicKey(request: AddOrUpdatePublicKeyRequest): Empty {
        userPublicKeysSource.saveUserPublicKey(request.toSKUserPublicKey())
        return empty { }
    }

    override suspend fun addOrUpdateUser(request: AddOrUpdateUserRequest): Empty {
        if (userPushTokenDataSource.checkIfTokenExists(request.userId) == 0L) {
            userPushTokenDataSource.saveUserToken(request.toSKUserPushToken())
        }
        return empty { }
    }

    override suspend fun sendMessage(request: SendMessageRequest): Empty {
        // Get public key.
        val publicKey: ByteArray? =
            userPublicKeysSource.getKeyBytes(request.userId, request.keyAlgorithm.name, request.isAuthKey)
        // Generate ciphertext.
        val encryptedManager: EncryptedManager = getEncrypterManager(request.keyAlgorithm)
        encryptedManager.loadPublicKey(publicKey)
        val ciphertext: ByteArray = encryptedManager.encrypt(request.data.toByteArray())
        encryptedManager.clearPublicKey()
        val ciphertextString = Base64.encode(ciphertext)
        // Get FCM token.
        val token: String? = userPushTokenDataSource.getToken(request.userId)

        // Create the data map to be sent as a JSON object.
        val dataMap: MutableMap<String, String> = HashMap()
        dataMap[SlackConstants.CIPHERTEXT_KEY] = ciphertextString
        dataMap[SlackConstants.KEY_ALGORITHM_KEY] = request.keyAlgorithm.name

        // send push message here to token with dataMap and
        return empty { }
    }
}

private fun AddOrUpdatePublicKeyRequest.toSKUserPublicKey(): SKUserPublicKey {
    return SKUserPublicKey(this.userId, this.algorithm.name, this.isAuth, this.keyBytes.toByteArray())
}

private fun AddOrUpdateUserRequest.toSKUserPushToken(): SKUserPushToken {
    return SKUserPushToken(userId, token = this.token, platform = PLATFORM_ANDROID)
}
