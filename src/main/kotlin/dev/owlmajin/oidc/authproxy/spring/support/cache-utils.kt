package dev.owlmajin.oidc.authproxy.spring.support

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import java.time.Duration

private val log = LoggerFactory.getLogger("dev.owlmajin.oidc.authproxy.spring.support.CacheUtilsKt")

fun <K : Any, V : Any?> buildCache(
    name: String = "",
    ttl: Duration?,
    maxSize: Long,
): Cache<K, V>? {
    if (ttl == null || ttl.isZero || ttl.isNegative) {
        log.info("Cache $name disabled (ttl = $ttl)")
        return null
    }

    return Caffeine.newBuilder()
        .expireAfterWrite(ttl)
        .maximumSize(maxSize)
        .build<K, V>()
        .also { log.info("Cache $name enabled (ttl = $ttl, maxSize = $maxSize)") }
}