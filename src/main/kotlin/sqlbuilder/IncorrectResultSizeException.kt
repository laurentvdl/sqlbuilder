package sqlbuilder

/**
 * Thrown if select/update count is wrong.
 *
 * @author Laurent Van der Linden
 */
class IncorrectResultSizeException(message: String) : PersistenceException(message)
