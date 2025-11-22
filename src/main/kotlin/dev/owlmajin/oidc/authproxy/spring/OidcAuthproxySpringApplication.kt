package dev.owlmajin.oidc.authproxy.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OidcAuthproxySpringApplication

fun main(args: Array<String>) {
	runApplication<OidcAuthproxySpringApplication>(*args)
}
