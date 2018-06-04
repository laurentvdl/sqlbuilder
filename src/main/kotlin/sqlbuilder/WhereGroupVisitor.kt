package sqlbuilder

interface WhereGroupVisitor<in T> {
    fun forItem(subGroup: WhereGroup, item: T)
}