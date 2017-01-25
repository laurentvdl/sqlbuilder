package sqlbuilder.meta

/**
 * Mark a field as not persistent.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transient