package me.ezra_home.retail_software_solution.organizations.business.contact.mapping

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ToIdentityType
