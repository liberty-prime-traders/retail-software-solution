package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.organizations.business.tag.TagCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductTagValidator(
    private val tagCache: TagCache
) {

    fun validateTagsExist(tagIds: Set<UUID>) {
        if (tagIds.isEmpty()) return

        val existingTagIds = tagCache.getAllTags().map { it.id }.toSet()
        val missingTagIds = tagIds - existingTagIds
        if (missingTagIds.isNotEmpty()) {
            throw RtsGenericException(
                "The following tag IDs do not exist: ${missingTagIds.joinToString(", ")}"
            )
        }
    }
}
