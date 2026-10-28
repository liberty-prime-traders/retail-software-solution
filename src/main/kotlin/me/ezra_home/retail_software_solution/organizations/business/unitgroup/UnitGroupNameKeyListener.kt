package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import me.ezra_home.retail_software_solution.util.business.StringUtils

// A listener rather than a callback on the entity: BaseEntity and ImmutableEntity already declare @PrePersist and @PreUpdate.
class UnitGroupNameKeyListener {

    @PrePersist
    @PreUpdate
    fun refreshNameKey(unitGroupEntity: UnitGroupEntity) {
        unitGroupEntity.nameKey = StringUtils.alphanumericKey(unitGroupEntity.name)
    }
}
