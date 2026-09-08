package me.ezra_home.retail_software_solution.platform.business.startup_checks

import me.ezra_home.retail_software_solution.platform.business.startup_checks.api.StartupCheck
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Int.MAX_VALUE)
class StartupCheckRunner(
    private val startupChecks: List<StartupCheck>
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (startupChecks.isEmpty()) return
        log.info("Running {} startup check(s)", startupChecks.size)
        val failures = mutableListOf<Pair<String, Throwable>>()
        startupChecks.forEach { check ->
            try {
                check.check()
                log.info("[startup-check] {} passed", check.name)
            } catch (e: Throwable) {
                log.error("[startup-check] {} failed", check.name, e)
                failures += check.name to e
            }
        }
        if (failures.isNotEmpty()) {
            throw IllegalStateException(
                "${failures.size} startup check(s) failed: ${failures.joinToString { it.first }}"
            )
        }
    }
}
