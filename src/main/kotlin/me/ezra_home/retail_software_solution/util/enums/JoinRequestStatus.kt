package me.ezra_home.retail_software_solution.util.enums

enum class JoinRequestStatus(override val code: String) : HasCode {
    PENDING("PND"),
    APPROVED("APRVD"),
    DENIED("DND"),
}
