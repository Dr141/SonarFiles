package com.sonarfiles.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class SonarFilesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        // Remove a versao padrao/recortada do BouncyCastle fornecida pelo SO
        // e insere o BouncyCastle atualizado na primeira posicao para garantir
        // suporte completo a ed25519, chacha20 e cifras modernas SMB3/SSH
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
