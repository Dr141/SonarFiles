package com.sonarfiles.app.core.di

import com.sonarfiles.app.core.common.DefaultDispatchersProvider
import com.sonarfiles.app.core.common.DispatchersProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatchersProvider(): DispatchersProvider {
        return DefaultDispatchersProvider()
    }
}
