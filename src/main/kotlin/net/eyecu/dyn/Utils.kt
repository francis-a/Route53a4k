package net.eyecu.dyn

import net.eyecu.dyn.Route53a4k.logger
import software.amazon.awssdk.services.route53.Route53Client
import software.amazon.awssdk.services.route53.model.Change
import software.amazon.awssdk.services.route53.model.ChangeAction.UPSERT
import software.amazon.awssdk.services.route53.model.ChangeBatch
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest
import software.amazon.awssdk.services.route53.model.RRType.A
import software.amazon.awssdk.services.route53.model.ResourceRecord
import software.amazon.awssdk.services.route53.model.ResourceRecordSet

fun ConfigurationProperties.listResourceRecordSetsRequest(): ListResourceRecordSetsRequest =
    ListResourceRecordSetsRequest.builder()
        .hostedZoneId(hostedZoneId)
        .startRecordType("A")
        .startRecordName(hostname)
        .maxItems("2")
        .build()

fun ConfigurationProperties.changeResourceRecordSetsRequest(ipAddress: String): ChangeResourceRecordSetsRequest =
    ChangeResourceRecordSetsRequest.builder()
        .hostedZoneId(hostedZoneId)
        .changeBatch(changeBatch(ipAddress))
        .build()

fun ConfigurationProperties.changeBatch(ipAddress: String): ChangeBatch =
    ChangeBatch.builder()
        .changes(change(ipAddress))
        .build()


fun ConfigurationProperties.change(ipAddress: String): Change = Change.builder()
    .action(UPSERT)
    .resourceRecordSet(
        ResourceRecordSet.builder()
            .name(hostname)
            .resourceRecords(
                resourceRecord(ipAddress)
            ).ttl(300)
            .type(A)
            .build()
    ).build()

fun resourceRecord(ipAddress: String): ResourceRecord = ResourceRecord.builder()
    .value(ipAddress)
    .build()

fun <T> Route53Client.runCatchingWithSdk(
    block: Route53Client.() -> T
): T? = runCatching {
    block(this)
}.onFailure {
    logger.error("Remote operation exception returned from API call", it)
}.getOrNull()