package me.ezra_home.retail_software_solution.cross_tier.authority

import java.util.UUID

interface MembershipSummaryProjection {
    fun getUserId(): UUID
    fun getMembershipCount(): Long
    fun getActive(): Boolean
}
