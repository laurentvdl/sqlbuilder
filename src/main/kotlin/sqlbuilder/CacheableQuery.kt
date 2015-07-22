package sqlbuilder

/**
 * Reusable query result.
 */
data class CacheableQuery(val query: String, private val parameters: List<Any>?, private val offset: Int?, private val rows: Int?)
