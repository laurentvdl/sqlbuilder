package sqlbuilder

/**
 * Delete statement. Only used with beans: every non-null property is added to where clause.
 * <br/>Use Update for custom deletes.
 *
 * @author Laurent Van der Linden
 */
interface Delete {
    fun from(entity: String): Delete

    fun deleteBean(bean: Any): Int
}
