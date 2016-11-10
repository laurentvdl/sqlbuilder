package sqlbuilder.impl

import sqlbuilder.Backend
import sqlbuilder.Delete
import java.sql.SQLException
import org.slf4j.LoggerFactory
import sqlbuilder.PersistenceException
import sqlbuilder.SqlConverter

class DeleteImpl(private val backend: Backend): Delete {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var entity: String? = null

    public override fun from(entity: String): Delete {
        this.entity = entity
        return this
    }

    public override fun deleteBean(bean: Any): Int {
        val metaResolver = backend.configuration.metaResolver

        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(bean.javaClass)
            )
        }

        val keys = metaResolver.getKeys(bean.javaClass)
        if (keys.isEmpty()) {
            throw PersistenceException("cannot delete bean without a list of keys")
        }

        val sql = StringBuilder("delete from ").append(entity).append(" where ")

        val allProperties = metaResolver.getProperties(bean.javaClass, false)
        val keyProperties = allProperties.filter {
            keys.contains(it.name)
        }

        try {
            keyProperties.map({"${it.columnName} = ?"}).joinTo(sql, " and ")

            val con = backend.getSqlConnection()

            try {
                val sqlString = sql.toString()
                logger.info(sqlString)

                val sqlConverter = SqlConverter(backend.configuration)

                val ps = con.prepareStatement(sqlString)!!

                for ((index,key) in keyProperties.withIndex()) {
                    sqlConverter.setParameter(ps, key.get(bean), index + 1, key.classType, null)
                }
                return ps.executeUpdate()
            } finally {
                backend.closeConnection(con)
            }
        } catch (e: SQLException) {
            throw PersistenceException("delete <$sql> failed", e)
        }

    }
}