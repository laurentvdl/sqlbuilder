package sqlbuilder.impl

import org.slf4j.LoggerFactory
import sqlbuilder.Backend
import sqlbuilder.Delete
import sqlbuilder.IncorrectMetadataException
import sqlbuilder.PersistenceException
import sqlbuilder.SqlConverter
import java.sql.SQLException

class DeleteImpl(private val backend: Backend): Delete {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val metaResolver = backend.metaResolver

    private var entity: String? = null

    override fun from(entity: String): Delete {
        this.entity = entity
        return this
    }

    override fun deleteBean(bean: Any): Int {
        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(bean.javaClass)
            )
        }

        val keys = metaResolver.getKeys(bean.javaClass)
        if (keys.isEmpty()) {
            throw IncorrectMetadataException("cannot delete bean without a list of keys")
        }

        val sql = StringBuilder("delete from ").append(entity).append(" where ")

        try {
            keys.map({"${it.columnName} = ?"}).joinTo(sql, " and ")

            val con = backend.getSqlConnection()

            try {
                val sqlString = sql.toString()
                logger.info(sqlString)

                val sqlConverter = SqlConverter(backend.configuration)

                val ps = con.prepareStatement(sqlString)!!

                for ((index,key) in keys.withIndex()) {
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