package sqlbuilder

/**
 * Use LazyValues for conditional where variables to avoid eager evaluation.
 */
@Deprecated("Use java 8 Supplier interface", ReplaceWith("Supplier", "java.util.function.Supplier"))
interface LazyValue {
    @Throws(Exception::class )
    fun eval(): Any?
}
