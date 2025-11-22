package dev.owlmajin.oidc.authproxy.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(OidcProperties.PREFIX)
data class OidcProperties(
    var issuerUri: String? = null,
    var userinfoUri: String? = null,
    var jwkSetUri: String? = null,
    var usernameClaimName: String? = null,
    var loginClaimName: String? = null,
    var authorities: Authorities = Authorities()
) {
    companion object {
        const val PREFIX = "oidc"
        const val PROP_ISSUER_URI = "$PREFIX.issuer-uri"
        const val PROP_USERINFO_URI = "$PREFIX.userinfo-uri"
        const val PROP_JWKS_URI = "$PREFIX.jwk-set-uri"
    }

    data class Authorities(
        var source: AuthoritiesSource = AuthoritiesSource.TOKEN,
        var cacheTtl: Duration = Duration.ofMinutes(1),
        var mappings: List<Mapping> = emptyList()
    ) {
        data class Mapping(
            var pattern: String = "",
            var template: String? = null
        )
    }
}

enum class AuthoritiesSource {
    TOKEN, USERINFO
}
