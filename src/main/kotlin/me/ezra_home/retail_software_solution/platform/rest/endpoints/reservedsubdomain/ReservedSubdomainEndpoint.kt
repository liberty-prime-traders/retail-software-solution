package me.ezra_home.retail_software_solution.platform.rest.endpoints.reservedsubdomain

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainDto
import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/reserved-subdomains")
class ReservedSubdomainEndpoint(private val reservedSubdomainService: ReservedSubdomainService) {

    @PostMapping("{suggestedSubdomain}")
    @PreAuthorize("hasRole('${RtsRoles.ROLE_CREATE_ORGANIZATION}')")
    fun sanitizeThenReserveSubdomain(@PathVariable suggestedSubdomain: String): String =
        reservedSubdomainService.sanitizeThenReserveSubdomain(suggestedSubdomain)

    @GetMapping
    @PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
    fun getAllReservedSubdomains(): Collection<ReservedSubdomainDto> = reservedSubdomainService.getReservedSubdomains()

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('${RtsRoles.ROLE_PLATFORM_ADMIN}')")
    fun releaseSubdomain(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        reservedSubdomainService.releaseSubdomain(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
