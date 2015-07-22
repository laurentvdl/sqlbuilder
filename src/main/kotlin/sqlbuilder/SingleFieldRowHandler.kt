package sqlbuilder

/**
 * Useful for mapping a single column (like count(*)).
 * @param <T> type of single result
 *
 * @author Laurent Van der Linden
 */
public class SingleFieldRowHandler<T>(private val requiredType: Class<T>) : CachedRowHandler<T> {
    override var result: T = null

    override fun handle(set: ResultSet, row: Int): Boolean {
        result = set.getObject(requiredType, 1)
        return false
    }
}