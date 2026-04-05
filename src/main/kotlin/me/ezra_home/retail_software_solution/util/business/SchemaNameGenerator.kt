package me.ezra_home.retail_software_solution.util.business

import java.text.Normalizer

internal object SchemaNameGenerator {

    fun generateSubDomain(suggestedName: String): String {
        val normalized = Normalizer.normalize(suggestedName, Normalizer.Form.NFD)
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

    fun generateSchemaName(suggestedName: String, prefix: String): String {
        val subDomain = generateSubDomain(suggestedName)
        return convertTrustedSubdomainToSchema(subDomain, prefix)
    }

    fun convertTrustedSubdomainToSchema(subDomain: String, prefix: String): String {
        return "${prefix}_$subDomain".replace("-", "_")
    }

}
