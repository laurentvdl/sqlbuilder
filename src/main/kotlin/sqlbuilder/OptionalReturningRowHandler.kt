package sqlbuilder

/**
 * RowHandler that returns a R or null after a select operation.
 *
 * @author Laurent Van der Linden
 */
interface OptionalReturningRowHandler<R : Any?> : RowHandler {
    val result: R?
}