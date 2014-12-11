package sqlbuilder

/**
 * Where condition.
 */
public class Condition(val where: String, val arguments: Array<Any>?, val relation: Relation) : WherePart
