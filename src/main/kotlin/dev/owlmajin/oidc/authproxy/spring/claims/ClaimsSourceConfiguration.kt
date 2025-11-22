package dev.owlmajin.oidc.authproxy.spring.claims

import dev.owlmajin.oidc.authproxy.spring.config.AuthoritiesSource
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ClaimsSourceConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebClient.Builder::class)
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

    @Bean
    @ConditionalOnMissingBean(ClaimsSource::class)
    fun claimsSource(
        properties: OidcProperties,
        builder: WebClient.Builder
    ): ClaimsSource =
        when (properties.authorities.source) {
            AuthoritiesSource.TOKEN -> TokenClaimsSource()
            AuthoritiesSource.USERINFO -> UserinfoClaimsSource(properties, builder)
        }
}
