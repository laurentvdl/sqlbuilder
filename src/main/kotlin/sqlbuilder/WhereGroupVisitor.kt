package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
interface WhereGroupVisitor<in T> {
    fun forItem(subGroup: WhereGroup, item: T)
}