package me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeStatus
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.ORG_JURISDICTION_TAX_TYPE)
@HasReference(tableName = TableName.ORG_JURISDICTION_TAX_TYPE)
class OrgJurisdictionTaxTypeEntity(

    @Column(name = "jurisdiction_tax_type_id", nullable = false, updatable = false)
    var jurisdictionTaxTypeId: UUID,

    @Convert(converter = OrgJurisdictionTaxTypeStatusConverter::class)
    @Column(name = "status", length = 5, nullable = false)
    var status: OrgJurisdictionTaxTypeStatus = OrgJurisdictionTaxTypeStatus.ACTIVE

) : HasReferenceEntity()
