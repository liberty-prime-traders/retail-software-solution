package me.ezra_home.retail_software_solution.util.model

enum class TableName(val tableName: String) {
    ADDRESS(TableNames.ADDRESS),
    SYS_USER(TableNames.SYS_USER),
    LOCATION(TableNames.LOCATION),
    CATEGORY(TableNames.CATEGORY),
    JOB_TITLE(TableNames.JOB_TITLE),
    UNIT_GROUP(TableNames.UNIT_GROUP),
    UNIT_VALUE(TableNames.UNIT_VALUE),
    ORGANIZATION(TableNames.ORGANIZATION),
    ORGANIZATION_ADMIN(TableNames.ORGANIZATION_ADMIN),
    RESERVED_SUBDOMAIN(TableNames.RESERVED_SUBDOMAIN),
    PAYMENT_METHOD(TableNames.PAYMENT_METHOD),
    VARIATION(TableNames.VARIATION),
    ORGANIZATION_JOIN_REQUEST(TableNames.ORGANIZATION_JOIN_REQUEST),
    ORGANIZATION_USER(TableNames.ORGANIZATION_USER),
    PRODUCT(TableNames.PRODUCT),
    DB_VERSION(TableNames.DB_VERSION),
    DB_MIGRATION(TableNames.DB_MIGRATION),
    TABLE_REGISTRY(TableNames.TABLE_REGISTRY),
    ORG_TABLE_REGISTRY(TableNames.ORG_TABLE_REGISTRY);

    companion object {
        private val tableNameSet: Set<String> = entries.map { it.tableName }.toSet()
        fun exists(name: String?): Boolean = name in tableNameSet
    }

}
