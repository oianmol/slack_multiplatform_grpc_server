import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.data.impl.*
import dev.baseio.slackserver.data.sources.UsersDataSource
import dev.baseio.slackserver.services.*
import dev.baseio.slackserver.services.interceptors.AuthInterceptor
import io.grpc.ServerBuilder

fun main() {
  val workspaceDataSource = WorkspaceDataSourceImpl(Database.slackDB)
  val usersDataSource: UsersDataSource = UsersDataSourceImpl(Database.slackDB)

  val channelMemberDataSource = ChannelMemberDataSourceImpl(Database.slackDB)
  val channelsDataSource = ChannelsDataSourceImpl(Database.slackDB, channelMemberDataSource)
  val messagesDataSource = MessagesDataSourceImpl(Database.slackDB)
  val authDataSource = AuthDataSourceImpl(Database.slackDB)

  val authenticationDelegate: AuthenticationDelegate = AuthenticationDelegateImpl(authDataSource, usersDataSource)

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
        authDelegate = authenticationDelegate
      )
    )
    .addService(
      ChannelService(
        channelsDataSource = channelsDataSource,
        channelMemberDataSource = channelMemberDataSource,
        usersDataSource = usersDataSource
      )
    )
    .addService(
      MessagingService(
        messagesDataSource = messagesDataSource,
      )
    )
    .addService(UserService(usersDataSource = usersDataSource))
    .intercept(AuthInterceptor())
    .build()
    .start()
    .awaitTermination()
}