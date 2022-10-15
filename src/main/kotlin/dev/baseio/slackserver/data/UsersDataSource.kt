package dev.baseio.slackserver.data

import kotlinx.coroutines.flow.Flow

interface UsersDataSource {
  suspend fun saveUser(skUser: SkUser): SkUser?
  fun getChangeInUserFor(workspaceId: String): Flow<Pair<SkUser?, SkUser?>>
  suspend fun getUsers(workspaceId: String): List<SkUser>
  suspend fun getUser(userId: String, workspaceId: String): SkUser?
}