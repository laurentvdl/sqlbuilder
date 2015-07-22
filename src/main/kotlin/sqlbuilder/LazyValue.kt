package sqlbuilder

/**
 * Use LazyValues for conditional where variables to avoid eager evaluation.
 *
 * @author Laurent Van der Linden
 */
public interface LazyValue {
    throws(Exception::class )
    public fun eval(): Any
}
