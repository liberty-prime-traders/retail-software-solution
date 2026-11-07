package me.ezra_home.retail_software_solution.locations.business.stock_revision

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StockRevisionRepository : JpaRepository<StockRevisionEntity, UUID>
