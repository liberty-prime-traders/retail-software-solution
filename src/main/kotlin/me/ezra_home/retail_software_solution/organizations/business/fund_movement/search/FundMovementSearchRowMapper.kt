package me.ezra_home.retail_software_solution.organizations.business.fund_movement.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementDto
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementTypeConverter
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementVoidDto
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.FundMovementWithVoid
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object FundMovementSearchRowMapper {

    fun fromTuple(tuple: Tuple): FundMovementWithVoid = FundMovementWithVoid(
        fundMovementDto = FundMovementDto(
            id = tuple.get("id", UUID::class.java),
            referenceNumber = tuple.get("reference_number", String::class.java),
            type = requireNotNull(FundMovementTypeConverter().convertToEntityAttribute(tuple.get("type", String::class.java))),
            debitAccountCode = tuple.get("debit_account_code", String::class.java),
            creditAccountCode = tuple.get("credit_account_code", String::class.java),
            amount = tuple.get("amount", BigDecimal::class.java),
            movementDate = toLocalDate(tuple.get("movement_date")),
            note = tuple.get("note", String::class.java),
            createdById = tuple.get("created_by_id", UUID::class.java),
            createdOn = toOffsetDateTime(tuple, "created_on")
        ),
        fundMovementVoidDto = tuple.get("void_id", UUID::class.java)?.let { voidId ->
            FundMovementVoidDto(
                id = voidId,
                referenceNumber = tuple.get("void_reference_number", String::class.java),
                fundMovementId = tuple.get("id", UUID::class.java),
                voidDate = toLocalDate(tuple.get("void_date")),
                reason = tuple.get("void_reason", String::class.java),
                createdById = tuple.get("void_created_by_id", UUID::class.java),
                createdOn = toOffsetDateTime(tuple, "void_created_on")
            )
        }
    )

    private fun toOffsetDateTime(tuple: Tuple, alias: String) =
        requireNotNull(DateQualifier.toOffsetDateTime(tuple.get(alias, Instant::class.java)))

    // Native date columns arrive as java.sql.Date or LocalDate depending on the driver path.
    private fun toLocalDate(value: Any): LocalDate = when (value) {
        is LocalDate -> value
        is Date -> value.toLocalDate()
        else -> throw IllegalStateException("Unexpected date value type ${value::class.java.name}")
    }
}
