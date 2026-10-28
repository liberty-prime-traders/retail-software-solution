package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UnitGroupEntityTest {

    @Test
    fun `names equal on alphanumerics share a name key`() {
        assertEquals(UnitGroupEntity(name = "Pack Size").nameKey, UnitGroupEntity(name = "pack-size").nameKey)
        assertEquals("packsize", UnitGroupEntity(name = "Pack Size").nameKey)
    }

    @Test
    fun `different names have different name keys`() {
        assertNotEquals(UnitGroupEntity(name = "Pack Size").nameKey, UnitGroupEntity(name = "Pack Sizes").nameKey)
    }

    @Test
    fun `the key follows a rename when the entity is refreshed before saving`() {
        val unitGroupEntity = UnitGroupEntity(name = "Boxes")

        unitGroupEntity.name = "Crates"
        UnitGroupNameKeyListener().refreshNameKey(unitGroupEntity)

        assertEquals("crates", unitGroupEntity.nameKey)
    }
}
