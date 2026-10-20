package me.ezra_home.retail_software_solution.organizations.business.loan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.LOAN)
@HasReference(tableName = TableName.LOAN)
class LoanEntity(

    @Column(name = "lender_contact_id", nullable = false, updatable = false)
    var lenderContactId: UUID,

    @Column(name = "asset_account_code", nullable = false, updatable = false)
    var assetAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "loan_date", nullable = false, updatable = false)
    var loanDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
