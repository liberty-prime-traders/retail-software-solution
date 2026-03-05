package me.ezra_home.retail_software_solution.configuration.datasource

import org.springframework.core.annotation.AliasFor
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(transactionManager = DataSourceBeanNames.LOCATION_SCHEMA_TRANSACTION_MANAGER)
annotation class TransactionalOnLocationSchema(

    @get:AliasFor(annotation = Transactional::class, attribute = "r")
    val rollbackOn: Array<KClass<out Throwable>> = [Exception::class],

    @get:AliasFor(annotation = Transactional::class, attribute = "propagation")
    val propagation: Propagation = Propagation.REQUIRED,

    @get:AliasFor(annotation = Transactional::class, attribute = "readOnly")
    val readOnly: Boolean = false
)
