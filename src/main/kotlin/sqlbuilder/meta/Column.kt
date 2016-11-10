package sqlbuilder.meta

/**
 * Map a field to a column with a different name.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column (
        /**
         * The name of the column. Defaults to the property or field name.
         */
        val name: String = ""
)