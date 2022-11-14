import com.google.auth.oauth2.GoogleCredentials
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.signature.SignatureConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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

    initializeTink()

    initializeFCM()

    val koinApplication = startKoin {
        modules(dataSourcesModule)
    }
    // The {certificate, private key} pair to use for gRPC TLS.
    val tlsCertFile = object {}.javaClass.getResourceAsStream(TLS_CERT_PATH_OPTION)
    val tlsPrivateKeyFile = object {}.javaClass.getResourceAsStream(TLS_PRIVATE_KEY_PATH_OPTION)

    ServerBuilder.forPort(17600)
        //.useTransportSecurity(tlsCertFile, tlsPrivateKeyFile) // TODO enable this once the kmp library supports this.
        .addService(
            AuthService(
                authDataSource = koinApplication.koin.get(),
                authenticationDelegate = koinApplication.koin.get()
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

fun initializeFCM() {
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()

    FirebaseApp.initializeApp(options)
}

fun initializeTink() {
    com.google.crypto.tink.Config.register(SignatureConfig.LATEST);
    AeadConfig.register()
}
