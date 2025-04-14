package net.eyecu.dyn

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class Configuration {

    fun validateAndLoadProperties(): ConfigurationProperties? {
        val file = File(FILENAME)
        if (!file.exists()) {
            return null
        }

        val properties = FileInputStream(file).use {
            Properties().apply {
                load(it)
            }
        }

        val hostedZoneId = properties.getProperty(HOSTED_ZONE_ID) ?: return null
        val hostname = properties.getProperty(HOSTNAME) ?: return null
        val awsRegion = properties.getProperty(AWS_REGION)
        val awsAccessKeyId = properties.getProperty(AWS_ACCESS_KEY_ID) ?: return null
        val awsAccessSecret = properties.getProperty(AWS_ACCESS_SECRET) ?: return null
        val useDefaultCredentialProviderChain =
            properties.getProperty(USE_DEFAULT_PROVIDER_CHAIN)?.toBooleanStrictOrNull() ?: return null


        return ConfigurationProperties(
            hostedZoneId = hostedZoneId,
            awsRegion = awsRegion,
            awsAccessKeyId = awsAccessKeyId,
            awsAccessSecret = awsAccessSecret,
            useDefaultCredentialProviderChain = useDefaultCredentialProviderChain,
            hostname = hostname
        )
    }

    fun createConfigurationProperties(
        hostedZoneId: String,
        hostname: String,
        awsRegion: String,
        awsAccessKeyId: String,
        awsAccessSecret: String,
        useDefaultCredentialProviderChain: Boolean
    ) {
        val outputFile = File(FILENAME)
        outputFile.parentFile.mkdirs()

        val properties = Properties().apply {
            setProperty(HOSTED_ZONE_ID, hostedZoneId)
            setProperty(HOSTNAME, hostname)
            setProperty(USE_DEFAULT_PROVIDER_CHAIN, useDefaultCredentialProviderChain.toString())
            setProperty(AWS_REGION, awsRegion)
            setProperty(AWS_ACCESS_KEY_ID, awsAccessKeyId)
            setProperty(AWS_ACCESS_SECRET, awsAccessSecret)
        }

        val comment = if (outputFile.exists()) {
            "config updated"
        } else {
            "config created"
        }

        FileOutputStream(outputFile, false).use {
            properties.store(it, comment)
        }
    }

    companion object {
        const val FILENAME = "config/route53a4k.properties"

        const val HOSTED_ZONE_ID = "hosted.zone.id"
        const val HOSTNAME = "hostname"
        const val USE_DEFAULT_PROVIDER_CHAIN = "use.default.credential.provider.chain"
        const val AWS_REGION = "aws.region"
        const val AWS_ACCESS_KEY_ID = "aws.access.key.id"
        const val AWS_ACCESS_SECRET = "aws.access.secret"
    }
}