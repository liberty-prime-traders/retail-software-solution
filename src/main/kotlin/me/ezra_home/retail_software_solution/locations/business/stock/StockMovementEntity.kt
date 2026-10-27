package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategoryConverter
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

  /**
   * This movement's exact slice in storage units: the figure to sum or compare. `wholeLineQuantityText` is for display.
   */
  @Column(name = "storage_moved_quantity", nullable = false, precision = 28, scale = 4)
  var storageMovedQuantity: BigDecimal,

  /**
   * The quantity exactly as the user typed it for the whole document line (the stock-in or stock-out), in the unit
   * they chose, pluralized ("2.5 Bales"). Frozen at write time like the breakdown texts, and copied unchanged onto
   * every movement that line produces, so a stock-out spanning several FIFO layers repeats it on each.
   */
  @Column(name = "whole_line_quantity_text", nullable = false, length = 200, updatable = false)
  var wholeLineQuantityText: String,

  @Column(name = "storage_remaining_quantity", nullable = false, precision = 28, scale = 4)
  var storageRemainingQuantity: BigDecimal,

  @Column(name = "moved_cost", nullable = false, precision = 28, scale = 4)
  var movedCost: BigDecimal,

  /**
   * This and `storageRemainingBreakdownText` are snapshots rendered at write time: history must keep showing the units
   * as they were, not re-render against unit sizes that have changed since.
   */
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

  @Column(name = "storage_units_per_user_provided_unit", nullable = false, updatable = false)
  var storageUnitsPerUserProvidedUnit: Long,

  @Column(name = "category", length = 5)
  @Convert(converter = StockAdjustmentCategoryConverter::class)
  var category: StockAdjustmentCategory? = null,

  @Column(name = "note", length = 500)
  var note: String? = null

) : HasReferenceEntity()
