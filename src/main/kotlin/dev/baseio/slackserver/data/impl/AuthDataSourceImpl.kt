package dev.baseio.slackserver.data.impl

import at.favre.lib.crypto.bcrypt.BCrypt
import dev.baseio.slackserver.data.sources.AuthDataSource
import dev.baseio.slackserver.data.models.SkAuthUser
import dev.baseio.slackserver.data.models.SkUser
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.findOne
import java.util.*

class AuthDataSourceImpl(private val slackCloneDB: CoroutineDatabase) : AuthDataSource {
  override suspend fun login(email: String, password: String, workspaceId: String): SkUser? {
    val user = slackCloneDB.getCollection<SkUser>().collection
      .findOne(
        SkUser::email eq email,
        SkUser::workspaceId eq workspaceId
      )
    user.awaitFirstOrNull()?.let { user ->
      slackCloneDB.getCollection<SkAuthUser>().collection
        .findOne(SkAuthUser::userId eq user.uuid)
        .awaitFirstOrNull()?.let {
          val result: BCrypt.Result = BCrypt.verifyer().verify(password.toCharArray(), it.password)
          if (result.verified) {
            return user
          }
        }
    }
    return null
  }

  override suspend fun register(email: String, password: String, user: SkUser): SkUser? {
    //save the user details
    slackCloneDB.getCollection<SkUser>().collection.insertOne(
      user
    ).awaitFirstOrNull()
    // save the auth
    val bcryptHashString: String = BCrypt.withDefaults().hashToString(12, password.toCharArray())

    slackCloneDB.getCollection<SkAuthUser>().collection.insertOne(
      SkAuthUser(UUID.randomUUID().toString(), user.uuid, bcryptHashString)
    ).awaitFirstOrNull()

    return slackCloneDB.getCollection<SkUser>().collection.findOne(SkUser::uuid eq user.uuid).awaitFirstOrNull()
  }
}