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
@Table(name = TableNames.LOAN_REPAYMENT)
@HasReference(tableName = TableName.LOAN_REPAYMENT)
class LoanRepaymentEntity(

    @Column(name = "loan_id", nullable = false, updatable = false)
    var loanId: UUID,

    @Column(name = "asset_account_code", nullable = false, updatable = false)
    var assetAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "repayment_date", nullable = false, updatable = false)
    var repaymentDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
