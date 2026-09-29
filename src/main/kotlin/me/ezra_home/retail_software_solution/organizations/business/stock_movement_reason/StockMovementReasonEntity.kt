package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.STOCK_MOVEMENT_REASON)
@HasReference(tableName = TableName.STOCK_MOVEMENT_REASON)
class StockMovementReasonEntity(

  @Column(name = "code", length = 20, updatable = false)
  var code: String? = null,

  @Column(name = "name", nullable = false, length = 100)
  var name: String,

  @Column(name = "description")
  var description: String? = null,

  @Column(name = "system_defined", nullable = false, updatable = false)
  var systemDefined: Boolean = false

) : HasReferenceEntity()
