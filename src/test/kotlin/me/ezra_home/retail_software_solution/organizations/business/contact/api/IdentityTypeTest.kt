package me.ezra_home.retail_software_solution.organizations.business.contact.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdentityTypeTest {

  @Test
  fun `a non-blank company name makes an organization and anything else an individual`() {
    assertEquals(IdentityType.ORGANIZATION, IdentityType.forCompanyName("Acme"))
    assertEquals(IdentityType.INDIVIDUAL, IdentityType.forCompanyName(null))
    assertEquals(IdentityType.INDIVIDUAL, IdentityType.forCompanyName(""))
    assertEquals(IdentityType.INDIVIDUAL, IdentityType.forCompanyName("   "))
  }
}
