package me.ezra_home.retail_software_solution.business.userlocation

import me.ezra_home.retail_software_solution.model.entity.UserLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserLocationRepository : JpaRepository<UserLocationEntity, UUID> {

    @Modifying
    @Query("update UserLocationEntity u set u.endOn = CURRENT_TIMESTAMP where u.locationId = :locationId and u.userId in :userIds")
    fun terminateLocationAssignments(@Param("locationId") locationId: UUID, @Param("userIds") userIds: Set<UUID>)

    fun findByLocationId(locationId: UUID): Collection<UserLocationEntity>
}
