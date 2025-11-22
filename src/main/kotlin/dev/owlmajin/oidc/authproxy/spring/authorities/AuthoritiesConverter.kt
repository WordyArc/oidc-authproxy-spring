package dev.owlmajin.oidc.authproxy.spring.authorities

import dev.owlmajin.oidc.authproxy.spring.config.OidcProperties
import dev.owlmajin.oidc.authproxy.spring.support.buildCache
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils

class AuthoritiesConverter(private val properties: OidcProperties) {

    companion object {
        private val log = LoggerFactory.getLogger(AuthoritiesConverter::class.java)

        // Поиск переменных внутри template: ${varName}
        private val TEMPLATE_VAR_REGEX = "\\$\\{(\\w+)}".toRegex()

        private const val DEFAULT_CACHE_MAX_SIZE = 10_000L
    }

    private val cache = buildCache<Map<String, Any?>, List<String>>(
        "authorities",
        properties.authorities.cacheTtl,
        DEFAULT_CACHE_MAX_SIZE
    )

    fun convert(claims: Map<String, Any?>): Collection<GrantedAuthority> {
        val authorityNames: List<String> = cache
            ?.get(claims) { extractAuthorities(it) }
            ?: extractAuthorities(claims)

        return AuthorityUtils.createAuthorityList(*authorityNames.toTypedArray())
    }

    private fun extractAuthorities(claims: Map<String, Any?>): List<String> {
        val mappings = properties.authorities.mappings
        if (mappings.isEmpty()) {
            val raw = mutableListOf<String>()
            (claims["roles"] as? Collection<*>)?.forEach { raw += it.toString() }
            (claims["scope"] as? String)
                ?.split(' ')
                ?.filter { it.isNotBlank() }
                ?.forEach { raw += it }

            if (log.isTraceEnabled) log.trace("Fallback authorities $raw")
            return raw.distinct()
        }

        val flat = flattenClaims(claims)
        val result = mutableSetOf<String>()
        val compiledMappings: List<Pair<Regex, OidcProperties.Authorities.Mapping>> =
            mappings.map { it.pattern.toRegex() to it }

        for ((path, value) in flat) {
            if (value == null) continue

            for ((regex, mapping) in compiledMappings) {
                val match = regex.find(path) ?: continue

                when (value) {
                    is Collection<*> -> value.forEach {
                        addFromMatch(mapping, match, it.toString(), result)
                    }

                    else -> addFromMatch(mapping, match, value.toString(), result)
                }
            }
        }

        if (log.isTraceEnabled) log.trace("Extracted authorities: $result")

        return result.toList()
    }

    private fun addFromMatch(
        mapping: OidcProperties.Authorities.Mapping,
        match: MatchResult,
        roleValue: String,
        acc: MutableSet<String>
    ) {
        val template = mapping.template

        val role =
            if (template.isNullOrBlank()) {
                roleValue
            } else {
                TEMPLATE_VAR_REGEX.replace(template) { matchResult ->
                    when (val varName = matchResult.groupValues[1]) {
                        "role" -> roleValue
                        else -> match.groups[varName]?.value.orEmpty()
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
        if (source.isEmpty()) return emptyMap()

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
