package me.ezra_home.retail_software_solution.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RtsRoleNamesTest {

    @Test
    fun `every RtsRoleNames constant matches an RtsRole entry name, and vice versa`() {
        val declaredNames = RtsRoleNames::class.java.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNames = RtsRole.entries.map { it.name }.toSet()

        assertEquals(enumNames, declaredNames)
    }
}
