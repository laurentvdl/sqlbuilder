package sqlbuilder

interface CacheStrategy {
    fun get(query: CacheableQuery): Any?
    fun put(query: CacheableQuery, result: Any?)
}
