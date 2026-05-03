package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleRepository : JpaRepository<SaleEntity, UUID> {

    @Query("SELECT s FROM SaleEntity s")
    fun findTopN(pageable: Pageable): List<SaleEntity>
}
