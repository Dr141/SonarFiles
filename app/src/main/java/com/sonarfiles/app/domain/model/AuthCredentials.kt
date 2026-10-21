package com.sonarfiles.app.domain.model

sealed interface AuthCredentials {
    data object Anonymous : AuthCredentials

    data class Password(
        val username: String,
        val password: CharArray,
        val domain: String? = null
    ) : AuthCredentials {
        fun clear() = password.fill('\u0000')

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Password
            return username == other.username && password.contentEquals(other.password) && domain == other.domain
        }

        override fun hashCode(): Int {
            var result = username.hashCode()
            result = 31 * result + password.contentHashCode()
            result = 31 * result + (domain?.hashCode() ?: 0)
            return result
        }
    }

    data class SshKey(
        val username: String,
        val privateKeyPem: CharArray,
        val passphrase: CharArray? = null
    ) : AuthCredentials {
        fun clear() {
            privateKeyPem.fill('\u0000')
            passphrase?.fill('\u0000')
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SshKey
            return username == other.username &&
                    privateKeyPem.contentEquals(other.privateKeyPem) &&
                    (passphrase?.contentEquals(other.passphrase ?: charArrayOf()) ?: (other.passphrase == null))
        }

        override fun hashCode(): Int {
            var result = username.hashCode()
            result = 31 * result + privateKeyPem.contentHashCode()
            result = 31 * result + (passphrase?.contentHashCode() ?: 0)
            return result
        }
    }
}
