package dev.baseio.slackserver.data.impl

import dev.baseio.slackserver.data.models.SKUserPushToken
import dev.baseio.slackserver.data.sources.UserPushTokenDataSource
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

class UserPushTokenDataSourceImpl(private val coroutineDatabase: CoroutineDatabase) : UserPushTokenDataSource {
    override suspend fun saveUserToken(userToken: SKUserPushToken) {
        coroutineDatabase.getCollection<SKUserPushToken>()
            .insertOne(userToken)
    }

    override suspend fun checkIfTokenExists(userId: String): Long {
        return coroutineDatabase.getCollection<SKUserPushToken>().countDocuments(SKUserPushToken::userId eq userId)
    }

    override suspend fun getToken(userId: String): String? {
        return coroutineDatabase.getCollection<SKUserPushToken>().findOne(SKUserPushToken::userId eq userId)?.token
    }
}