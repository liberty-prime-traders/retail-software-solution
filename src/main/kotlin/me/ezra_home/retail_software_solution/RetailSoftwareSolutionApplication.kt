package me.ezra_home.retail_software_solution

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class RetailSoftwareSolutionApplication

fun main(args: Array<String>) {
	runApplication<RetailSoftwareSolutionApplication>(*args)
}
