package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.ADDRESS)
class AddressEntity(

    @Size(max = 100)
    @Column(name = "line1", length = 100)
    var line1: String? = null,

    @Size(max = 50)
    @Column(name = "line2", length = 50)
    var line2: String? = null,

    @Size(max = 30)
    @Column(name = "line3", length = 30)
    var line3: String? = null,

    @Size(max = 100)
    @Column(name = "state", length = 100)
    var state: String? = null,

    @Size(max = 10)
    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,

    @Size(max = 100)
    @Column(name = "country", length = 100)
    var country: String? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): HasCreatorEntity()
