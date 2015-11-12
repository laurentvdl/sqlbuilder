package sqlbuilder.impl

/**
 * @author Laurent Van der Linden.
 */
data class PreparedSql(val sql: String, val whereParameters: List<Any>)