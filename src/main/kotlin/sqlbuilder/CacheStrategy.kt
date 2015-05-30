package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
public interface CacheStrategy {
    public fun get(query: CacheableQuery): Any?
    public fun put(query: CacheableQuery, result: Any)
}
