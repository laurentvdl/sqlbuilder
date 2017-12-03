package sqlbuilder

/**
 * Thrown if select/update count is wrong.
 *
 * @author Laurent Van der Linden
 */
open class IncorrectMetadataException(message: String) : PersistenceException(message)
