package me.ezra_home.retail_software_solution.business.audit

import me.ezra_home.retail_software_solution.business.util.mapper.RtsMapperConfig
import me.ezra_home.retail_software_solution.business.util.mapper.userinfo.FullName
import me.ezra_home.retail_software_solution.model.entity.audit.AuditEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface AuditMapper {

    @Mapping(target = "createdBy", source = "createdById", qualifiedBy = [FullName::class])
    @Mapping(target = "lastUpdatedBy", source = "lastUpdatedById", qualifiedBy = [FullName::class])
    fun toDto(auditEntity: AuditEntity): AuditDto
}
