package dev.baseio.slackserver.data.sources

import dev.baseio.slackserver.data.models.SKUserPublicKey

interface UserPublicKeysSource {
    suspend fun saveUserPublicKey(skUserPublicKey: SKUserPublicKey)
    suspend fun getKeyBytes(userId: String, name: String, authKey: Boolean): SKUserPublicKey?
}