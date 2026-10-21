package com.sonarfiles.app.core.di

import android.content.Context
import androidx.room.Room
import com.sonarfiles.app.data.local.SonarDatabase
import com.sonarfiles.app.data.local.dao.ServerDao
import com.sonarfiles.app.data.local.dao.TransferHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSonarDatabase(
        @ApplicationContext context: Context
    ): SonarDatabase {
        return Room.databaseBuilder(
            context,
            SonarDatabase::class.java,
            SonarDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideServerDao(database: SonarDatabase): ServerDao {
        return database.serverDao()
    }

    @Provides
    @Singleton
    fun provideTransferHistoryDao(database: SonarDatabase): TransferHistoryDao {
        return database.transferHistoryDao()
    }
}
