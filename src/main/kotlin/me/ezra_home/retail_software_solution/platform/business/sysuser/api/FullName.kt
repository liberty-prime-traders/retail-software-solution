package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class NullableFullName

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class CreatorFullName

object FullNames {
    fun of(firstName: String, lastName: String?): String =
        listOfNotNull(firstName, lastName?.takeIf { it.isNotBlank() }).joinToString(" ")
}

object Initials {
    fun of(firstName: String, lastName: String?): String =
        listOfNotNull(firstName.firstOrNull(), lastName?.firstOrNull())
            .joinToString("") { it.uppercase() }
}
