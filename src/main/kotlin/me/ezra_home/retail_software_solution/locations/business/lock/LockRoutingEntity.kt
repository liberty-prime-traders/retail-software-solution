package me.ezra_home.retail_software_solution.locations.business.lock

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

// Exists only to give EntityAdvisoryLockRepository an entity to bind against, which routes its
// native advisory-lock query through the location-schema EntityManagerFactory. Maps only `id`
// on the `sale` table (always present) — never read, written, or otherwise used.
@Entity
@Table(name = TableNames.SALE)
class LockRoutingEntity(
    @Id
    var id: UUID
)
