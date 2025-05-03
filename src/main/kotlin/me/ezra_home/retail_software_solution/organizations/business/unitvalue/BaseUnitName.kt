package me.ezra_home.retail_software_solution.business.unitvalue

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class BaseUnitName()
