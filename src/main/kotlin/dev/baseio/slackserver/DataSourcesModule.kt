package dev.baseio.slackserver

import dev.baseio.slackserver.data.database.Database
import dev.baseio.slackserver.data.impl.*
import dev.baseio.slackserver.data.sources.*
import dev.baseio.slackserver.services.AuthenticationDelegate
import dev.baseio.slackserver.services.AuthenticationDelegateImpl
import dev.baseio.slackserver.services.IQrCodeGenerator
import dev.baseio.slackserver.services.QrCodeGenerator
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val dataSourcesModule = module {

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
    factory<AuthenticationDelegate> {
        AuthenticationDelegateImpl(KoinJavaComponent.getKoin().get(), KoinJavaComponent.getKoin().get())
    }
    factory<IQrCodeGenerator> { QrCodeGenerator() }
}
