package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock.api.FifoLayer
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSourceConverter
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_ENTRY)
@HasReference(tableName = TableName.STOCK_ENTRY)
class StockEntryEntity(

  @Column(name = "location_product_id", nullable = false)
  var locationProductId: UUID,

  @Column(name = "source_type", length = 5)
  @Convert(converter = StockItemSourceConverter::class)
  var sourceType: StockItemSource? = null,

  @Column(name = "document_reference", length = 30)
  var documentReference: String? = null,

  @Column(name = "detail_reference", length = 30)
  var detailReference: String? = null,

  @Column(name = "storage_batch_size", nullable = false, precision = 28, scale = 4, updatable = false)
  override var storageBatchSize: BigDecimal,

  @Column(name = "storage_quantity_remaining", nullable = false, precision = 28, scale = 4)
  override var storageQuantityRemaining: BigDecimal,

  @Column(name = "priority")
  var priority: Int,

  // Derived from batchCost / storageBatchSize, for display and grouping layers of equal rate: costing prorates batchCost, never multiplies this.
  @Column(name = "unit_cost", nullable = false, precision = 28, scale = 10)
  var unitCost: BigDecimal,

  @Column(name = "batch_cost", nullable = false, precision = 28, scale = 4, updatable = false)
  override var batchCost: BigDecimal,

  @Column(name = "cost_remaining", nullable = false, precision = 28, scale = 4)
  override var costRemaining: BigDecimal

) : HasReferenceEntity(), FifoLayer
