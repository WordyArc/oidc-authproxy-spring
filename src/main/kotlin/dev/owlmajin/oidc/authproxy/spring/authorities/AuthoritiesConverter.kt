package dev.owlmajin.oidc.authproxy.spring.authorities

import com.github.benmanes.caffeine.cache.Caffeine
import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils

class AuthoritiesConverter(private val properties: OidcProperties) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(properties.authorities.cacheTtl)
        .maximumSize(10_000)
        .build<Map<String, Any?>, List<String>>()

    private val templateVarRegex = "\\$\\{(\\w+)}".toRegex()

    fun convert(claims: Map<String, Any?>): Collection<GrantedAuthority> {
        val authorityNames = cache.get(claims) { extractAuthorities(it) }
        return AuthorityUtils.createAuthorityList(*authorityNames.toTypedArray())
    }

    private fun extractAuthorities(claims: Map<String, Any?>): List<String> {
        if (properties.authorities.mappings.isEmpty()) {
            val raw = mutableListOf<String>()
            (claims["roles"] as? Collection<*>)?.forEach { raw += it.toString() }
            (claims["scope"] as? String)?.split(" ")?.forEach { raw += it }
            return raw.distinct()
        }

        val flat = flattenClaims(claims)
        val result = mutableSetOf<String>()

        for ((path, value) in flat) {
            for (mapping in properties.authorities.mappings) {
                val regex = mapping.pattern.toRegex()
                val match = regex.find(path) ?: continue

                when (value) {
                    is Collection<*> -> value.forEach { v ->
                        addFromMatch(mapping, match, v.toString(), result)
                    }

                    else -> addFromMatch(mapping, match, value.toString(), result)
                }
            }
        }

        return result.toList()
    }

    private fun addFromMatch(
        mapping: OidcProperties.Authorities.Mapping,
        match: MatchResult,
        roleValue: String,
        acc: MutableSet<String>
    ) {
        val template = mapping.template

        val role = if (template.isNullOrBlank()) {
            roleValue
        } else {
            templateVarRegex.replace(template) { mr ->
                when (val varName = mr.groupValues[1]) {
                    "role" -> roleValue
                    else -> match.groups[varName]?.value ?: ""
                }
            }
        }

        if (role.isNotBlank()) {
            acc += role
        }
    }

    private fun flattenClaims(
        source: Map<String, Any?>,
        prefix: String = ""
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((k, v) in source) {
            val path = if (prefix.isEmpty()) k else "$prefix/$k"
            when (v) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    result.putAll(flattenClaims(v as Map<String, Any?>, path))
                }

                else -> result[path] = v
            }
        }
        return result
    }
}
