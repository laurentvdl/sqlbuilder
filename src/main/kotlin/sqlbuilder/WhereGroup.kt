package sqlbuilder

import java.util.ArrayList

/**
 * Where clause builder.
 */
class WhereGroup(private var parent: Any, private val select: Select, private val relation: Relation) : WherePart {
    private val children = ArrayList<WherePart>()

    /**
     * Default AND group.
     * @return this
     */
    fun group(): WhereGroup {
        return group(Relation.AND)
    }

    /**
     * Start a group, specifying it's relation to any predecessor.
     * @param relation
     * @return this
     */
    fun group(relation: Relation): WhereGroup {
        val nestedGroup = WhereGroup(this, select, relation)
        children.add(nestedGroup)
        return nestedGroup
    }

    /**
     * End the current group and go up in the hierarchy.
     * @return this
     */
    fun endGroup(): WhereGroup {
        return parent as WhereGroup
    }

    /**
     * Add a fixed where condition that <b>AND</b>'s itself to its predecessor.
     * @param condition where condition
     * @return this
     */
    fun and(condition: String): WhereGroup {
        return andInternal(true, condition, null)
    }

    /**
     * Add an AND condition if the test succeeds.
     * @param test only apply this operation if true, useful for builder invocation
     * @param condition where condition
     * @return this
     */
    fun and(test: Boolean, condition: String?): WhereGroup {
        return andInternal(test, condition, null)
    }

    /**
     * Add an parameterized where condition that <b>AND</b>'s itself to its predecessor.
     * @param condition where condition
     * @param parameters values for any where parameters (?) specified in the condition
     * @return this
     */
    fun and(condition: String, vararg parameters: Any?): WhereGroup {
        return andInternal(true, condition, parameters)
    }

    /**
     * Add an parameterized where condition that <b>AND</b>'s itself to its predecessor if the test succeeds.
     * @param test only apply this operation if true, useful for builder invocation
     * @param condition where condition
     * @param parameters values for any where parameters (?) specified in the condition
     * @return this
     */
    fun and(test: Boolean, condition: String?, vararg parameters: Any?): WhereGroup {
        return andInternal(test, condition, parameters)
    }

    /**
     * varargs are not suitable for method overriding, use Array internally
     */
    private fun andInternal(test: Boolean, condition: String?, parameters: Array<out Any?>?): WhereGroup {
        if (test) {
            if (condition != null) {
                children.add(Condition(condition, parameters, Relation.AND))
            } else {
                throw IllegalArgumentException("Condition should not be null when test evaluates to true")
            }
        }
        return this
    }

    /**
     * Add a fixed where condition that <b>OR</b>'s itself to its predecessor.
     * @param condition where condition
     * @return this
     */
    fun or(condition: String): WhereGroup {
        return orInternal(true, condition, null)
    }

    /**
     * Add a fixed where condition that <b>OR</b>'s itself to its predecessor if the test succeeds.
     * @param test only apply this operation if true, useful for builder invocation
     * @param condition where condition
     * @return this
     */
    fun or(test: Boolean, condition: String?): WhereGroup {
        return orInternal(test, condition, null)
    }

    /**
     * Add a parametrized where condition that <b>OR</b>'s itself to its predecessor.
     * @param condition where condition
     * @param parameters values for any where parameters (?) specified in the condition
     * @return this
     */
    fun or(condition: String, vararg parameters: Any?): WhereGroup {
        return orInternal(true, condition, parameters)
    }

    /**
     * Add a parametrized where condition that <b>OR</b>'s itself to its predecessor if the test succeeds.
     * @param test only apply this operation if true, useful for builder invocation
     * @param condition where condition
     * @param parameters values for any where parameters (?) specified in the condition
     * @return this
     */
    fun or(test: Boolean, condition: String?, vararg parameters: Any?): WhereGroup {
        return orInternal(test, condition, parameters)
    }

    /**
     * create a nested AND group and specify the clauses for each iterated item via a visitor function
     */
    fun <T> and(iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup = forIterable(Relation.AND, iterable, visitor)

    /**
     * create a nested OR group and specify the clauses for each iterated item via a visitor function
     */
    fun <T> or(iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup = forIterable(Relation.OR, iterable, visitor)

    fun <T> forIterable(relation: Relation, iterable: Iterable<T>?, visitor: WhereGroupVisitor<T>): WhereGroup {
        if (iterable != null && iterable.any()) {
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
    private fun orInternal(test: Boolean, condition: String?, parameters: Array<out Any?>?): WhereGroup {
        if (test) {
            if (condition != null) {
                children.add(Condition(condition, parameters, Relation.OR))
            } else {
                throw IllegalArgumentException("Condition should not be null when test evaluates to true")
            }
        }
        return this
    }

    fun getNestedConditions(): List<WherePart> {
        return children
    }

    /**
     * Close the where builder.
     * @return the select statement
     */
    fun endWhere(): Select {
        return select
    }

    fun toSql(sql: StringBuilder, parameters: MutableList<Any?>) {
        var clean = true
        for (child in children) {
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
                            appendRelation(sql, child.relation)
                        }
                        clean = false
                        sql.append("(")
                        child.toSql(sql, parameters)
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

    val isNotEmpty: Boolean
        get() = getNestedConditions().any {
            it is Condition || (it is WhereGroup && it.isNotEmpty)
        }
}
