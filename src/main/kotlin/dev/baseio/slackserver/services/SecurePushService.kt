package dev.baseio.slackserver.services

import com.google.crypto.tink.subtle.Base64
import dev.baseio.slackdata.protos.Empty
import dev.baseio.slackdata.protos.empty
import dev.baseio.slackdata.securepush.*
import dev.baseio.slackserver.data.models.PLATFORM_ANDROID
import dev.baseio.slackserver.data.models.SKUserPublicKey
import dev.baseio.slackserver.data.models.SKUserPushToken
import dev.baseio.slackserver.data.sources.UserPublicKeysSource
import dev.baseio.slackserver.data.sources.UserPushTokenDataSource
import dev.baseio.slackserver.security.EncryptedManager
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.getKoin
import java.util.*
import kotlin.coroutines.CoroutineContext

class SecurePushService(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    private val userPushTokenDataSource: UserPushTokenDataSource,
    private val userPublicKeysSource: UserPublicKeysSource,
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
        try{
            // Get public key.
            val publicKey: SKUserPublicKey? =
                userPublicKeysSource.getKeyBytes(request.userId, request.keyAlgorithm.name, request.isAuthKey)
            // Generate ciphertext.
            val encryptedManager: EncryptedManager = getKoin().get(named(request.keyAlgorithm.name))
            publicKey?.let { encryptedManager.loadPublicKey(it) }
            val ciphertext = encryptedManager.encrypt(request.dataList.map { it.byte.toByte() }.toByteArray())
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
        }catch (ex:Exception){
            ex.printStackTrace()
            throw ex
        }
    }
}

private fun AddOrUpdatePublicKeyRequest.toSKUserPublicKey(): SKUserPublicKey {
    return SKUserPublicKey(this.userId, this.algorithm.name, this.isAuth, this.keyBytesList.map { it.byte.toByte() }.toByteArray())
}

private fun AddOrUpdateUserRequest.toSKUserPushToken(): SKUserPushToken {
    return SKUserPushToken(userId, token = this.token, platform = PLATFORM_ANDROID)
}
