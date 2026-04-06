package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import org.mapstruct.Context
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
class JurisdictionQualifier {

    @JurisdictionTypeName
    fun getJurisdictionTypeName(jurisdictionTypeId: UUID, @Context ctx: JurisdictionMappingContext): String? =
        ctx.typeNames[jurisdictionTypeId]

    @ParentJurisdictionName
    fun getParentJurisdictionName(parentJurisdictionId: UUID?, @Context ctx: JurisdictionMappingContext): String? =
        parentJurisdictionId?.let { ctx.jurisdictionNames[it] }

    @JurisdictionTaxTypes
    fun getJurisdictionTaxTypes(jurisdictionId: UUID, @Context ctx: JurisdictionMappingContext): List<UUID> =
        ctx.taxTypesByJurisdiction[jurisdictionId] ?: emptyList()
}
