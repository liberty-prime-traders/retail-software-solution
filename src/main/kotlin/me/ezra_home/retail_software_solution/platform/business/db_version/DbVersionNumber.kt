package me.ezra_home.retail_software_solution.platform.business.db_version

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class DbVersionNumber
