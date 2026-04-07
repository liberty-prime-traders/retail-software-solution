package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagInsertDto
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagResponseDto
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagService
import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
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
@RequestMapping("secured/tag")
class TagEndpoint(private val tagService: TagService) {

    @PostMapping
    fun createTag(@RequestBody tagInsertDto: TagInsertDto): TagResponseDto =
        tagService.createTag(tagInsertDto)

    @PutMapping
    fun updateTag(@RequestBody tagDto: TagUpdateDto): TagResponseDto =
        tagService.updateTag(tagDto)

    @GetMapping
    fun getAllTags(): Collection<TagResponseDto> =
        tagService.getAllTags()

    @DeleteMapping("{id}")
    fun deleteTag(@PathVariable id: UUID): ResponseEntity<HttpStatusCode> {
        tagService.deleteTag(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
