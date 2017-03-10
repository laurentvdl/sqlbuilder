package sqlbuilder.impl

import org.slf4j.LoggerFactory
import sqlbuilder.Backend
import sqlbuilder.IncorrectMetadataException
import sqlbuilder.IncorrectResultSizeException
import sqlbuilder.PersistenceException
import sqlbuilder.SqlConverter
import sqlbuilder.Update
import sqlbuilder.exclude
import sqlbuilder.include
import sqlbuilder.meta.PropertyReference
import sqlbuilder.usea
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.Arrays

/**
 * Update statement: pass in a bean or run a custom statement
 *
 * @author Laurent Van der Linden
 */
class UpdateImpl(private val backend: Backend): Update {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val metaResolver = backend.metaResolver

    private var entity: String? = null
    private var checkNullability = false
    private var getkeys = false
    private var includeFields: Array<out String>? = null
    private var excludeFields: Array<out String>? = null

    private var _generatedKey: Long = 0

    override fun entity(entity: String): Update {
        this.entity = entity
        return this
    }

    override fun checkNullability(check: Boolean): Update {
        checkNullability = check
        return this
    }

    override fun updateBean(bean: Any) {
        val keys = metaResolver.getKeys(bean.javaClass)
        updateBean(bean, keys.map(PropertyReference::columnName).toTypedArray())
    }

    override fun updateBean(bean: Any, keys: Array<out String>) {
        if (entity == null) entity = metaResolver.getTableName(bean.javaClass)
        entity = backend.configuration.escapeEntity(entity)

        if (keys.isEmpty()) {
            throw IncorrectMetadataException("cannot update bean without a list of keys")
        }

        val sqlCon = backend.getSqlConnection()
        try {
            val getters = metaResolver.getProperties(bean.javaClass, false)
                    .exclude(excludeFields)
                    .include(includeFields)

            val sql = StringBuilder("update ").append(entity).append(" set ")


            val valueProperties = getters.filter { !keys.contains(it.columnName) }
            val keyProperties = getters.filter { keys.contains(it.columnName) }

            valueProperties.map { "${it.columnName} = ?" }.joinTo(sql, ",")

            sql.append(" where ")

            keyProperties.map { "${it.columnName} = ?" }.joinTo(sql, " and ")

            val sqlString = sql.toString()
            logger.info(sqlString)

            val sqlConverter = SqlConverter(backend.configuration)

            try {
                if (checkNullability) {
                    backend.checkNullability(entity!!, bean, sqlCon, getters)
                }
                var updates = 0
                sqlCon.prepareStatement(sqlString)!!.usea { ps: PreparedStatement ->
                    for ((index, getter) in valueProperties.withIndex()) {
                        try {
                            sqlConverter.setParameter(ps, getter.get(bean), index + 1, getter.classType, null)
                        } catch (e: IllegalArgumentException) {
                            throw PersistenceException("unable to get " + getter.name, e)
                        }
                    }

                    for ((index, getter) in keyProperties.withIndex()) {
                        sqlConverter.setParameter(ps, getter.get(bean), valueProperties.size + index + 1, getter.classType, null)
                    }

                    updates = ps.executeUpdate()
                }

                if (updates != 1) {
                    throw PersistenceException("updateBean resulted in $updates updated rows instead of 1 using <$sql> with bean $bean")
                }
            } catch (sqlx: SQLException) {
                throw PersistenceException("update <$sql> failed", sqlx)
            }

        } finally {
            backend.closeConnection(sqlCon)
        }
    }

    override fun updateStatement(sql: String): Int {
        return updateStatement(sql, null, null)
    }

    override fun updateStatement(sql: String, vararg parameters: Any): Int {
        return updateStatement(sql, parameters, null)
    }

    override fun updateStatement(sql: String, parameters: Array<out Any>?, types: IntArray?): Int {
        logger.info(sql)

        val sqlConverter = SqlConverter(backend.configuration)

        val connection = backend.getSqlConnection()
        try {
            val autoGeneratedKeys = if (getkeys) Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS
            return connection.prepareStatement(sql, autoGeneratedKeys)!!.usea { ps ->
                if (parameters != null) {
                    for ((index, parameter) in parameters.withIndex()) {
                        val parameterType = if (types == null) null else types[index]
                        sqlConverter.setParameter(ps, parameter, index + 1, null, parameterType)
                    }
                }

                val rows = ps.executeUpdate()

                _generatedKey = 0
                if (getkeys) {
                    try {
                        val keys = ps.generatedKeys
                        if (keys != null) {
                            if (keys.next()) {
                                _generatedKey = keys.getLong(1)
                            }
                            keys.close()
                        }
                    } catch (ignore: AbstractMethodError) {
                    } catch (sqlx: SQLException) {
                        throw PersistenceException("unable to retrieve generated keys", sqlx)
                    }

                }
                rows
            }
        } catch (e: Exception) {
            throw PersistenceException("update <$sql> failed with parameters ${Arrays.toString(parameters)}", e)
        } finally {
            backend.closeConnection(connection)
        }
    }

    override fun updateStatementExpecting(sql: String, expectedUpdateCount: Int, vararg parameters: Any): Int {
        val updates = updateStatement(sql, parameters, null)
        if (updates != expectedUpdateCount) {
            val errorBuilder = StringBuilder("expected $expectedUpdateCount rows to be updated, but $updates rows were ($sql [")
            parameters.joinTo(errorBuilder, ",")
            errorBuilder.append("])")
            throw IncorrectResultSizeException(errorBuilder.toString())
        }
        return updates
    }

    override fun getKeys(cond: Boolean): Update {
        this.getkeys = cond
        return this
    }

    override fun excludeFields(vararg excludes: String): Update {
        this.excludeFields = excludes
        return this
    }

    override fun includeFields(vararg includes: String): Update {
        this.includeFields = includes
        return this
    }

    override fun getGeneratedKey(): Long  = _generatedKey
}