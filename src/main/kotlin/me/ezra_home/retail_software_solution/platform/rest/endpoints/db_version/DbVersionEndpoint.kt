package me.ezra_home.retail_software_solution.platform.rest.endpoints.db_version

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionCreationDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("/secured/db-versions")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class DbVersionEndpoint(private val dbVersionService: DbVersionService) {
    @PostMapping
    fun createVersion(@RequestBody request: DbVersionCreationDto): DbVersionResponseDto =
        dbVersionService.createDbVersion(request)

    @GetMapping
    fun getAllDbVersions(): Collection<DbVersionResponseDto> {
        return dbVersionService.getAllDbVersions()
    }

    @PostMapping("/{versionId}/activate")
    fun activateVersion(@PathVariable versionId: UUID): DbVersionResponseDto =
        dbVersionService.activateDbVersion(versionId)
}