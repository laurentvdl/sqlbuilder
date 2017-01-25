package sqlbuilder

/**
 * Sole RuntimeException used for any handler (database) error.
 *
 * @author Laurent Van der Linden
 */
open class PersistenceException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)
