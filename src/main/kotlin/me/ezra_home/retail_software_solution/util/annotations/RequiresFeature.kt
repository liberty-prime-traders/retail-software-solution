package me.ezra_home.retail_software_solution.util.annotations

import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeature(val value: Feature)
