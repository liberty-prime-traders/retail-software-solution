package me.ezra_home.retail_software_solution.locations.business.revision

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.BaseRevisionEntity
import org.hibernate.envers.RevisionEntity


@Entity
@RevisionEntity
@Table(name = "revinfo")
class LocationRevisionEntity: BaseRevisionEntity()
