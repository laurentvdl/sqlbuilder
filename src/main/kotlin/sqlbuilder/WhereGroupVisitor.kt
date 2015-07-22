package sqlbuilder

/**
 * @author Laurent Van der Linden
 */
public interface WhereGroupVisitor<T> {
    fun forItem(subGroup: WhereGroup, item: T)
}