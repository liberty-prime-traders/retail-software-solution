package me.ezra_home.retail_software_solution.util.enums

import java.util.UUID

enum class ServiceAccount(val uniqueId: UUID) {
    LOCATION_TO_ORGANIZATION_SYNC(UUID.fromString("11111111-1111-1111-1111-111111111111"));
}
