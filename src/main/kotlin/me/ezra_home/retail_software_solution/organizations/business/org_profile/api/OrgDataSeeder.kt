package me.ezra_home.retail_software_solution.organizations.business.org_profile.api

interface OrgDataSeeder {
    fun seed()

    companion object Order {
        const val DEFAULT = 0
        const val UNIT_GROUP = 10
        const val UNIT_VALUE = 20
    }
}
