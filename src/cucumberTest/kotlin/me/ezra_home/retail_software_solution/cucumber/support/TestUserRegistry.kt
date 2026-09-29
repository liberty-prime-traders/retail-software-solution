package me.ezra_home.retail_software_solution.cucumber.support

import java.util.UUID

/**
 * Test-only stand-in for what a real login would give us: since cucumber authenticates via a fake
 * JWT (TestAuthenticationFilter) rather than a real Google login, the fixture-created sys_user ids
 * have to be shared somewhere both the seeder and the fake JWT issuer can read them from.
 */
object TestUserRegistry {
    var platformAdminUserId: UUID? = null
    var organizationUserId: UUID? = null
}
