package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.OPENING_BALANCE)
@HasReference(tableName = TableName.OPENING_BALANCE)
class OpeningBalanceEntity(

    @Column(name = "account_code", nullable = false, updatable = false)
    var accountCode: String,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : ImmutableEntity()
