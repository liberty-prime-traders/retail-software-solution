package me.ezra_home.retail_software_solution.configuration.security

import org.springframework.security.core.Authentication

fun interface OktaIdExtractor {
    fun extract(authentication: Authentication): String
}
