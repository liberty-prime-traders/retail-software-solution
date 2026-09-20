package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class FullName

object FullNames {
    fun of(firstName: String?, lastName: String?): String? =
        listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { null }
}

object Initials {
    fun of(firstName: String?, lastName: String?): String? =
        listOfNotNull(firstName?.firstOrNull(), lastName?.firstOrNull())
            .joinToString("") { it.uppercase() }
            .ifBlank { null }
}
