package com.sonarfiles.app.core.di

import com.sonarfiles.app.core.network.AndroidNetworkConnectivityObserver
import com.sonarfiles.app.core.network.NetworkConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkConnectivityObserver(
        observer: AndroidNetworkConnectivityObserver
    ): NetworkConnectivityObserver
}
