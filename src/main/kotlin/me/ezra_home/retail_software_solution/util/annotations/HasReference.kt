package me.ezra_home.retail_software_solution.util.annotations

import me.ezra_home.retail_software_solution.util.model.TableName

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasReference(
    val tableName: TableName
)
