import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.data.impl.*
import dev.baseio.slackserver.data.sources.UsersDataSource
import dev.baseio.slackserver.services.*
import dev.baseio.slackserver.services.interceptors.AuthInterceptor
import io.grpc.ServerBuilder

fun main() {
    val workspaceDataSource = WorkspaceDataSourceImpl(Database.slackDB)
    val channelsDataSource = ChannelsDataSourceImpl(Database.slackDB)
    val messagesDataSource = MessagesDataSourceImpl(Database.slackDB)
    val usersDataSource : UsersDataSource = UsersDataSourceImpl(Database.slackDB)
    val authDataSource = AuthDataSourceImpl(Database.slackDB)

    val authenticationDelegate: AuthenticationDelegate = AuthenticationDelegateImpl(authDataSource,usersDataSource)

    ServerBuilder.forPort(17600)
        .addService(
            AuthService(
                authDataSource = authDataSource,
                authenticationDelegate = authenticationDelegate
            )
        )
        .addService(
            WorkspaceService(
                workspaceDataSource = workspaceDataSource,
                registerUser = authenticationDelegate
            )
        )
        .addService(ChannelService(channelsDataSource = channelsDataSource))
        .addService(MessagingService(messagesDataSource = messagesDataSource, usersDataSource = usersDataSource))
        .addService(UserService(usersDataSource = usersDataSource))
        .intercept(AuthInterceptor())
        .build()
        .start()
        .awaitTermination()
}