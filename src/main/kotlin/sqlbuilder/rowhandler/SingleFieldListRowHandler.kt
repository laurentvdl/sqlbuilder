package sqlbuilder.rowhandler

import sqlbuilder.ResultSet
import sqlbuilder.ReturningRowHandler
import java.util.ArrayList

/**
 * Useful for mapping a single column as a List.
 * @param <T> type of single result
 *
 * @author Laurent Van der Linden
 */
public class SingleFieldListRowHandler<T>(private val requiredType: Class<T>) : ReturningRowHandler<List<T>> {
    private var list = ArrayList<T>()

    override var result: List<T> = list

    override fun handle(set: ResultSet, row: Int): Boolean {
        val value = set.getObject(requiredType, 1)
        if (value != null) {
            list.add(value)
        }
        return true
    }
}