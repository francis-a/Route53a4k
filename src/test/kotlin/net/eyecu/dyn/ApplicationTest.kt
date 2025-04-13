import io.mockk.*
import net.eyecu.dyn.Application
import net.eyecu.dyn.ConfigurationProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.route53.Route53Client
import software.amazon.awssdk.services.route53.model.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ApplicationTest {

    private lateinit var httpClient: HttpClient
    private lateinit var route53Client: Route53Client
    private lateinit var app: Application
    private val config = ConfigurationProperties(
        hostedZoneId = "Z123",
        hostname = "test.example.com.",
        awsAccessKeyId = "a",
        awsAccessSecret = "b",
        useDefaultCredentialProviderChain = true
    )

    @BeforeEach
    fun setUp() {
        httpClient = mockk()
        route53Client = mockk(relaxed = true)
        app = Application(httpClient = httpClient, checkIpUri = URI("http://test-ip"))
    }

    @Test
    fun `run - updates record if IP has changed`() {
        val ipResponse = mockk<HttpResponse<String>>()
        every { ipResponse.statusCode() } returns 200
        every { ipResponse.body() } returns "1.2.3.4\n"
        every { httpClient.send(any<HttpRequest>(), eq(HttpResponse.BodyHandlers.ofString())) } returns ipResponse

        val currentRecord = ResourceRecordSet.builder()
            .name("test.example.com.")
            .type(RRType.A)
            .resourceRecords(ResourceRecord.builder().value("4.3.2.1").build())
            .build()
        every {
            route53Client.listResourceRecordSets(any<ListResourceRecordSetsRequest>())
        } returns ListResourceRecordSetsResponse.builder().resourceRecordSets(currentRecord).build()

        every { route53Client.changeResourceRecordSets(any<ChangeResourceRecordSetsRequest>()) } returns ChangeResourceRecordSetsResponse.builder()
            .build()

        app.run(config, route53Client)

        verify(exactly = 1) {
            route53Client.changeResourceRecordSets(withArg<ChangeResourceRecordSetsRequest> {
                assert(it.changeBatch().changes().isNotEmpty())
            })
        }
    }

    @Test
    fun `run - does not update record if IP is same`() {
        val ipResponse = mockk<HttpResponse<String>>()
        every { ipResponse.statusCode() } returns 200
        every { ipResponse.body() } returns "1.2.3.4\n"
        every { httpClient.send(any<HttpRequest>(), eq(HttpResponse.BodyHandlers.ofString())) } returns ipResponse

        val currentRecord = ResourceRecordSet.builder()
            .name("test.example.com.")
            .type(RRType.A)
            .resourceRecords(ResourceRecord.builder().value("1.2.3.4").build())
            .build()
        every {
            route53Client.listResourceRecordSets(any<ListResourceRecordSetsRequest>())
        } returns ListResourceRecordSetsResponse.builder().resourceRecordSets(currentRecord).build()

        app.run(config, route53Client)

        verify(exactly = 0) {
            route53Client.changeResourceRecordSets(any<ChangeResourceRecordSetsRequest>())
        }
    }

    @Test
    fun `run - does nothing if IP fetch fails`() {
        val ipResponse = mockk<HttpResponse<String>>()
        every { ipResponse.statusCode() } returns 500
        every { httpClient.send(any<HttpRequest>(), eq(HttpResponse.BodyHandlers.ofString())) } returns ipResponse

        app.run(config, route53Client)

        verify(exactly = 0) {
            route53Client.changeResourceRecordSets(any<ChangeResourceRecordSetsRequest>())
        }
        confirmVerified(route53Client)
    }

    @Test
    fun `hostedZoneIsAccessible returns true when record exists`() {
        val record = ResourceRecordSet.builder()
            .name("test.example.com.")
            .type(RRType.A)
            .resourceRecords(ResourceRecord.builder().value("1.2.3.4").build())
            .build()
        every {
            route53Client.listResourceRecordSets(any<ListResourceRecordSetsRequest>())
        } returns ListResourceRecordSetsResponse.builder().resourceRecordSets(record).build()

        val result = app.hostedZoneIsAccessible(config, route53Client)
        assert(result)
    }

    @Test
    fun `hostedZoneIsAccessible returns false when no matching record exists`() {
        every {
            route53Client.listResourceRecordSets(any<ListResourceRecordSetsRequest>())
        } returns ListResourceRecordSetsResponse.builder().resourceRecordSets(emptyList()).build()

        val result = app.hostedZoneIsAccessible(config, route53Client)
        assert(!result)
    }
}
