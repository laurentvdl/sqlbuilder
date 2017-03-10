package sqlbuilder.rowhandler

import sqlbuilder.OptionalReturningRowHandler
import sqlbuilder.ResultSet

/**
 * Useful for mapping a single column (like count(*)).
 * @param <T> type of single result
 *
 * @author Laurent Van der Linden
 */
class SingleFieldRowHandler<T : Any?>(private val requiredType: Class<T>) : OptionalReturningRowHandler<T> {
    override var result: T? = null

    override fun handle(set: ResultSet, row: Int): Boolean {
        result = set.getObject(requiredType, 1)
        return false
    }
}