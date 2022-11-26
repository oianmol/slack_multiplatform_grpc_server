import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dev.baseio.slackserver.data.models.SkChannel
import dev.baseio.slackserver.data.models.SkChannelMember
import dev.baseio.slackserver.data.models.SkMessage
import dev.baseio.slackserver.dataSourcesModule
import dev.baseio.slackserver.services.*
import dev.baseio.slackserver.services.interceptors.AuthInterceptor
import io.grpc.ServerBuilder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.getKoin


const val TLS_CERT_PATH_OPTION = "tls/tls.crt"
const val TLS_PRIVATE_KEY_PATH_OPTION = "tls/tls.key"

fun main() {
    initializeFCM()

    initKoin()

    // The {certificate, private key} pair to use for gRPC TLS.
    val tlsCertFile = object {}.javaClass.getResourceAsStream(TLS_CERT_PATH_OPTION)
    val tlsPrivateKeyFile = object {}.javaClass.getResourceAsStream(TLS_PRIVATE_KEY_PATH_OPTION)

    ServerBuilder.forPort(8081)
        //.useTransportSecurity(tlsCertFile, tlsPrivateKeyFile) // TODO enable this once the kmp library supports this.
        .addService(
            AuthService(
                pushTokenDataSource = getKoin().get(),
                authenticationDelegate = getKoin().get()
            )
        )
        .addService(QrCodeService(database = getKoin().get(), qrCodeGenerator = getKoin().get()))
        .addService(
            WorkspaceService(
                workspaceDataSource = getKoin().get(),
                authDelegate = getKoin().get()
            )
        )
        .addService(
            ChannelService(
                channelsDataSource = getKoin().get(),
                channelMemberDataSource = getKoin().get(),
                usersDataSource = getKoin().get(),
                channelMemberPNSender = getKoin().get(named(SkChannelMember::class.java.name)),
                channelPNSender = getKoin().get(named(SkChannel::class.java.name))
            )
        )
        .addService(
            MessagingService(
                messagesDataSource = getKoin().get(),
                pushNotificationForMessages = getKoin().get(named(SkMessage::class.java.name))
            )
        )
        .addService(UserService(usersDataSource = getKoin().get()))
        .intercept(AuthInterceptor())
        .build()
        .start()
        .awaitTermination()

    stopKoin()
}

private fun initKoin() {
    startKoin {
        modules(dataSourcesModule)
    }
}

fun initializeFCM() {
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()

    FirebaseApp.initializeApp(options)
}