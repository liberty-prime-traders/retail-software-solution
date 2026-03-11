package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SUPPLIER_RETURN)
@HasReference(tableName = TableName.SUPPLIER_RETURN)
class SupplierReturnEntity(

  @Column(name = "supplier_id", nullable = false)
  var supplierId: UUID,

  @Column(name = "purchase_id")
  var purchaseId: UUID? = null,

  @Column(name = "purchase_delivery_id")
  var purchaseDeliveryId: UUID? = null,

  @Column(name = "return_date", nullable = false)
  var returnDate: LocalDate,

  @Column(name = "reason", columnDefinition = "TEXT")
  var reason: String? = null,

  @Column(name = "notes", columnDefinition = "TEXT")
  var notes: String? = null

) : HasReferenceEntity()
