package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.MANUAL_JOURNAL)
@HasReference(tableName = TableName.MANUAL_JOURNAL)
class ManualJournalEntity(

    @Column(name = "summary", nullable = false, updatable = false, columnDefinition = "TEXT")
    var summary: String,

    @Column(name = "details", updatable = false, columnDefinition = "TEXT")
    var details: String? = null,

    @Column(name = "posting_date", nullable = false, updatable = false)
    var postingDate: LocalDate,

    @Column(name = "reversal_of_manual_journal_id", updatable = false, unique = true)
    var reversalOfManualJournalId: UUID? = null

) : ImmutableEntity() {

    fun requiredId(): UUID = requireNotNull(id) { "ManualJournalEntity is missing its id" }
}
