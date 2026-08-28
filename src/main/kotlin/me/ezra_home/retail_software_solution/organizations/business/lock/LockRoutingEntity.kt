package me.ezra_home.retail_software_solution.organizations.business.lock

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

// Exists only to give OrgEntityAdvisoryLockRepository an entity to bind against, which routes
// its native advisory-lock query through the organization-schema EntityManagerFactory. Maps
// only `id` on the `account` table (always present) — never read, written, or otherwise used.
@Entity
@Table(name = TableNames.ACCOUNT)
class LockRoutingEntity(
    @Id
    var id: UUID
)
