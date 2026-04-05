package me.ezra_home.retail_software_solution.platform.business.organization_join_request.public

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class JoinRequestStatus(override val code: String) : HasCode {
    PENDING("PND"),
    APPROVED("APRVD"),
    DENIED("DND"),
}
