package sqlbuilder

import java.util.ArrayList

/**
 * Where criterium builder.
 */
public class WhereGroup(protected var parent: Any, private val select: Select, private val relation: Relation) : WherePart {
    private val children = ArrayList<WherePart>()

    /**
     * Default AND group.
     * @return this
     */
    public fun group(): WhereGroup {
        return group(Relation.AND)
    }

    /**
     * Start a group, specifying it's relation to any predecessor.
     * @param relation
     * @return this
     */
    public fun group(relation: Relation): WhereGroup {
        val nestedGroup = WhereGroup(this, select, relation)
        children.add(nestedGroup)
        return nestedGroup
    }

    /**
     * End the current group and go up in the hierarchy.
     * @return this
     */
    public fun endGroup(): WhereGroup {
        return parent as WhereGroup
    }

    /**
     * Add a fixed where condition that <b>AND</b>'s itself to its predecessor.
     * @param condition
     * @return this
     */
    public fun and(condition: String): WhereGroup {
        return andInterrnal(true, condition, null)
    }

    /**
     * Add an AND condition if the test succeeds.
     * @param test
     * @param condition
     * @return this
     */
    public fun and(test: Boolean, condition: String): WhereGroup {
        return andInterrnal(test, condition, null)
    }

    /**
     * Add an parameterized where condition that <b>AND</b>'s itself to its predecessor.
     * @param condition
     * @param parameters
     * @return this
     */
    public fun and(condition: String, vararg parameters: Any): WhereGroup {
        return andInterrnal(true, condition, parameters)
    }

    /**
     * Add an parameterized where condition that <b>AND</b>'s itself to its predecessor if the test succeeds.
     * @param test
     * @param condition
     * @param parameters
     * @return this
     */
    public fun and(test: Boolean, condition: String, vararg parameters: Any): WhereGroup {
        return andInterrnal(test, condition, parameters)
    }

    /**
     * varargs are not suitable for method overriding, use Array internally
     */
    private fun andInterrnal(test: Boolean, condition: String, parameters: Array<Any>?): WhereGroup {
        if (test) children.add(Condition(condition, parameters, Relation.AND))
        return this
    }

    /**
     * Add a fixed where condition that <b>OR</b>'s itself to its predecessor.
     * @param condition
     * @return
     */
    public fun or(condition: String): WhereGroup {
        return orInternal(true, condition, null)
    }

    /**
     * Add a fixed where condition that <b>OR</b>'s itself to its predecessor if the test succeeds.
     * @param test
     * @param condition
     * @return
     */
    public fun or(test: Boolean, condition: String): WhereGroup {
        return orInternal(test, condition, null)
    }

    /**
     * Add a parametrized where condition that <b>OR</b>'s itself to its predecessor.
     * @param condition
     * @param parameters
     * @return
     */
    public fun or(condition: String, vararg parameters: Any): WhereGroup {
        return orInternal(true, condition, parameters)
    }

    /**
     * Add a parametrized where condition that <b>OR</b>'s itself to its predecessor if the test succeeds.
     * @param test
     * @param condition
     * @param parameters
     * @return
     */
    public fun or(test: Boolean, condition: String, vararg parameters: Any): WhereGroup {
        return orInternal(test, condition, parameters)
    }

    public fun <T> and(iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup = forIterable(Relation.AND, iterable, visitor)

    public fun <T> or(iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup = forIterable(Relation.OR, iterable, visitor)

    fun <T> forIterable(relation: Relation, iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup {
        if (iterable != null) {
            val subGroup = group(relation)
            for (item in iterable) {
                visitor.forItem(subGroup, item)
            }
        }
        return this
    }

    /**
     * varargs are not suitable for method overriding, use Array internally
     */
    private fun orInternal(test: Boolean, condition: String, parameters: Array<Any>?): WhereGroup {
        if (test) children.add(Condition(condition, parameters, Relation.OR))
        return this
    }

    fun getNestedConditions(): List<WherePart> {
        return children
    }

    /**
     * Close the where builder.
     * @return
     */
    public fun endWhere(): Select {
        return select
    }

    public fun toSql(sql: StringBuilder, parameters: MutableList<Any>) {
        var clean = true
        val childrenSize = children.size()
        for (i in 0..childrenSize - 1) {
            val child = children.get(i)
            if (child is Condition) {
                if (!clean) {
                    appendRelation(sql, child.relation)
                }
                clean = false
                sql.append(child.where)
                if (child.arguments != null) {
                    parameters.addAll(child.arguments)
                }
            } else
                if (child is WhereGroup) {
                    if (!child.getNestedConditions().isEmpty()) {
                        if (!clean) {
                            appendRelation(sql, (child as WhereGroup).relation)
                        }
                        clean = false
                        sql.append("(")
                        (child as WhereGroup).toSql(sql, parameters)
                        sql.append(")")
                    }
                }
        }
    }

    private fun appendRelation(sql: StringBuilder, relation: Relation) {
        when (relation) {
            Relation.AND -> {
                sql.append(" and ")
            }
            Relation.OR -> {
                sql.append(" or ")
            }
        }
    }
}
