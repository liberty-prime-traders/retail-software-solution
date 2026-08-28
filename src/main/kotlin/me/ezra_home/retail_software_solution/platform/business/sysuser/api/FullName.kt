package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class FullName
