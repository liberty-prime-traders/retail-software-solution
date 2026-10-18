package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementTypeConverter
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

  @Column(name = "storage_moved_quantity", nullable = false, precision = 28, scale = 4)
  var storageMovedQuantity: BigDecimal,

  // The source line's whole figure, identical on every movement the line produced: never sum it.
  @Column(name = "user_provided_moved_quantity", nullable = false, precision = 19, scale = 4)
  var userProvidedMovedQuantity: BigDecimal,

  @Column(name = "storage_remaining_quantity", nullable = false, precision = 28, scale = 4)
  var storageRemainingQuantity: BigDecimal,

  @Column(name = "moved_cost", nullable = false, precision = 28, scale = 4)
  var movedCost: BigDecimal,

  @Column(name = "storage_moved_breakdown_text", nullable = false, length = 200, updatable = false)
  var storageMovedBreakdownText: String,

  @Column(name = "storage_remaining_breakdown_text", nullable = false, length = 200, updatable = false)
  var storageRemainingBreakdownText: String,

  @Column(name = "document_reference", length = 30)
  var documentReference: String? = null,

  @Column(name = "detail_reference", length = 30)
  var detailReference: String? = null,

  @Column(name = "unit_id", nullable = false)
  var unitId: UUID,

  @Column(name = "storage_units_per_unit", nullable = false, updatable = false)
  var storageUnitsPerUnit: Long,

  @Column(name = "category", length = 5)
  var category: StockAdjustmentCategory? = null,

  @Column(name = "note", length = 500)
  var note: String? = null

) : HasReferenceEntity()
