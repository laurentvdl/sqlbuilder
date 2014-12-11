package sqlbuilder

/**
 * Use LazyValues for conditional where variables to avoid eager evaluation.
 *
 * @author Laurent Van der Linden
 */
public trait LazyValue {
    throws(javaClass<Exception>())
    public fun eval(): Any
}
