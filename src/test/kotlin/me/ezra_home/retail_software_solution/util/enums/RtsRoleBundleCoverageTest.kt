package me.ezra_home.retail_software_solution.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RtsRoleBundleCoverageTest {

    // Privilege-escalation-shaped permissions (granting further roles/permissions) are
    // deliberately kept add-on-only — bundling them into a general-purpose role would make
    // escalation implicit. Extend this set only for permissions with that same shape.
    private val bundleExemptPermissions = setOf(
        RtsPermission.MANAGE_ORGANIZATION_ACCESS,
        RtsPermission.MANAGE_LOCATION_ACCESS
    )

    @Test
    fun `every non-exempt RtsPermission is reachable through at least one RtsRole bundle`() {
        val bundledPermissions = RtsRole.entries.flatMap { it.permissions }.toSet()
        val unreachable = RtsPermission.entries.filterNot { it in bundledPermissions || it in bundleExemptPermissions }

        assertTrue(
            unreachable.isEmpty(),
            "These permissions can only ever be granted as add-ons, never via a role: $unreachable"
        )
    }
}
