package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.organizations.business.tag.api.TagService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductTagValidator(
    private val tagService: TagService
) {

    fun validateTagsExist(tagIds: Set<UUID>) {
        if (tagIds.isEmpty()) return

        val existingTagIds = tagService.getAllTagDtos().map { it.id }.toSet()
        val missingTagIds = tagIds - existingTagIds
        if (missingTagIds.isNotEmpty()) {
            throw RtsGenericException(
                "The following tag IDs do not exist: ${missingTagIds.joinToString(", ")}"
            )
        }
    }

    fun validateNoOverlap(tagsToAdd: Set<UUID>, tagsToRemove: Set<UUID>) {
        val overlap = tagsToAdd.intersect(tagsToRemove)
        if (overlap.isNotEmpty()) {
            throw RtsGenericException(
                "The following tag IDs are present in both tagsToAdd and tagsToRemove: ${overlap.joinToString(", ")}"
            )
        }
    }
}
