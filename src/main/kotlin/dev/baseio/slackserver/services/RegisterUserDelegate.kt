package dev.baseio.slackserver.services

import dev.baseio.slackdata.protos.SKAuthResult
import dev.baseio.slackdata.protos.SKAuthUser
import dev.baseio.slackserver.data.impl.AuthDataSourceImpl

interface RegisterUserDelegate {
    suspend fun registerUser(request: SKAuthUser, workspaceId: String): SKAuthResult
}

class RegisterUserDelegateImpl(private val authDataSource: AuthDataSourceImpl) : RegisterUserDelegate{
    override suspend fun registerUser(request: SKAuthUser, workspaceId: String): SKAuthResult {
        return try {
            val generatedUser = authDataSource.register(
                request.email,
                request.password,
                request.user.toDBUser().copy(workspaceId = workspaceId)
            )
            skAuthResult(generatedUser)
        } catch (ex: Exception) {
            ex.printStackTrace()
            SKAuthResult.newBuilder().build()
        }
    }

}