package me.ezra_home.retail_software_solution.organizations.business.contact.api

enum class IdentityType {
    ORGANIZATION,
    INDIVIDUAL;

    companion object {
        fun forCompanyName(companyName: String?): IdentityType =
            if (companyName.isNullOrBlank()) INDIVIDUAL else ORGANIZATION
    }
}
