package dev.baseio.slackserver.data.impl

import dev.baseio.slackserver.data.models.SKUserPublicKey
import dev.baseio.slackserver.data.sources.UserPublicKeysSource
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.eq

class UserPublicKeysSourceImpl(private val coroutineDatabase: CoroutineDatabase) : UserPublicKeysSource {
    override suspend fun saveUserPublicKey(skUserPublicKey: SKUserPublicKey) {
        coroutineDatabase.getCollection<SKUserPublicKey>()
            .insertOne(skUserPublicKey)
    }

    override suspend fun getKeyBytes(userId: String, name: String, authKey: Boolean): SKUserPublicKey? {
        return coroutineDatabase.getCollection<SKUserPublicKey>()
            .findOne(
                SKUserPublicKey::userId eq userId,
                SKUserPublicKey::algorithm eq name,
                SKUserPublicKey::isAuth eq  authKey
            )
    }
}