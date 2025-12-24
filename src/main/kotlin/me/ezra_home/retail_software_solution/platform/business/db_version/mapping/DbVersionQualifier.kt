package me.ezra_home.retail_software_solution.platform.business.db_version.mapping

import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DbVersionQualifier(private final val dbVersionCache: DbVersionCache) {

    @DbVersionNumber
    fun resolveVersionNumber(versionId: UUID?): String? {
        return versionId?.let {
            dbVersionCache.getAllDbVersions().find { it.id == versionId }?.versionNumber
        }
    }
}
