package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.JurisdictionTaxTypeCache
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.JurisdictionTypeCache
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class JurisdictionTypeName

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class JurisdictionTaxTypes

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ParentJurisdictionName

@Component
class JurisdictionQualifier(
    private val jurisdictionTypeCache: JurisdictionTypeCache,
    private val jurisdictionTaxTypeCache: JurisdictionTaxTypeCache,
    private val jurisdictionCache: JurisdictionCache
) {

    @JurisdictionTypeName
    fun getJurisdictionTypeName(jurisdictionTypeId: UUID): String? =
        jurisdictionTypeCache.getAll().find { it.id == jurisdictionTypeId }?.name

    @ParentJurisdictionName
    fun getParentJurisdictionName(parentJurisdictionId: UUID?): String? =
        parentJurisdictionId?.let { id -> jurisdictionCache.getAll().find { it.id == id }?.name }

    @JurisdictionTaxTypes
    fun getJurisdictionTaxTypes(jurisdictionId: UUID): List<UUID> {
        return jurisdictionTaxTypeCache.getAll()
            .filter { it.jurisdictionId == jurisdictionId && it.active }
            .map { it.taxTypeId }
    }
}
