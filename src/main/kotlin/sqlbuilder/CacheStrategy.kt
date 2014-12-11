package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
public trait CacheStrategy {
    public fun get(query: CacheableQuery): Any?
    public fun put(query: CacheableQuery, result: Any)
}
