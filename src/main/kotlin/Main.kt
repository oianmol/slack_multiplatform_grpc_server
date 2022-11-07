import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.proto.Ecdsa
import com.google.crypto.tink.signature.SignatureConfig
import dev.baseio.slackdata.securepush.KeyAlgorithm
import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.dataSourcesModule
import dev.baseio.slackserver.security.EncryptedManagerFactory
import dev.baseio.slackserver.services.*
import dev.baseio.slackserver.services.interceptors.AuthInterceptor
import io.grpc.ServerBuilder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val TLS_CERT_PATH_OPTION = "tls/tls.crt"
const val TLS_PRIVATE_KEY_PATH_OPTION = "tls/tls.key"
const val SENDER_SIGNING_KEY = "ecdsa/sender_signing_key.dat"

fun main() {
    com.google.crypto.tink.Config.register(SignatureConfig.LATEST);
    AeadConfig.register()
    val koinApplication = startKoin {
        modules(dataSourcesModule, module {
            factory(qualifier = named(KeyAlgorithm.RSA_ECDSA.name)) {
                val ins = Ecdsa::javaClass.javaClass.getResourceAsStream(SENDER_SIGNING_KEY)
                EncryptedManagerFactory().create(KeyAlgorithm.RSA_ECDSA,ins)
            }
            factory(qualifier = named(KeyAlgorithm.WEB_PUSH.name)) {
                EncryptedManagerFactory().create(KeyAlgorithm.WEB_PUSH, null)
            }
        })
    }
    // The {certificate, private key} pair to use for gRPC TLS.
    val tlsCertFile = object {}.javaClass.getResourceAsStream(TLS_CERT_PATH_OPTION)
    val tlsPrivateKeyFile = object {}.javaClass.getResourceAsStream(TLS_PRIVATE_KEY_PATH_OPTION)

    ServerBuilder.forPort(8443)
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