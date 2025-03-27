package me.ezra_home.retail_software_solution

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableMethodSecurity
class RetailSoftwareSolutionApplication

fun main(args: Array<String>) {
	runApplication<RetailSoftwareSolutionApplication>(*args)
}
