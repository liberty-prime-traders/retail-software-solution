package me.ezra_home.retail_software_solution.business.variation

import me.ezra_home.retail_software_solution.business.variation.dto.CreateVariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.UpdateVariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationEntityDto
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/variations")
class VariationController(
    private val variationService: VariationService
) {
    @GetMapping
    fun getAllVariations(): ResponseEntity<Unit> {
        return ResponseEntity.ok(variationService.getAllVariations())
    }

    @GetMapping("/{variationId}")
    fun getVariation(@PathVariable variationId: UUID): ResponseEntity<VariationEntityDto> {
        return ResponseEntity.ok(variationService.getVariation(variationId))
    }

    @PostMapping
    fun createVariation(
        @RequestBody dto: CreateVariationDto,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<VariationEntityDto> {
        val userId = UUID.fromString(jwt.subject)
        return ResponseEntity.ok(variationService.createVariation(dto, userId))
    }

    @PutMapping("/{variationId}")
    fun updateVariation(
        @PathVariable variationId: UUID,
        @RequestBody dto: UpdateVariationDto,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<VariationEntityDto> {
        val userId = UUID.fromString(jwt.subject)
        return ResponseEntity.ok(variationService.updateVariation(variationId, dto, userId))
    }

    @DeleteMapping("/{variationId}")
    fun deleteVariation(
        @PathVariable variationId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Unit> {
        val userId = UUID.fromString(jwt.subject)
        variationService.deleteVariation(variationId, userId)
        return ResponseEntity.noContent().build()
    }
}