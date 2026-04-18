package me.ezra_home.retail_software_solution.locations.business.purchase

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
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

  @Column(name = "location_product_id", nullable = false)
  override var locationProductId: UUID,

  @Column(name = "quantity_ordered", nullable = false, precision = 15, scale = 3)
  var quantityOrdered: BigDecimal,

  @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
  var unitCost: BigDecimal,

  @Column(name = "quantity_delivered", nullable = false, precision = 15, scale = 3)
  var quantityDelivered: BigDecimal = BigDecimal.ZERO,

  @Column(name = "quantity_canceled", nullable = false, precision = 15, scale = 3)
  var quantityCanceled: BigDecimal = BigDecimal.ZERO

) : HasLocationProduct, HasReferenceEntity() {

  fun getExpectedQuantity(): BigDecimal {
    return quantityOrdered.subtract(quantityCanceled)
  }

  fun getTotalCost(): BigDecimal {
    return unitCost.multiply(getExpectedQuantity())
  }

}
