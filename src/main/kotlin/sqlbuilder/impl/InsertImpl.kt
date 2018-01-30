package sqlbuilder.impl

import org.slf4j.LoggerFactory
import sqlbuilder.Backend
import sqlbuilder.Insert
import sqlbuilder.PersistenceException
import sqlbuilder.SqlConverter
import sqlbuilder.exclude
import sqlbuilder.include
import java.sql.PreparedStatement
import java.sql.SQLException

class InsertImpl(private val backend: Backend): Insert {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val metaResolver = backend.metaResolver

    private var entity: String? = null
    private var cachedStatement: PreparedStatement? = null
    private var batch = false
    private var getkeys = true
    private var checkNullability = false
    private var includeFields: Set<String>? = null
    private var excludeFields: Set<String>? = null

    override fun into(entity: String): Insert {
        this.entity = entity
        return this
    }

    override fun batch(): Insert {
        this.batch = true
        return this
    }

    override fun getKeys(cond: Boolean): Insert {
        this.getkeys = cond
        return this
    }

    override fun checkNullability(check: Boolean): Insert {
        checkNullability = check
        return this
    }

    override fun endBatch(): Insert {
        this.batch = false
        if (cachedStatement != null) {
            try {
                cachedStatement!!.close()
            } catch (ignore: SQLException) {}

            this.cachedStatement = null
        }
        return this
    }

    override fun insertBean(bean: Any): Long {
        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(bean.javaClass)
            )
        }

        var sqlString: String? = null
        val sqlCon = backend.getSqlConnection()

        val allProperties = metaResolver.getProperties(bean.javaClass, false)
                .exclude(excludeFields)
                .include(includeFields)

        val keys = metaResolver.getKeys(bean.javaClass)

        val values = allProperties.map { getter ->
            Pair(getter, getter.get(bean))
        }.filter({ it.second != null }).toMap()

        val properties = allProperties.filter {
            values.containsKey(it)
        }

        try {
            if (!batch || cachedStatement == null || !backend.isInTransaction()) {
                val sql = StringBuilder("insert into ").append(entity).append("(")
                properties.joinTo(sql, ",") { it.columnName }
                sql.append(") values(")
                for (i in 0 until properties.size) {
                    if (i > 0) sql.append(",")
                    sql.append("?")
                }
                sql.append(")")

                sqlString = sql.toString()

                logger.info(sqlString)

                cachedStatement = sqlCon.prepareStatement(sqlString, keys.map { it.columnName }.toTypedArray())
            }
            try {
                if (checkNullability) {
                    backend.checkNullability(entity!!, bean, sqlCon, properties)
                }

                val sqlConverter = SqlConverter(backend.configuration)


                for ((index,property) in properties.withIndex()) {
                    logger.debug("setInsertParameter " + property.columnName + " <" + property.classType.name + ">")
                    sqlConverter.setParameter(cachedStatement!!, values[property], index + 1, property.classType, null)
                }

                cachedStatement!!.executeUpdate()

                if (getkeys) {
                    try {
                        var key: Long = 0
                        val generatedKeys = cachedStatement!!.generatedKeys
                        if (generatedKeys != null) {
                            if (generatedKeys.next()) {
                                key = generatedKeys.getLong(1)

                                setBeanGeneratedKey(bean, key)
                            }
                            generatedKeys.close()
                        }
                        return key
                    } catch (ignore: AbstractMethodError) {
                    } catch (ignore: SQLException) {}
                }
            } finally {
                if (!batch) {
                    cachedStatement!!.close()
                    cachedStatement = null
                }
            }
            return 0
        } catch (sqlx: SQLException) {
            throw PersistenceException("insert <$sqlString> failed with parameters ${values.values}", sqlx)
        } finally {
            backend.closeConnection(sqlCon)
        }
    }

    private fun setBeanGeneratedKey(bean: Any, key: Long) {
        val keys = metaResolver.getKeys(bean.javaClass)
        if (keys.size == 1) {
            val soleKey = keys.first()
            when {
                soleKey.classType == Long::class.java || soleKey.classType == java.lang.Long::class.java -> soleKey.set(bean, key)
                soleKey.classType == Int::class.java || soleKey.classType == Integer::class.java -> soleKey.set(bean, key.toInt())
                soleKey.classType == Short::class.java || soleKey.classType == java.lang.Short::class.java -> soleKey.set(bean, key.toShort())
            }
        }
    }

    override fun excludeFields(vararg excludes: String): Insert {
        this.excludeFields = excludes.toSet()
        return this
    }

    override fun includeFields(vararg includes: String): Insert {
        this.includeFields = includes.toSet()
        return this
    }
}