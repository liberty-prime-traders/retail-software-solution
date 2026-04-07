package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementType
import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementTypeConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.STOCK_MOVEMENT_REASON_TYPE)
@HasReference(tableName = TableName.STOCK_MOVEMENT_REASON_TYPE)
class StockMovementReasonTypeEntity(

  @Column(name = "stock_movement_reason_id", nullable = false, updatable = false)
  val stockMovementReasonId: UUID,

  @Convert(converter = MovementTypeConverter::class)
  @Column(name = "movement_type", nullable = false, updatable = false, length = 5)
  val movementType: MovementType

) : ExpirableAssignmentEntity()
