package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryTypeConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.MANUAL_JOURNAL_LINE)
@HasReference(tableName = TableName.MANUAL_JOURNAL_LINE)
class ManualJournalLineEntity(

    @Column(name = "manual_journal_id", nullable = false, updatable = false)
    var manualJournalId: UUID,

    @Column(name = "account_code", nullable = false, updatable = false, length = 50)
    var accountCode: String,

    @Convert(converter = EntryTypeConverter::class)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 5)
    var entryType: EntryType,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(name = "line_memo", updatable = false, columnDefinition = "TEXT")
    var lineMemo: String? = null

) : HasReferenceEntity()
