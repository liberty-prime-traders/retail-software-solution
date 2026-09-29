package me.ezra_home.retail_software_solution.platform.business.startup_checks.api

interface StartupCheck {
    val name: String
    fun check()
}
