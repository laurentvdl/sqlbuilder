package sqlbuilder

public interface CachedRowHandler<R> : ReturningRowHandler<R> {
    override var result: R
}
