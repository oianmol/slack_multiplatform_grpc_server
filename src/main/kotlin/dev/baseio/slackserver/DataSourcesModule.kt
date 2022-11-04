package dev.baseio.slackserver

import dev.baseio.slackdata.securepush.KeyAlgorithm
import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.data.impl.*
import dev.baseio.slackserver.data.sources.*
import dev.baseio.slackserver.security.EncryptedManager
import dev.baseio.slackserver.security.EncryptedManagerFactory
import dev.baseio.slackserver.security.RsaEcdsaConstants
import dev.baseio.slackserver.services.AuthenticationDelegate
import dev.baseio.slackserver.services.AuthenticationDelegateImpl
import dev.baseio.slackserver.services.IQrCodeGenerator
import dev.baseio.slackserver.services.QrCodeGenerator
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent
import java.io.InputStream

val dataSourcesModule = module {
    factory(named(RsaEcdsaConstants.FILE_INPUT_STREAM)) {
        this.javaClass.getResourceAsStream(RsaEcdsaConstants.FILE_INPUT_STREAM)!!
    }
    factory(qualifier = named(KeyAlgorithm.RSA_ECDSA.name)) {
        EncryptedManagerFactory().create(KeyAlgorithm.RSA_ECDSA)
    }
    factory(qualifier = named(KeyAlgorithm.WEB_PUSH.name)) {
        EncryptedManagerFactory().create(KeyAlgorithm.WEB_PUSH)
    }

    factory<WorkspaceDataSource> { WorkspaceDataSourceImpl(Database.slackDB) }
    factory<UsersDataSource> { UsersDataSourceImpl(Database.slackDB) }

    factory<ChannelMemberDataSource> {
        ChannelMemberDataSourceImpl(Database.slackDB)
    }
    factory<ChannelsDataSource> {
        ChannelsDataSourceImpl(Database.slackDB, getKoin().get())
    }
    factory<MessagesDataSource> {
        MessagesDataSourceImpl(Database.slackDB)
    }
    factory<AuthDataSource> {
        AuthDataSourceImpl(Database.slackDB)
    }
    factory<UserPushTokenDataSource> {
        UserPushTokenDataSourceImpl(Database.slackDB)
    }
    factory<UserPublicKeysSource> {
        UserPublicKeysSourceImpl(Database.slackDB)
    }
    factory<AuthenticationDelegate> {
        AuthenticationDelegateImpl(KoinJavaComponent.getKoin().get(), KoinJavaComponent.getKoin().get())
    }
    factory<IQrCodeGenerator> { QrCodeGenerator() }
}
