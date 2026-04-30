package me.ezra_home.retail_software_solution.platform.business

import me.ezra_home.retail_software_solution.platform.business.sysuser.ServiceAccountInserter
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class PlatformBootstrap(private val serviceAccountInserter: ServiceAccountInserter) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        serviceAccountInserter.seed()
    }
}
