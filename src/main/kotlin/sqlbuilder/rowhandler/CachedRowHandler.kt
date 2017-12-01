package sqlbuilder.rowhandler

import sqlbuilder.ResultSet
import sqlbuilder.ReturningRowHandler

/**
 * @author Laurent Van der Linden.
 */
class CachedRowHandler<out T>(private val cachedResult: T) : ReturningRowHandler<T> {
    override val result: T
        get() = cachedResult

    override fun handle(set: ResultSet, row: Int): Boolean {
        throw UnsupportedOperationException()
    }
}