package me.ezra_home.retail_software_solution.locations.business.purchase

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.HasConversionRatio
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE_LINE)
@HasReference(tableName = TableName.PURCHASE_LINE)
class PurchaseLineEntity(

  @Column(name = "purchase_id", nullable = false)
  var purchaseId: UUID,

  @Column(name = "location_product_id", nullable = false, updatable = false)
  override var locationProductId: UUID,

  @Column(name = "quantity_ordered", nullable = false, precision = 19, scale = 4)
  var quantityOrdered: BigDecimal,

  @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
  var unitCost: BigDecimal,

  @Column(name = "unit_id", nullable = false)
  var unitId: UUID,

  @Column(name = "conversion_numerator", nullable = false)
  override var conversionNumerator: Long,

  @Column(name = "conversion_denominator", nullable = false)
  override var conversionDenominator: Long,

  @Column(name = "quantity_delivered", nullable = false, precision = 19, scale = 4)
  var quantityDelivered: BigDecimal = BigDecimal.ZERO,

  @Column(name = "quantity_canceled", nullable = false, precision = 19, scale = 4)
  var quantityCanceled: BigDecimal = BigDecimal.ZERO

) : HasLocationProduct, HasReferenceEntity(), HasConversionRatio {

  fun getExpectedQuantity(): BigDecimal = quantityOrdered.subtract(quantityCanceled)

  fun getRemainingQuantity(): BigDecimal = getExpectedQuantity() - quantityDelivered

  fun getTotalCost(): BigDecimal = Decimals.multiplyScale4(unitCost, getExpectedQuantity())

}
