package no.nav.tsm.sykmelding.dokarkiv

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.ktor.auth.texas.TexasTarget
import no.nav.tsm.ktor.auth.texas.TexasToken
import no.nav.tsm.sykmelding.journalpost.JournalpostRequest
import no.nav.tsm.utils.Environment
import no.nav.tsm.utils.ExternalApis
import no.nav.tsm.utils.Runtime
import no.nav.tsm.utils.RuntimeEnvironments

class DokarkivCloudClientTest {
    val env =
        Environment(
            runtime =
                Runtime(
                    name = "test-app",
                    env = RuntimeEnvironments.DEV,
                ),
            kafka = mockk(relaxed = true),
            external = {
                ExternalApis(
                    dokarkiv =
                        "https://dokarkiv-q2.dev-fss-pub.nais.io/rest/journalpostapi/v1/journalpost"
                )
            },
            bucket = "test-bucket",
        )

    val texas = mockk<TexasClient>()

    @Test
    fun `should properly apply headers and stuff`() = runTest {
        coEvery {
            texas.entraIdToken("teamdokumenthandtering", "dokarkiv", TexasTarget.DEV_FSS)
        } returns TexasToken(token = "test-token")

        val mockEngine = MockEngine { request ->
            request.url.host shouldEqual "dokarkiv-q2.dev-fss-pub.nais.io"
            request.url.encodedPath shouldEqual "/rest/journalpostapi/v1/journalpost"
            request.url.encodedQuery shouldEqual "forsoekFerdigstill=true"

            println(request.headers)
            request.headers["Authorization"] shouldEqual "Bearer test-token"
            request.headers["Nav-Callid"] shouldEqual "ekstern-referanse-id"

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json")),
                content =
                    """
                    {
                      "dokumenter": [],
                      "journalpostId": "very-journal-id",
                      "journalpostferdigstilt": false,
                      "journalstatus": null,
                      "melding": null
                    }
                    """
                        .trimIndent(),
            )
        }

        val client =
            DokarkivCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = texas,
                environment = env,
            )

        val created =
            client.createJournalpost(
                journalpostRequest =
                    JournalpostRequest(
                        avsenderMottaker = null,
                        bruker = null,
                        dokumenter = emptyList(),
                        eksternReferanseId = "ekstern-referanse-id",
                        journalfoerendeEnhet = null,
                        kanal = null,
                        tittel = null,
                        tema = null,
                        journalpostType = "TEST_TYPE",
                        sak = null,
                    )
            )

        created.getOrNull().shouldNotBeNull()
    }
}
