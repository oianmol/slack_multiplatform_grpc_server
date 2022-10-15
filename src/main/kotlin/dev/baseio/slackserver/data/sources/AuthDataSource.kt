package dev.baseio.slackserver.data.sources

import dev.baseio.slackserver.data.models.SkUser


interface AuthDataSource {
  suspend fun register(email: String, password: String, user: SkUser): SkUser?
  suspend fun login(email: String, password: String, workspaceId: String): SkUser?
}

