package me.ezra_home.retail_software_solution.locations.business.location_product

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.product.ProductStatusConverter
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.LOCATION_PRODUCT)
@HasReference(tableName = TableName.LOCATION_PRODUCT)
class LocationProductEntity(

  @Column(name = "product_id", nullable = false, unique = true)
  var productId: UUID,

  @Column(name = "name", nullable = false, length = 100)
  var productName: String,

  @Column(name = "description", length = 500)
  var description: String? = null,

  @Column(name = "product_group_name", nullable = false, length = 100)
  var productGroupName: String,

  @Column(name = "category_id", nullable = false)
  var categoryId: UUID,

  @Column(name = "base_unit_id", nullable = false)
  var baseUnitId: UUID,

  @Column(name = "default_sale_price", precision = 15, scale = 2)
  var defaultSalePrice: BigDecimal? = null,

  @Column(name = "min_stock_level")
  var minStockLevel: Int? = null,

  @Column(name = "last_purchase_price", precision = 15, scale = 2)
  var lastPurchasePrice: BigDecimal? = null,

  @Convert(converter = ProductStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: ProductStatus = ProductStatus.ACTIVE,

  @NotAudited
  @Column(name = "last_synced_at")
  var lastSyncedAt: OffsetDateTime? = null

) : HasReferenceEntity()
