package com.sonarfiles.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sonarfiles.app.data.local.dao.ServerDao
import com.sonarfiles.app.data.local.dao.TransferHistoryDao
import com.sonarfiles.app.data.local.entity.ServerEntity
import com.sonarfiles.app.data.local.entity.TransferHistoryEntity
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.TransferStatus
import com.sonarfiles.app.domain.model.TransferType

class Converters {
    @TypeConverter
    fun fromProtocolType(value: ProtocolType): String = value.name

    @TypeConverter
    fun toProtocolType(value: String): ProtocolType = ProtocolType.valueOf(value)

    @TypeConverter
    fun fromTransferType(value: TransferType): String = value.name

    @TypeConverter
    fun toTransferType(value: String): TransferType = TransferType.valueOf(value)

    @TypeConverter
    fun fromTransferStatus(value: TransferStatus): String = value.name

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus = TransferStatus.valueOf(value)
}

@Database(
    entities = [ServerEntity::class, TransferHistoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SonarDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun transferHistoryDao(): TransferHistoryDao

    companion object {
        const val DATABASE_NAME = "sonar_files.db"
    }
}
