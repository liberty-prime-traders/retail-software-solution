package me.ezra_home.retail_software_solution.organizations.business.ledger

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = TableNames.LEDGER_ENTRY_GROUP)
@HasReference(tableName = TableName.LEDGER_ENTRY_GROUP)
class LedgerEntryGroupEntity(

    @Column(name = "source_reference_number", nullable = false, updatable = false)
    var sourceReferenceNumber: String,

    @Convert(converter = LedgerSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, updatable = false, length = 5)
    var sourceType: LedgerSourceType,

    @Column(name = "source_location_id", updatable = false)
    var sourceLocationId: UUID? = null,

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    var fiscalPeriodId: UUID,

    @Column(name = "posted_on", nullable = false, updatable = false)
    var postedOn: Instant

) : HasReferenceEntity()
