package net.eyecu.dyn

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.OptionTransformContext
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.boolean
import org.jobrunr.scheduling.cron.CronExpression
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.route53.Route53Client

class Route53a4kApplication : CliktCommand() {
    override fun run() = Unit
}

class Run(
    private val application: Application,
    private val configuration: Configuration
) : CliktCommand() {

    override fun help(context: Context) =
        "Immediate run Route53a4k and dynamically verify and update the pre-configured Route53 hosted zone."

    override fun run() {
        val properties = loadProperties(configuration, application)
        application.run(properties, properties.getSdk())
    }
}

class Schedule(
    private val application: Application,
    private val configuration: Configuration
) : CliktCommand() {

    private val schedule by option()
        .prompt("Verification schedule")
        .help("A valid cron expression used to determine when Route53a4k verify and update Route53 DNS records")
        .validate {
            try {
                CronExpression.create(it)
            } catch (e: Exception) {
                fail("$it must be a valid six value cron expression")
            }
        }

    override fun help(context: Context) =
        "Schedule Route53a4k to dynamically verify and update the pre-configured Route53 hosted zone. Route53a4k will continue to update Route53 as long as it is running."

    override fun run() {
        val properties = loadProperties(configuration, application)
        application.schedule(schedule, properties, properties.getSdk())
    }
}

class Init(
    private val configuration: Configuration
) : CliktCommand() {

    private val hostedZoneId by option()
        .prompt("Hosted Zone ID")
        .help("Route 53 Hosted Zone ID")


    private val hostname by option()
        .prompt("Hostname")
        .help("The hostname (domain name) that should ne updated")

    private val useDefaultAwsProviderChain by option()
        .boolean()
        .default(false)
        .help(
            "Use the default AWS credential provider chain assuming AWS credentials are provided as defined in the AWS documentation (https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html). " +
                    "If set an awsAccessKeyId and awsAccessSecret do not need to be provided."
        )

    private val awsAccessSecret by option()
        .optionalValue("")
        .prompt("AWS Access Secret")
        .help("AWS Access secret with the following IAM permissions: $REQUIRED_PERMISSIONS")
        .validate {
            validateAwsKeys(it)
        }

    private val awsAccessKeyId by option()
        .optionalValue("")
        .prompt("AWS Access Key ID")
        .help("AWS Access Key ID with the following IAM permissions: $REQUIRED_PERMISSIONS")
        .validate {
            validateAwsKeys(it)
        }

    private fun OptionTransformContext.validateAwsKeys(value: String) {
        if (!useDefaultAwsProviderChain && (value.isBlank() || value.isEmpty())) {
            fail("AWS Access Key ID and secret are required unless the default AWS credential provider chain is used")
        }
    }

    override fun help(context: Context): String =
        "Create the configuration file required to run Route53a4k, if a configuration file already exists it will be overwritten. This is required before running schedule."

    companion object {
        private const val REQUIRED_PERMISSIONS = """
            route53:ChangeResourceRecordSets, route53:ListResourceRecordSets
        """
    }

    override fun run() {
        configuration.createConfigurationProperties(
            hostedZoneId = hostedZoneId,
            awsAccessKeyId = awsAccessKeyId,
            awsAccessSecret = awsAccessSecret,
            useDefaultCredentialProviderChain = useDefaultAwsProviderChain,
            hostname = hostname
        )
    }

}

private fun loadProperties(configuration: Configuration, application: Application) =
    configuration.validateAndLoadProperties()?.also {
        validateAwsSettings(it, application)
    } ?: throw CliktError("Configuration file not found or is invalid, please run init before scheduling Route53a4k")

private fun validateAwsSettings(properties: ConfigurationProperties, application: Application) {
    if (!application.hostedZoneIsAccessible(properties, properties.getSdk())) {
        throw CliktError("The AWS configration may be invalid or the requested hosted zone and hostname may not exist. Please run init again and ensure the provided AWS credentials are valid and the hosted zone is set up with an A record that points to a valid IP address.")
    }
}

private fun ConfigurationProperties.getSdk(): Route53Client {
    val builder = Route53Client.builder()

    if (!useDefaultCredentialProviderChain) {
        builder.credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.builder()
                    .accessKeyId(awsAccessKeyId)
                    .secretAccessKey(awsAccessSecret)
                    .build()
            )
        )
    }

    return builder.build()
}