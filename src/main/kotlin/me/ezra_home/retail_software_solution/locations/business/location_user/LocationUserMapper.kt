package me.ezra_home.retail_software_solution.locations.business.location_user

import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserInsertDto
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface LocationUserMapper {

    fun toDomainDto(entity: LocationUserEntity): LocationUserDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "endOn", ignore = true)
    fun toEntity(insertDto: LocationUserInsertDto): LocationUserEntity

    fun toEntity(dto: LocationUserDto): LocationUserEntity

    @Mapping(source = "userId", target = "user", qualifiedBy = [FullName::class])
    @Mapping(source = "createdOn", target = "startOn")
    fun toDto(dto: LocationUserDto): LocationUserResponseDto
}
