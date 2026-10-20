package me.ezra_home.retail_software_solution.organizations.business.loan

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import me.ezra_home.retail_software_solution.util.model.ViewNames
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.util.UUID

@Entity
@Immutable
@Table(name = ViewNames.LOAN_STATUS)
class LoanStatusEntity(

    @Id
    @Column(name = "loan_id")
    var loanId: UUID,

    @Column(name = "amount_repaid")
    var amountRepaid: BigDecimal,

    @Column(name = "outstanding")
    var outstanding: BigDecimal,

    @Column(name = "status", length = 5)
    var status: LoanStatus
)
