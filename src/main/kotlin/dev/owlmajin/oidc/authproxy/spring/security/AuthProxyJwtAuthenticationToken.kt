package dev.owlmajin.oidc.authproxy.spring.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class AuthProxyJwtAuthenticationToken(
    jwt: Jwt,
    authorities: Collection<GrantedAuthority>,
    name: String,
    val userInfo: OidcUserInfo
) : JwtAuthenticationToken(
    jwt,
    authorities,
    name
)
