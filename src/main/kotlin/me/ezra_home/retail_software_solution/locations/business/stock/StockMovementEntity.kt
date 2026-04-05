package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_MOVEMENT)
@HasReference(tableName = TableName.STOCK_MOVEMENT)
class StockMovementEntity(

  @Column(name = "stock_entry_id", nullable = false)
  var stockEntryId: UUID,

  @NotAudited
  @Column(name = "location_product_id", nullable = false)
  var locationProductId: UUID,

  @Convert(converter = MovementTypeConverter::class)
  @Column(name = "movement_type", nullable = false, length = 5)
  var movementType: MovementType,

  @Column(name = "reason_id")
  var reasonId: UUID? = null,

  @Column(name = "moved_quantity", nullable = false, precision = 15, scale = 3)
  var movedQuantity: BigDecimal,

  @Column(name = "remaining_quantity", nullable = false, precision = 15, scale = 3)
  var remainingQuantity: BigDecimal,

  @Column(name = "reference_id")
  var referenceId: UUID? = null

) : HasReferenceEntity()
