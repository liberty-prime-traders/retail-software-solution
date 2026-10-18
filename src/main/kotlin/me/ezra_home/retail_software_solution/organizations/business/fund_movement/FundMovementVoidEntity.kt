package me.ezra_home.retail_software_solution.organizations.business.fund_movement

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
@Table(name = TableNames.FUND_MOVEMENT_VOID)
@HasReference(tableName = TableName.FUND_MOVEMENT_VOID)
class FundMovementVoidEntity(

    @Column(name = "fund_movement_id", nullable = false, updatable = false, unique = true)
    var fundMovementId: UUID,

    @Column(name = "void_date", nullable = false, updatable = false)
    var voidDate: LocalDate,

    @Column(name = "reason", updatable = false)
    var reason: String?

) : ImmutableEntity()
