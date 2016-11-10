package sqlbuilder.meta

/**
 * Defines the table name for a type.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table (
        /**
         * The table name for this type. Defaults to the class name.
         */
        val name: String = ""
)