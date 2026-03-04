package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE)
@HasReference(tableName = TableName.PURCHASE)
class PurchaseEntity(

  @Column(name = "supplier_id", nullable = false)
  var supplierId: UUID,

  @Convert(converter = PurchaseStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: PurchaseStatus = PurchaseStatus.DRAFT,

  @Column(name = "notes")
  var notes: String? = null,

  @Column(name = "date_ordered")
  var dateOrdered: OffsetDateTime? = null,

  @Column(name = "ordered_by")
  var orderedById: UUID? = null

) : HasReferenceEntity()
