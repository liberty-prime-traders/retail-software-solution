package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import java.text.Normalizer

object SubdomainGenerator {

    fun generateSubdomain(suggestedSubdomain: String): String {
        val normalized = Normalizer.normalize(suggestedSubdomain, Normalizer.Form.NFD)
            .replace("[^\\p{ASCII}]".toRegex(), "")

        var subdomain = normalized.lowercase()
            .replace("[^a-z0-9]".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')

        subdomain = subdomain.dropWhile { it.isDigit() }.trim('-')

        if (subdomain.length > 63) {
            subdomain = subdomain.substring(0, 63).trim('-')
        }

        return subdomain
    }
}
