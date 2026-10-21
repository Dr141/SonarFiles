package com.sonarfiles.app.core.di

import com.sonarfiles.app.data.network.client.RemoteConnectionFactoryImpl
import com.sonarfiles.app.data.repository.CredentialRepositoryImpl
import com.sonarfiles.app.data.repository.NetworkScannerRepositoryImpl
import com.sonarfiles.app.data.repository.ServerRepositoryImpl
import com.sonarfiles.app.data.repository.TransferRepositoryImpl
import com.sonarfiles.app.domain.repository.CredentialRepository
import com.sonarfiles.app.domain.repository.NetworkScannerRepository
import com.sonarfiles.app.domain.repository.RemoteConnectionFactory
import com.sonarfiles.app.domain.repository.ServerRepository
import com.sonarfiles.app.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(
        impl: ServerRepositoryImpl
    ): ServerRepository

    @Binds
    @Singleton
    abstract fun bindNetworkScannerRepository(
        impl: NetworkScannerRepositoryImpl
    ): NetworkScannerRepository

    @Binds
    @Singleton
    abstract fun bindCredentialRepository(
        impl: CredentialRepositoryImpl
    ): CredentialRepository

    @Binds
    @Singleton
    abstract fun bindRemoteConnectionFactory(
        impl: RemoteConnectionFactoryImpl
    ): RemoteConnectionFactory

    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository
}
