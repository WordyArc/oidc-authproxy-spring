package dev.owlmajin.oidc.authproxy.spring.claims

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.web.reactive.function.client.WebClient
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

class UserinfoClaimsSource(
    private val properties: OidcProperties,
    webClientBuilder: WebClient.Builder
) : ClaimsSource {

    private val log = LoggerFactory.getLogger(javaClass)

    private val client: WebClient = webClientBuilder.build()

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(properties.authorities.cacheTtl)
        .maximumSize(10_000)
        .build<String, Map<String, Any?>?>()

    override fun getClaims(jwt: Jwt): Map<String, Any?> {
        val userinfoUri = properties.userinfoUri
            ?: error("oidc.userinfo-uri is required when authorities.source=USERINFO")

        return cache.get(jwt.tokenValue) {
            fetchUserinfo(userinfoUri, jwt)
        } ?: emptyMap()
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
            .block(Duration.ofSeconds(5))
    }
}
