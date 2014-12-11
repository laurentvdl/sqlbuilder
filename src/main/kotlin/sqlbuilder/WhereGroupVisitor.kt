package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
public trait WhereGroupVisitor<T> {
    fun forItem(subGroup: WhereGroup, item: T)
}