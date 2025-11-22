package dev.owlmajin.oidc.authproxy.spring.security

import dev.owlmajin.oidc.authproxy.spring.authorities.AuthoritiesConverter
import dev.owlmajin.oidc.authproxy.spring.claims.ClaimsSource
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.util.StringUtils
import org.springframework.web.server.ResponseStatusException


class JwtAuthProxyConverter(
    private val properties: OidcProperties,
    private val claimsSource: ClaimsSource,
    private val authoritiesConverter: AuthoritiesConverter
) : Converter<Jwt, AbstractAuthenticationToken> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val principalClaimsDefault = listOf(
        "upn",
        "preferred_username",
        IdTokenClaimNames.SUB
    )

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val merged = linkedMapOf<String, Any?>().apply {
            putAll(jwt.claims)
            putAll(claimsSource.getClaims(jwt))
        }

        val username = extractUsername(merged)
        val login = extractLogin(merged)

        if (login != null) {
            merged["userLogin"] = login
        }

        log.trace("Merged user claims: {}", merged)

        val authorities = authoritiesConverter.convert(merged)
        val userInfo = OidcUserInfo(merged)

        return AuthProxyJwtAuthenticationToken(jwt, authorities, username, userInfo)
    }

    private fun extractUsername(claims: Map<String, Any?>): String {
        val configured = properties.usernameClaimName
        val candidates = if (StringUtils.hasText(configured))
            listOf(configured!!)
        else principalClaimsDefault

        return candidates.firstNotNullOfOrNull { claims[it] as? String }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No principal claim in token/userinfo")
    }

    private fun extractLogin(claims: Map<String, Any?>): String? {
        val configured = properties.loginClaimName
        val candidates = when {
            StringUtils.hasText(configured) -> listOf(configured!!)
            else -> listOf("login", "sberpdi")
        }

        return candidates.firstNotNullOfOrNull { claims[it] as? String }
    }
}