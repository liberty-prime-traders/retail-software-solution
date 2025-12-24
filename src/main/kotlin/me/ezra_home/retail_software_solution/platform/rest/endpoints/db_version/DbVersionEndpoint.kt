package me.ezra_home.retail_software_solution.platform.rest.endpoints.db_version

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/secured/db-versions")
@PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
class DbVersionEndpoint(private val dbVersionService: DbVersionService) {

    @GetMapping
    fun getAllDbVersions(): Collection<DbVersionResponseDto> = dbVersionService.getAllDbVersions()

    @PutMapping("/{versionId}/activate")
    fun activateVersion(@PathVariable versionId: UUID): DbVersionResponseDto =
        dbVersionService.activateDbVersion(versionId)
}
