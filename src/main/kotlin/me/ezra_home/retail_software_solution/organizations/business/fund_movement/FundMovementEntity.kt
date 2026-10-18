package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = TableNames.FUND_MOVEMENT)
@HasReference(tableName = TableName.FUND_MOVEMENT)
class FundMovementEntity(

    @Convert(converter = FundMovementTypeConverter::class)
    @Column(name = "type", nullable = false, updatable = false, length = 5)
    var type: FundMovementType,

    @Column(name = "debit_account_code", nullable = false, updatable = false)
    var debitAccountCode: String,

    @Column(name = "credit_account_code", nullable = false, updatable = false)
    var creditAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,

    @Column(name = "movement_date", nullable = false, updatable = false)
    var movementDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity()
