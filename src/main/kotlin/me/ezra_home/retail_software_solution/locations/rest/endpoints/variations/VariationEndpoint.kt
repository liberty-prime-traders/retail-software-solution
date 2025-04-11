package me.ezra_home.retail_software_solution.locations.rest.endpoints.variations

import me.ezra_home.retail_software_solution.locations.business.variation.VariationService
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationInsertDto
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationResponseDto
import me.ezra_home.retail_software_solution.locations.business.variation.dto.VariationUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/variations")
class VariationEndpoint(private val variationService: VariationService) {
    
    @GetMapping
    fun getAll(): Collection<VariationResponseDto> = variationService.getAllVariations()
    
    @PostMapping
    fun createVariation(@RequestBody variationInsertDto: VariationInsertDto): VariationResponseDto {
        return variationService.createVariation(variationInsertDto)
    }

    @PutMapping
    fun updateVariation(@RequestBody variationUpdateDto: VariationUpdateDto): VariationResponseDto {
        return variationService.updateVariation(variationUpdateDto)
    }

    @DeleteMapping("{id}")
    fun deleteVariation(@PathVariable id: UUID?): ResponseEntity<HttpStatus> {
        variationService.deleteVariation(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
