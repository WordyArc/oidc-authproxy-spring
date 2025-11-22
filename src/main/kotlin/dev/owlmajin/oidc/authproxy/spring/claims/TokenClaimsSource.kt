package dev.owlmajin.oidc.authproxy.spring.claims

import org.springframework.security.oauth2.jwt.Jwt

class TokenClaimsSource : ClaimsSource {
    override fun getClaims(jwt: Jwt): Map<String, Any?> = jwt.claims
}