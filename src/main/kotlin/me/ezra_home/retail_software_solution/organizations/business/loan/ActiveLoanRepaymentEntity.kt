package me.ezra_home.retail_software_solution.organizations.business.loan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.ViewNames
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = ViewNames.ACTIVE_LOAN_REPAYMENT)
class ActiveLoanRepaymentEntity(

    @Id
    @Column(name = "id")
    var id: UUID,

    @Column(name = "reference_number")
    var referenceNumber: String,

    @Column(name = "loan_id")
    var loanId: UUID,

    @Column(name = "amount")
    var amount: BigDecimal,

    @Column(name = "created_on")
    var createdOn: OffsetDateTime
)
