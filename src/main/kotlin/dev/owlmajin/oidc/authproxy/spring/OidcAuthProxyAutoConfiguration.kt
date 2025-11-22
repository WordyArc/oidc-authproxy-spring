package dev.owlmajin.oidc.authproxy.spring

import dev.owlmajin.oidc.authproxy.spring.authorities.AuthoritiesConverter
import dev.owlmajin.oidc.authproxy.spring.claims.ClaimsSource
import dev.owlmajin.oidc.authproxy.spring.claims.ClaimsSourceConfiguration
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import dev.owlmajin.oidc.authproxy.spring.security.JwtAuthProxyConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@AutoConfiguration
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration::class)
@EnableConfigurationProperties(OidcProperties::class)
@Import(ClaimsSourceConfiguration::class)
class OidcAuthProxyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun authoritiesConverter(properties: OidcProperties): AuthoritiesConverter =
        AuthoritiesConverter(properties)

    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthProxyConverter(
        properties: OidcProperties,
        claimsSource: ClaimsSource,
        authoritiesConverter: AuthoritiesConverter
    ): JwtAuthProxyConverter =
        JwtAuthProxyConverter(properties, claimsSource, authoritiesConverter)

    @Bean
    @ConditionalOnMissingBean(JwtDecoder::class)
    fun jwtDecoder(properties: OidcProperties): JwtDecoder {
        val jwkSetUri = properties.jwkSetUri
        val issuerUri = properties.issuerUri

        return when {
            !jwkSetUri.isNullOrBlank() ->
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()

            !issuerUri.isNullOrBlank() ->
                NimbusJwtDecoder.withIssuerLocation(issuerUri).build()

            else -> error("Either oidc.jwk-set-uri or oidc.issuer-uri must be set")
        }
    }

    @Bean
    fun resourceServerCustomizer(jwtAuthProxyConverter: JwtAuthProxyConverter)
            : Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> =
        Customizer { cfg ->
            cfg.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthProxyConverter) }
        }
}