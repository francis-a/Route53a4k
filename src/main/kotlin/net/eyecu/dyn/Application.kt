package net.eyecu.dyn

import net.eyecu.dyn.Route53a4k.logger
import org.jobrunr.scheduling.BackgroundJob
import software.amazon.awssdk.services.route53.Route53Client
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class Application(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    checkIpUri: URI = URI.create("https://checkip.amazonaws.com/")
) {
    private val getRequest = HttpRequest.newBuilder(checkIpUri).GET().build()

    fun schedule(schedule: String, configurationProperties: ConfigurationProperties, route53Client: Route53Client) {
        logger.info("Scheduling DNS updates using crontab $schedule")
        BackgroundJob.scheduleRecurrently(schedule) {
            run(configurationProperties, route53Client)
        }
    }

    fun run(configurationProperties: ConfigurationProperties, route53Client: Route53Client) {
        logger.info("Attempting DNS record update")
        val result = updatedHostedZoneIp(configurationProperties, route53Client)
        println(result)
    }

    fun hostedZoneIsAccessible(configurationProperties: ConfigurationProperties, route53Client: Route53Client) =
        getHostnameRecordSet(configurationProperties, route53Client) != null

    private fun updatedHostedZoneIp(
        configurationProperties: ConfigurationProperties,
        route53Client: Route53Client
    ): OperationResult {
        val ipAddress = runCatching {
            getIpAddress()
        }.getOrNull() ?: return configurationProperties.buildOperatorResult(
            success = false,
            ipUpdated = false,
            localIpAddress = null
        )

        val currentHostedZoneIp = getCurrentHostedZoneIp(configurationProperties, route53Client)

        if (currentHostedZoneIp != ipAddress) {
            logger.info("IP address changed from $currentHostedZoneIp to $ipAddress, updating record set")
            changeHostedZoneIp(ipAddress, configurationProperties, route53Client)
        }

        return configurationProperties.buildOperatorResult(
            success = true,
            ipUpdated = currentHostedZoneIp != ipAddress,
            localIpAddress = ipAddress
        )
    }

    private fun getCurrentHostedZoneIp(configurationProperties: ConfigurationProperties, route53Client: Route53Client) =
        getHostnameRecordSet(configurationProperties, route53Client)?.let {
            it.resourceRecords().firstOrNull()?.value()
        }

    private fun getHostnameRecordSet(configurationProperties: ConfigurationProperties, route53Client: Route53Client) =
        route53Client.runCatchingWithSdk {
            listResourceRecordSets(configurationProperties.listResourceRecordSetsRequest())
                .resourceRecordSets()
                .firstOrNull {
                    it.name().startsWith(configurationProperties.hostname)
                }
        }

    private fun changeHostedZoneIp(
        newIpAddress: String,
        configurationProperties: ConfigurationProperties,
        route53Client: Route53Client
    ) = route53Client.runCatchingWithSdk {
        val changeRequest = configurationProperties.changeResourceRecordSetsRequest(newIpAddress)
        route53Client.changeResourceRecordSets(changeRequest)
    }

    private fun getIpAddress(): String? {
        val result = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString())

        return if (result.statusCode() != 200) {
            logger.error("Error loading IP address from $getRequest, got status code ${result.statusCode()}")
            null
        } else {
            result.body().trimMargin().also {
                logger.debug("Found IP address of $it")
            }
        }
    }

    private fun ConfigurationProperties.buildOperatorResult(
        success: Boolean,
        ipUpdated: Boolean,
        localIpAddress: String?,
    ) = OperationResult(
        hostedZoneId = hostedZoneId,
        hostName = hostname,
        success = success,
        ipUpdated = ipUpdated,
        localIpAddress = localIpAddress
    )

}

data class ConfigurationProperties(
    val hostedZoneId: String,
    val hostname: String,
    val awsRegion: String?,
    val awsAccessKeyId: String,
    val awsAccessSecret: String,
    val useDefaultCredentialProviderChain: Boolean,
)

private const val RED = "\u001B[31m"
private const val GREEN = "\u001B[32m"
private const val BLUE = "\u001B[34m"
private const val RESET = "\u001B[0m"

data class OperationResult(
    val hostedZoneId: String,
    val hostName: String,
    val success: Boolean,
    val ipUpdated: Boolean,
    val localIpAddress: String?,
) {
    private val logLine = """
        ====================================
        Hosted zone update operation result:
        host name =         $hostName
        hosted zone id =    $hostedZoneId             
        update performed =  $ipUpdated
        ${if(success) { GREEN } else { RED }}success =           $success$RESET
        ====================================
       
        $BLUE==> ${operation(success, ipUpdated, localIpAddress)}$RESET
    """.trimIndent()

    companion object {
        fun operation(
            success: Boolean,
            ipUpdated: Boolean,
            localIpAddress: String?,
        ): String = when {
            !success -> "Warning: Update operation failed"
            !ipUpdated -> "Local IP address matches Route53 record, no update performed"
            else -> "Route53 record updated to match local IP address of $localIpAddress"
        }
    }

    override fun toString() = logLine
}

