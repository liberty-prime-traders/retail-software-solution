package me.ezra_home.retail_software_solution.organizations.business.loan.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.time.Instant
import java.util.UUID

object LoanSearchRowMapper {

    fun fromTuple(tuple: Tuple): LoanSearchRow = LoanSearchRow(
        id = tuple.get("id", UUID::class.java),
        createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java)))
    )
}
