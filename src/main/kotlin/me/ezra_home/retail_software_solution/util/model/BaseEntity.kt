package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID? = null
) {

    @PrePersist
    fun prePersist() {
        if (id == null) id = UUID.randomUUID()
    }
}
