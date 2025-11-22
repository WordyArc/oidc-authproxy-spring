package dev.owlmajin.oidc.authproxy.spring.claims

import org.springframework.security.oauth2.jwt.Jwt

interface ClaimsSource {
    fun getClaims(jwt: Jwt): Map<String, Any?>
}
