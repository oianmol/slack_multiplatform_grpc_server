package dev.baseio.slackserver.data.sources

interface WorkspaceDataSource {
  suspend fun getWorkspaces(): List<SkWorkspace>
  suspend fun saveWorkspace(skWorkspace: SkWorkspace): SkWorkspace?
  suspend fun findWorkspacesForEmail(email: String): List<SkWorkspace>
  suspend fun findWorkspaceForName(name: String): SkWorkspace?
}


data class SkWorkspace(
  val uuid: String,
  val name: String,
  val domain: String,
  val picUrl: String?,
  val lastSelected: Boolean = false
)