package sqlbuilder

import org.slf4j.LoggerFactory
import java.lang.ref.SoftReference

/**
 * Caching strategy that uses memory softlinks to hold results.
 * The results are separated using the input query and parameters, so a single cacheId can be used for many queries.
 *
 * @author Laurent Van der Linden
 */
class MemoryCache(private val backend: Backend, private val cacheId: String?) : CacheStrategy {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    override fun get(query: CacheableQuery): Any? {
        val cachedResultReference = backend.getCache(cacheId)[query]
        if (cachedResultReference != null) {
            val cachedResult = cachedResultReference.get()
            if (cachedResult != null) {
                logger.debug("cache hit: {}", query.query)
                return cachedResult
            } else {
                logger.debug("softlink broken for {}", query.query)
            }
        }
        return null
    }

    override fun put(query: CacheableQuery, result: Any?) {
        if (result != null) {
            backend.getCache(cacheId).put(query, SoftReference(result))
        }
    }
}
