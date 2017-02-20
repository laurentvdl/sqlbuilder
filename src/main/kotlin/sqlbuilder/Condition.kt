package sqlbuilder

/**
 * Where condition.
 */
class Condition(val where: String, val arguments: Array<out Any?>?, val relation: Relation) : WherePart
