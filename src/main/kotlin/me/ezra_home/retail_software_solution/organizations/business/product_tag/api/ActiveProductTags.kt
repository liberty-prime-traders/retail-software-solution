package me.ezra_home.retail_software_solution.organizations.business.product_tag.api

import org.mapstruct.Qualifier

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActiveProductTags
