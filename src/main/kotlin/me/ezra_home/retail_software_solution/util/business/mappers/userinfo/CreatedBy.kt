package me.ezra_home.retail_software_solution.util.business.mappers.userinfo

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class CreatedBy
