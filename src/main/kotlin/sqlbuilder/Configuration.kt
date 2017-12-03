package sqlbuilder

import sqlbuilder.mapping.ToObjectMapper
import sqlbuilder.mapping.ToSQLMapper
import sqlbuilder.meta.BeanFactory
import sqlbuilder.meta.MetaResolver

/**
 * Configuration container for all pluggable aspects of a SqlBuilder instance.
 */
interface Configuration {
    fun createMetaResolver(): MetaResolver

    fun escapeEntity(entity: String?): String?

    fun objectMapperForType(targetType: Class<*>): ToObjectMapper?

    fun sqlMapperForType(targetType: Class<*>): ToSQLMapper?

    fun registerToObjectMapper(toObjectMapper: ToObjectMapper): Configuration

    fun unregisterToObjectMapper(toObjectMapper: ToObjectMapper): Configuration

    fun registerToSQLMapper(toSQLMapper: ToSQLMapper): Configuration

    fun unregisterToSQLMapper(toSQLMapper: ToSQLMapper): Configuration

    fun beanFactroy(): BeanFactory
}
