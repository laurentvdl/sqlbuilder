package sqlbuilder

/**
 * Use LazyValues for conditional where variables to avoid eager evaluation.
 *
 * @author Laurent Van der Linden
 */
interface LazyValue {
    @Throws(Exception::class )
    fun eval(): Any?
}
