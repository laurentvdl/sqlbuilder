package sqlbuilder

public trait CachedRowHandler<R> : ReturningRowHandler<R> {
    override var result: R
}
