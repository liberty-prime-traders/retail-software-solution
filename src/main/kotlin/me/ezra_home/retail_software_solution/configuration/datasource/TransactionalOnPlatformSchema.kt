package me.ezra_home.retail_software_solution.configuration.datasource

import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(transactionManager = DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
annotation class TransactionalOnPlatformSchema(
    val rollbackOn: Array<KClass<out Throwable>> = [Exception::class],
    val readOnly: Boolean = false
)
