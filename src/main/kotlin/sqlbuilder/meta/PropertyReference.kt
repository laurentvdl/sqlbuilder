package sqlbuilder.meta

/**
 * Provide read/write access to bean properties
 *
 * @author Laurent Van der Linden
 */
interface PropertyReference {
    val name: String

    val columnName: String

    fun set(bean: Any, value: Any?)

    fun get(bean: Any): Any?

    val classType: Class<*>
}
