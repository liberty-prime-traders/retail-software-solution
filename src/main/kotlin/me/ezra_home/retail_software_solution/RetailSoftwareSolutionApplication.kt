package me.ezra_home.retail_software_solution

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
class RetailSoftwareSolutionApplication

fun main(args: Array<String>) {
	runApplication<RetailSoftwareSolutionApplication>(*args)
}
