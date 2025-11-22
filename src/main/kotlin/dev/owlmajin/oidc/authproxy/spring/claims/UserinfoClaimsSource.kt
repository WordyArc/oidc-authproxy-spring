package dev.owlmajin.oidc.authproxy.spring.claims

import org.springframework.web.reactive.function.client.WebClient
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import dev.owlmajin.oidc.authproxy.spring.support.buildCache
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

class UserinfoClaimsSource(
    private val properties: OidcProperties,
    webClientBuilder: WebClient.Builder
) : ClaimsSource {

    private companion object {
        private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(5)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val client: WebClient = webClientBuilder.build()

    private val cache = buildCache<String, Map<String, Any?>?>(
        "userinfo",
        properties.authorities.cacheTtl,
        10_000L,
    )

    override fun getClaims(jwt: Jwt): Map<String, Any?> {
        val userinfoUri = properties.userinfoUri
            ?: error("oidc.userinfo-uri is required when authorities.source=USERINFO")

        val loader = { fetchUserinfo(userinfoUri, jwt) ?: emptyMap() }
        return cache
            ?.get(jwt.tokenValue) { loader() }
            ?: loader()
    }

    private fun fetchUserinfo(uri: String, jwt: Jwt): Map<String, Any?>? {
        return client.get()
            .uri(uri)
            .headers { it.setBearerAuth(jwt.tokenValue) }
            .exchangeToMono { response ->
                if (response.statusCode() != HttpStatus.OK) {
                    log.warn("Invalid userinfo response status: ${response.statusCode()}")
                    error("Invalid userinfo response status: ${response.statusCode()}")
                }
                response.bodyToMono<Map<String, Any?>>()
            }
            .block(DEFAULT_TIMEOUT)
    }
}
