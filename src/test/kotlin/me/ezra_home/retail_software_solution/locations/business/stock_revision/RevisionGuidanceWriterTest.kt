package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.GuidanceParagraphDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class RevisionGuidanceWriterTest {

    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pc", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))), unitId
    )

    private fun assessment(
        enteredQuantity: String,
        replayProblems: List<BlockingProblem>,
        takenQuantity: String,
        movementTypes: Set<MovementType> = setOf(MovementType.SALE),
        reservationProblems: List<BlockingProblem> = emptyList(),
        quantityHeld: String = "0",
        quantityLeft: String = "0"
    ) = DeliveryLineRevisionAssessment(
        validatedDeliveryLineRevision = ValidatedDeliveryLineRevisionDto(
            revisedDeliveryLineId = UUID.randomUUID(),
            deliveryReferenceNumber = "DEL1",
            revisedLineReferenceNumber = "DELN1",
            purchaseLineId = UUID.randomUUID(),
            locationProductId = UUID.randomUUID(),
            revisedUnitId = unitId,
            revisedStorageUnitsPerUserProvidedUnit = 1L,
            unitId = unitId,
            storageUnitsPerUserProvidedUnit = 1L,
            revisedUserProvidedQuantityDelivered = BigDecimal("20"),
            revisedUnitCost = BigDecimal("10.0000"),
            userProvidedQuantityDelivered = BigDecimal(enteredQuantity),
            unitCost = BigDecimal("10.0000"),
            problems = emptyList(),
        ),
        quantityBreakdown = quantityBreakdown,
        replayResult = ReplayResult(
            emptyList(), replayProblems, ExactQuantity.ofStorage(BigDecimal(takenQuantity)), movementTypes
        ),
        reservationShortfall = reservationProblems.takeIf { it.isNotEmpty() }?.let {
            ReservationShortfall(
                it, "Sugar", ExactQuantity.ofStorage(BigDecimal(quantityLeft)), ExactQuantity.ofStorage(BigDecimal(quantityHeld))
            )
        },
    )

    private fun problem(reference: String) = BlockingProblem(reference, "Sale.", "Enter more.")

    @Test
    fun `the quantity compared with what the change leaves is everything that drew from the delivery`() {
        val guidance = RevisionGuidanceWriter.write(assessment("12", listOf(problem("SAL1150")), "13"))

        assertEquals(
            "SAL1150 cannot be fully covered. Together, the records that drew from this delivery took 13 pcs, " +
                "and your change leaves 12 pcs.",
            guidance.first().segments.joinToString("") { it.text }
        )
    }

    @Test
    fun `uncovered records are named with the quantity they took and the quantity left`() {
        val guidance = RevisionGuidanceWriter.write(
            assessment("2", listOf(problem("SAL897"), problem("SAL851")), "6")
        )

        assertEquals(
            listOf("SAL851", "SAL897", "6 pcs", "2 pcs"),
            guidance.first().segments.filter { it.emphasis }.map { it.text }
        )
    }

    @Test
    fun `blocked records always come with the possible explanations`() {
        val guidance = RevisionGuidanceWriter.write(assessment("2", listOf(problem("SAL851")), "4"))

        assertEquals(5, guidance.size)
        assertTrue(guidance[3].segments.single().text.contains("correct or void it first"))
    }

    @Test
    fun `removing the delivery completely adds a paragraph about voiding the sales first`() {
        val guidance = RevisionGuidanceWriter.write(assessment("0", listOf(problem("SAL851")), "6"))

        assertEquals(6, guidance.size)
        assertTrue(guidance.last().segments.single().text.contains("must be voided first"))
    }

    @Test
    fun `no guidance is written when an uncovered consumer is not a sale`() {
        val movementTypes = setOf(MovementType.SALE, MovementType.TRANSFER_OUT)

        assertTrue(
            RevisionGuidanceWriter.write(assessment("2", listOf(problem("TRF1")), "4", movementTypes)).isEmpty()
        )
    }

    @Test
    fun `no sales guidance is written for blocking rows that name no uncovered consumer type`() {
        assertTrue(
            RevisionGuidanceWriter.write(assessment("2", listOf(problem("ADJ1")), "0", emptySet())).isEmpty()
        )
    }

    private fun textOf(guidance: List<GuidanceParagraphDto>) = guidance.single().segments.joinToString("") { it.text }

    @Test
    fun `one draft sale is named with what it holds and what the change leaves`() {
        val guidance = RevisionGuidanceWriter.write(
            assessment(
                "2", emptyList(), "0", reservationProblems = listOf(problem("SAL9 (draft)")),
                quantityHeld = "9", quantityLeft = "8"
            )
        )

        assertEquals(
            "Draft sale SAL9 (draft) holds 9 pcs of Sugar, but your change would leave only 8 pcs in stock. " +
                "Nothing has been sold on it yet. Reduce or remove the product on that draft, " +
                "or check that the quantity you are updating on this delivery is right.",
            textOf(guidance)
        )
        assertEquals(
            listOf("SAL9 (draft)", "9 pcs", "8 pcs"),
            guidance.single().segments.filter { it.emphasis }.map { it.text }
        )
    }

    @Test
    fun `several draft sales are named with what they hold between them`() {
        val guidance = RevisionGuidanceWriter.write(
            assessment(
                "2", emptyList(), "0", reservationProblems = listOf(problem("SAL9 (draft)"), problem("SAL8 (draft)")),
                quantityHeld = "10", quantityLeft = "8"
            )
        )

        assertEquals(
            "Draft sales SAL8 (draft), SAL9 (draft) hold 10 pcs of Sugar between them, but your change would " +
                "leave only 8 pcs in stock. Nothing has been sold on them yet. Reduce or remove the product on " +
                "those drafts, or check that the quantity you are updating on this delivery is right.",
            textOf(guidance)
        )
    }

    @Test
    fun `an assessment with no blocking problems has no guidance`() {
        assertTrue(RevisionGuidanceWriter.write(assessment("2", emptyList(), "0")).isEmpty())
    }
}
