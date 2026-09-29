package me.ezra_home.retail_software_solution.organizations.business.tag

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TagRepository: JpaRepository<TagEntity, UUID> {

    @Query("""
        SELECT t FROM TagEntity t
        WHERE FUNCTION('similarity', LOWER(t.tagName), LOWER(:tagName)) >= :threshold
        ORDER BY FUNCTION('similarity', LOWER(t.tagName), LOWER(:tagName)) DESC
    """)
    fun findSimilarTags(
        @Param("tagName") tagName: String,
        @Param("threshold") threshold: Double
    ): List<TagEntity>
}
