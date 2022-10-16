package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.SKAuthResult
import dev.baseio.slackdata.protos.SKAuthUser
import dev.baseio.slackserver.data.sources.AuthDataSource
import dev.baseio.slackserver.data.sources.UsersDataSource

interface AuthenticationDelegate {
    suspend fun authenticateUser(request: SKAuthUser, workspaceId: String): SKAuthResult
}

class AuthenticationDelegateImpl(
    private val authDataSource: AuthDataSource,
    private val usersDataSource: UsersDataSource
) : AuthenticationDelegate {
    override suspend fun authenticateUser(request: SKAuthUser, workspaceId: String): SKAuthResult {
        return kotlin.runCatching {
            val existingUser = usersDataSource.getUserWithEmailId(emailId = request.email, workspaceId = workspaceId)
            existingUser?.let {
                authDataSource.login(request.email, request.password, workspaceId)?.let {
                    return skAuthResult(it)
                }
            } ?: run {
                val generatedUser = authDataSource.register(
                    request.email,
                    request.password,
                    request.user.toDBUser().copy(workspaceId = workspaceId)
                )
                skAuthResult(generatedUser)
            }
        }.getOrThrow()
    }

}