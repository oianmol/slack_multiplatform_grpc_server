package dev.baseio.slackserver.data


interface AuthDataSource {
  suspend fun register(email: String, password: String, user: SkUser): SkUser?
  suspend fun login(email: String, password: String, workspaceId: String): SkUser?
}

data class SkAuthUser(
  val uuid: String,
  val userId: String,
  val password: String
)

data class SkUser(
  val uuid: String,
  val workspaceId: String,
  val gender: String?,
  val name: String,
  val location: String?,
  val email: String,
  val username: String,
  val userSince: Long,
  val phone: String,
  val avatarUrl: String
) {
  companion object {
    const val NAME = "skUser"
  }
}