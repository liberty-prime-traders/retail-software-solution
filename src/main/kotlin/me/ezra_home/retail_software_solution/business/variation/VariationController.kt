import jakarta.persistence.EntityNotFoundException
import me.ezra_home.retail_software_solution.business.variation.VariationService
import me.ezra_home.retail_software_solution.business.variation.dto.VariationInsertDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationResponseDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/variations")
class VariationController(
    private val variationService: VariationService
) {
    @PostMapping
    fun create(@RequestBody createDto: VariationInsertDto): ResponseEntity<VariationResponseDto> {
        val createdVariation = variationService.create(createDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVariation)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<VariationResponseDto> {
        return try {
            val variation = variationService.findById(id)
            ResponseEntity.ok(variation)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getAll(): ResponseEntity<List<VariationResponseDto>> {
        val variations = variationService.findAll()
        return ResponseEntity.ok(variations)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody updateRequest: VariationUpdateDto
    ): ResponseEntity<VariationResponseDto> {
        return try {
            val updatedVariation = variationService.update(id, updateRequest)
            ResponseEntity.ok(updatedVariation)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        return try {
            variationService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }
}
