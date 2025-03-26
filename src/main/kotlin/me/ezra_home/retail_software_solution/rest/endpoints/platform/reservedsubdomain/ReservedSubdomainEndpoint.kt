package me.ezra_home.retail_software_solution.rest.endpoints.platform.reservedsubdomain

import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainDto
import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @GetMapping
    fun getAllReservedSubdomains(): Collection<ReservedSubdomainDto> = reservedSubdomainService.getReservedSubdomains()

    @GetMapping("{suggestedSubdomain}")
    fun sanitizeSuggestedSubdomain(@PathVariable suggestedSubdomain: String): Result<String> =
        reservedSubdomainService.sanitizeSubdomain(suggestedSubdomain)

    @PostMapping("{suggestedSubdomain}")
    fun reserveSubdomain(@PathVariable suggestedSubdomain: String): Result<UUID?> =
        reservedSubdomainService.reserveSubdomain(suggestedSubdomain)

    @DeleteMapping("{id}")
    fun releaseSubdomain(@PathVariable id: UUID): ResponseEntity<HttpStatus> {
        reservedSubdomainService.releaseSubdomain(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
