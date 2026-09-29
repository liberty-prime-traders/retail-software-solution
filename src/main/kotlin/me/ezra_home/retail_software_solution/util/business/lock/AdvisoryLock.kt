package me.ezra_home.retail_software_solution.util.business.lock

// Acquires a Postgres advisory lock (pg_advisory_xact_lock) for the whole current transaction —
// released automatically on commit or rollback. Namespaced so keys from different natural-key
// types never collide, and acquired in sorted order so two threads locking the same set of keys
// in a different order can't deadlock each other.
//
// `Repository` is implemented once per schema (organization, location, …) by a JpaRepository
// that's otherwise unrelated to locking — its entity binding exists only to route the native
// query through that schema's own EntityManagerFactory, since pg_advisory_xact_lock is
// connection-scoped and each schema has its own connection pool. Pass whichever schema's
// implementation matches the transaction you're already inside.
object AdvisoryLock {

    interface Repository {
        fun acquire(key: String)
    }

    fun acquire(repository: Repository, namespace: String, keys: Collection<String>) {
        keys.toSortedSet().forEach { repository.acquire("$namespace:$it") }
    }

    fun acquire(repository: Repository, namespace: String, key: String) {
        acquire(repository, namespace, listOf(key))
    }
}
