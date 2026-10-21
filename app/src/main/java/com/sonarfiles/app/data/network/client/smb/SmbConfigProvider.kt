package com.sonarfiles.app.data.network.client.smb

import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.smbj.SmbConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbConfigProvider @Inject constructor() {

    fun createDefaultConfig(): SmbConfig {
        return SmbConfig.builder()
            .withDialects(
                SMB2Dialect.SMB_3_1_1,
                SMB2Dialect.SMB_3_0_2,
                SMB2Dialect.SMB_3_0,
                SMB2Dialect.SMB_2_1,
                SMB2Dialect.SMB_2_0_2
            )
            .withTimeout(15, TimeUnit.SECONDS)
            .withSoTimeout(60, TimeUnit.SECONDS)
            .withDfsEnabled(true)
            .withMultiProtocolNegotiate(true)
            .build()
    }
}
