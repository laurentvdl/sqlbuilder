package sqlbuilder

/**
 * RowHandler that returns a T (likely collection of sorts) after a select operation.
 *
 * @author Laurent Van der Linden
 */
trait ReturningRowHandler<R> : RowHandler {
    val result: R
}