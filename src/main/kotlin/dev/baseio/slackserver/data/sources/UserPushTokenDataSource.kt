package dev.baseio.slackserver.data.sources

import dev.baseio.slackserver.data.models.SKUserPushToken

interface UserPushTokenDataSource {
    suspend fun saveUserToken(userToken: SKUserPushToken)
    suspend fun checkIfTokenExists(userId: String): Long
    suspend fun getToken(userId: String): String?
}