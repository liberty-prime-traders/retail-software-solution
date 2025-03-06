import jakarta.persistence.EntityNotFoundException
import me.ezra_home.retail_software_solution.business.variation.VariationService
import me.ezra_home.retail_software_solution.business.variation.dto.VariationCreateDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationDto
import me.ezra_home.retail_software_solution.business.variation.dto.VariationUpdateRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/variations")
class VariationController(
    private val variationService: VariationService
) {
    @PostMapping
    fun create(@RequestBody createDto: VariationCreateDto): ResponseEntity<VariationDto> {
        val createdVariation = variationService.create(createDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVariation)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<VariationDto> {
        return try {
            val variation = variationService.findById(id)
            ResponseEntity.ok(variation)
        } catch (e: EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getAll(): ResponseEntity<List<VariationDto>> {
        val variations = variationService.findAll()
        return ResponseEntity.ok(variations)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody updateRequest: VariationUpdateRequest
    ): ResponseEntity<VariationDto> {
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