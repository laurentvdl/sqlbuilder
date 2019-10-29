package sqlbuilder

/**
 * Sole RuntimeException used for any handler (database) error.
 *
 * @author Laurent Van der Linden
 */
open class PersistenceException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause) {
    override val message: String?
        get() {
            var completeMessage = super.message ?: ""
            var causeCursor = cause
            while (causeCursor != null) {
                causeCursor.message?.let {
                    completeMessage += " caused by $it"
                }

                causeCursor = causeCursor.cause
            }
            return completeMessage
        }
}
