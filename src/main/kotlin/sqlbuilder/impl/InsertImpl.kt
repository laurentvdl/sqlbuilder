package sqlbuilder.impl

import sqlbuilder.exclude
import sqlbuilder.include
import sqlbuilder.Backend
import java.sql.PreparedStatement
import sqlbuilder.Insert
import java.sql.SQLException
import org.slf4j.LoggerFactory
import java.sql.Statement
import sqlbuilder.PersistenceException
import sqlbuilder.SqlConverter

class InsertImpl(val backend: Backend): Insert {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var entity: String? = null
    private var cachedStatement: PreparedStatement? = null
    private var batch = false
    private var getkeys = false
    private var checkNullability = false
    private var includeFields: Array<out String>? = null
    private var excludeFields: Array<out String>? = null

    public override fun into(entity: String): Insert {
        this.entity = entity
        return this
    }

    public override fun batch(): Insert {
        this.batch = true
        return this
    }

    public override fun getKeys(cond: Boolean): Insert {
        this.getkeys = cond
        return this
    }

    public override fun checkNullability(check: Boolean): Insert {
        checkNullability = check
        return this
    }

    public override fun endBatch(): Insert {
        this.batch = false
        if (cachedStatement != null) {
            try {
                cachedStatement!!.close()
            } catch (ignore: SQLException) {}

            this.cachedStatement = null
        }
        return this
    }

    public override fun insertBean(bean: Any): Long {
        val metaResolver = backend.configuration.metaResolver

        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(bean.javaClass)
            )
        }

        var sqlString: String? = null
        val sqlCon = backend.getSqlConnection()

        var allProperties = metaResolver.getProperties(bean.javaClass, false)
                .exclude(excludeFields)
                .include(includeFields)

        val values = allProperties.mapNotNull { getter ->
            Pair(getter, getter.get(bean))
        }.toMap().filter { it.value != null }

        val properties = allProperties filter {
            values.containsKey(it)
        }

        try {
            if (!batch || cachedStatement == null || !backend.isInTransaction()) {
                val sql = StringBuilder("insert into ").append(entity).append("(")
                properties.map { it.name }.joinTo(sql, ",")
                sql.append(") values(")
                for (i in 0..properties.size() - 1) {
                    if (i > 0) sql.append(",")
                    sql.append("?")
                }
                sql.append(")")

                sqlString = sql.toString()
                logger.info(sqlString)

                cachedStatement = sqlCon.prepareStatement(sqlString, if (getkeys) Statement.RETURN_GENERATED_KEYS else java.sql.Statement.NO_GENERATED_KEYS)
            }
            try {
                if (checkNullability) {
                    backend.checkNullability(entity!!, bean, sqlCon, properties)
                }

                for ((index,property) in properties.withIndex()) {
                    logger.debug("setInsertParameter " + property.name + " <" + property.classType.getName() + ">")
                    SqlConverter.setParameter(cachedStatement!!, values[property], index + 1, property.classType, null)
                }

                cachedStatement!!.executeUpdate();

                if (getkeys) {
                    try {
                        var key: Long = 0;
                        val keys = cachedStatement!!.getGeneratedKeys()
                        if (keys != null) {
                            if (keys.next()) {
                                key = keys.getLong(1)
                            }
                            keys.close()
                        }
                        return key;
                    } catch (ignore: AbstractMethodError) {
                    } catch (ignore: SQLException) {}
                }
            } finally {
                if (!batch) {
                    cachedStatement!!.close();
                    cachedStatement = null;
                }
            }
            return 0
        } catch (sqlx: SQLException) {
            throw PersistenceException("insert <$sqlString> failed with parameters ${values.values()}", sqlx)
        } finally {
            backend.closeConnection(sqlCon)
        }
    }

    override fun excludeFields(vararg excludes: String): Insert {
        this.excludeFields = excludes
        return this
    }

    override fun includeFields(vararg includes: String): Insert {
        this.includeFields = includes
        return this
    }
}