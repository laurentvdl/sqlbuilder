package sqlbuilder

/**
 * Insert statement: create row from bean
 *
 * @author Laurent Van der Linden
 */
interface Insert {
    fun into(entity: String): Insert

    fun batch(): Insert

    fun getKeys(cond: Boolean): Insert

    fun checkNullability(check: Boolean): Insert

    fun endBatch(): Insert

    /**
     * Insert the bean as a new row & return the first generated key if supported/autogenerated.
     * <br/>Otherwise '0'.
     * <br/>NULL values are not included in query.
     *
     * @param bean Value Bean
     *
     * @return generared key
     */
    fun insertBean(bean: Any): Long

    fun excludeFields(vararg excludes: String): Insert

    fun includeFields(vararg includes: String): Insert
}