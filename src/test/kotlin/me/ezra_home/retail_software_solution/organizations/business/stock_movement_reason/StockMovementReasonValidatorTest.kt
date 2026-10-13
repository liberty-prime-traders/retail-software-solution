package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonInsertDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class StockMovementReasonValidatorTest {

    private val cache = mock(StockMovementReasonCache::class.java)
    private val validator = StockMovementReasonValidator(cache)

    init {
        `when`(cache.getAll()).thenReturn(emptyList())
    }

    @Test
    fun `an org reason with no movement types is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            validator.validateInsert(StockMovementReasonInsertDto(name = "Samples", movementTypes = emptySet()))
        }
    }

    @Test
    fun `an org reason may apply to any movement types`() {
        validator.validateInsert(StockMovementReasonInsertDto(name = "Samples", movementTypes = MovementType.entries.toSet()))
    }

    @Test
    fun `clearing the movement types of an org reason is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            validator.validateUpdate(StockMovementReasonUpdateDto(id = UUID.randomUUID(), movementTypes = emptySet()))
        }
    }
}
