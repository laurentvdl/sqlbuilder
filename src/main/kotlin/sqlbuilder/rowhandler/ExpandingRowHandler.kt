package sqlbuilder.rowhandler

/**
 * RowHandler that can manipulate the query before execution
 *
 * @author Laurent Van der Linden
 */
interface ExpandingRowHandler {
    fun expand(sql: String?): String?
}