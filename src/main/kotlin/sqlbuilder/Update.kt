package sqlbuilder

/**
 * Update statement: pass in a bean or run a custom statement
 *
 * @author Laurent Van der Linden
 */
interface Update {
    fun entity(entity: String): Update

    fun checkNullability(check: Boolean): Update

    /**
     * Update all properties, including NULL values & keys (used for where)
     * @param bean to update
     */
    fun updateBean(bean: Any)

    /**
     * Update all properties, including NULL values & keys (used for where)
     * @param bean value bean
     * @param keys fields to use for where
     * @throws sqlbuilder.PersistenceException if more or less than 1 row is updated
     */
    fun updateBean(bean: Any, keys: Array<out String>)
    /**
     * Custom update that allows null parameters due to the types argument.
     * @param sql statement
     * @return updated rows
     */
    fun updateStatement(sql: String): Int

    /**
     * Custom update that allows null parameters due to the types argument.
     * @param sql statement
     * @param parameters parameters objects
     * @return updated rows
     */
    fun updateStatement(sql: String, vararg parameters: Any): Int

    /**
     * Custom update that allows null parameters due to the types argument.
     * @param sql statement
     * @param parameters parameters objects
     * @param types array of java.sql.Types
     * @return updated rows
     */
    fun updateStatement(sql: String, parameters: Array<out Any>?, types: IntArray?): Int

    /**
     * Special updatestatement that throws PersistenceException if updated rows do not match.
     * @param sql
     * @param expectedUpdateCount
     * @param parameters
     * @return updated rows if matching the expected
     */
    fun updateStatementExpecting(sql: String, expectedUpdateCount: Int, vararg parameters: Any): Int

    /**
     * store any generated id after executing the update statement (which should be an insert in this case)
     * <br/>use <code>getGeneratedKey</code> to get the value afterwards
     * @param cond
     * @return
     */
    fun getKeys(cond: Boolean): Update

    fun excludeFields(vararg excludes: String): Update

    fun includeFields(vararg includes: String): Update
}