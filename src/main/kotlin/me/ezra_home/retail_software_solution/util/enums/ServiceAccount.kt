package me.ezra_home.retail_software_solution.util.enums

import java.util.UUID

enum class ServiceAccount(val uniqueId: UUID) {
    LOCATION_TO_ORGANIZATION_SYNC(UUID.fromString("11111111-1111-1111-1111-111111111111")),
    RECORD_INITIALIZER(UUID.fromString("22222222-2222-2222-2222-222222222222"));
}
