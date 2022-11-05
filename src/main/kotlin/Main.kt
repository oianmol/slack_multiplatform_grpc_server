import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.dataSourcesModule
import dev.baseio.slackserver.services.*
import dev.baseio.slackserver.services.interceptors.AuthInterceptor
import io.grpc.ServerBuilder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

const val TLS_CERT_PATH_OPTION = "tls/tls.crt"
const val TLS_PRIVATE_KEY_PATH_OPTION = "tls/tls.key"

fun main() {
    val koinApplication = startKoin {
        modules(dataSourcesModule)
    }
    // The {certificate, private key} pair to use for gRPC TLS.
    val tlsCertFile = object {}.javaClass.getResourceAsStream(TLS_CERT_PATH_OPTION)
    val tlsPrivateKeyFile = object {}.javaClass.getResourceAsStream(TLS_PRIVATE_KEY_PATH_OPTION)

    ServerBuilder.forPort(443)
        //.useTransportSecurity(tlsCertFile, tlsPrivateKeyFile) // TODO enable this once the kmp library supports this.
        .addService(
            AuthService(
                authDataSource = koinApplication.koin.get(),
                authenticationDelegate = koinApplication.koin.get()
            )
        )
        .addService(
            SecurePushService(
                userPushTokenDataSource = koinApplication.koin.get(),
                userPublicKeysSource = koinApplication.koin.get()
            )
        )
        .addService(QrCodeService(database = Database.slackDB, qrCodeGenerator = koinApplication.koin.get()))
        .addService(
            WorkspaceService(
                workspaceDataSource = koinApplication.koin.get(),
                authDelegate = koinApplication.koin.get()
            )
        )
        .addService(
            ChannelService(
                channelsDataSource = koinApplication.koin.get(),
                channelMemberDataSource = koinApplication.koin.get(),
                usersDataSource = koinApplication.koin.get()
            )
        )
        .addService(MessagingService(messagesDataSource = koinApplication.koin.get()))
        .addService(UserService(usersDataSource = koinApplication.koin.get()))
        .intercept(AuthInterceptor())
        .build()
        .start()
        .awaitTermination()

    stopKoin()
}