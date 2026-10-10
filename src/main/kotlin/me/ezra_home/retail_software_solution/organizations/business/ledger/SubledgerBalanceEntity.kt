package me.ezra_home.retail_software_solution.organizations.business.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.SUBLEDGER_BALANCE)
class SubledgerBalanceEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "running_payable", nullable = false, precision = 19, scale = 4)
    var runningPayable: BigDecimal,

    @Column(name = "running_receivable", nullable = false, precision = 19, scale = 4)
    var runningReceivable: BigDecimal

) : HasCreatorEntity()
