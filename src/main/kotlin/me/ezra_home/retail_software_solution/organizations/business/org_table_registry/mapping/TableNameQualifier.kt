package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping

import org.mapstruct.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class TableNameQualifier
