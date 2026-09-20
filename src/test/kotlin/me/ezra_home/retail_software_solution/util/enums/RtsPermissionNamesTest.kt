package me.ezra_home.retail_software_solution.util.enums

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RtsPermissionNamesTest {

    @Test
    fun `every RtsPermissionNames constant matches an RtsPermission entry name, and vice versa`() {
        val declaredNames = RtsPermissionNames::class.java.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNames = RtsPermission.entries.map { it.name }.toSet()

        assertEquals(enumNames, declaredNames)
    }
}
