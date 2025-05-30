package me.ezra_home.retail_software_solution.platform.business.sysuser.mapping

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class CreatedBy
