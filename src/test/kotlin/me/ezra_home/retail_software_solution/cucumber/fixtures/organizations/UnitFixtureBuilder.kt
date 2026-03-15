package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Component
class UnitFixtureBuilder(private val requestFactory: AuthenticatedRequestFactory) {

  fun createBaseUnit(
    groupDto: UnitGroupInsertDto = defaultGroupDto(),
    valueDto: UnitValueInsertDto? = null
  ): UUID {
    val groupId = createUnitGroup(groupDto)
    return createUnitValue(valueDto ?: defaultValueDto(groupId))
  }

  private fun defaultGroupDto() = UnitGroupInsertDto(
    name = "Units-${UUID.randomUUID().toString().take(8)}"
  )

  private fun defaultValueDto(unitGroupId: UUID): UnitValueInsertDto {
    val seed = UUID.randomUUID().toString().take(8)
    return UnitValueInsertDto(
      name = "Piece-$seed",
      code = "P${seed.takeLast(4)}",
      unitGroupId = unitGroupId
    )
  }

  private fun createUnitGroup(dto: UnitGroupInsertDto): UUID {
    val response = requestFactory.jsonRequest().body(dto).post("/secured/unitgroups")
    assertEquals(200, response.statusCode, "Failed to create unit group. Response: ${response.asString()}")
    val id = response.jsonPath().getString("id")
    assertNotNull(id, "Unit group response missing id")
    return UUID.fromString(id)
  }

  private fun createUnitValue(dto: UnitValueInsertDto): UUID {
    val response = requestFactory.jsonRequest().body(dto).post("/secured/unitvalues")
    assertEquals(200, response.statusCode, "Failed to create unit value. Response: ${response.asString()}")
    val id = response.jsonPath().getString("id")
    assertNotNull(id, "Unit value response missing id")
    return UUID.fromString(id)
  }
}
