package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
interface CacheStrategy {
    fun get(query: CacheableQuery): Any?
    fun put(query: CacheableQuery, result: Any)
}
