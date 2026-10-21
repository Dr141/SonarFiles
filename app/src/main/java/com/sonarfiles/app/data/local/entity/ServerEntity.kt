package com.sonarfiles.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sonarfiles.app.domain.model.ProtocolType
import com.sonarfiles.app.domain.model.ServerConfig

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val displayName: String,
    val hostOrIp: String,
    val port: Int,
    val protocol: ProtocolType,
    val defaultShareOrPath: String = "/",
    val domainOrWorkgroup: String? = null,
    val macAddress: String? = null,
    val requiresBiometrics: Boolean = true,
    val isFavorite: Boolean = false,
    val lastConnectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ServerConfig = ServerConfig(
        id = id,
        displayName = displayName,
        hostOrIp = hostOrIp,
        port = port,
        protocol = protocol,
        defaultShareOrPath = defaultShareOrPath,
        domainOrWorkgroup = domainOrWorkgroup,
        macAddress = macAddress,
        requiresBiometrics = requiresBiometrics,
        isFavorite = isFavorite,
        lastConnectedAt = lastConnectedAt
    )

    companion object {
        fun fromDomain(domain: ServerConfig): ServerEntity = ServerEntity(
            id = domain.id,
            displayName = domain.displayName,
            hostOrIp = domain.hostOrIp,
            port = domain.port,
            protocol = domain.protocol,
            defaultShareOrPath = domain.defaultShareOrPath,
            domainOrWorkgroup = domain.domainOrWorkgroup,
            macAddress = domain.macAddress,
            requiresBiometrics = domain.requiresBiometrics,
            isFavorite = domain.isFavorite,
            lastConnectedAt = domain.lastConnectedAt
        )
    }
}
