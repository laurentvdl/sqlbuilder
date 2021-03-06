package sqlbuilder.impl

import sqlbuilder.Configuration
import sqlbuilder.impl.mappers.BigDecimalMapper
import sqlbuilder.impl.mappers.BooleanMapper
import sqlbuilder.impl.mappers.ByteArrayMapper
import sqlbuilder.impl.mappers.CharMapper
import sqlbuilder.impl.mappers.DateMapper
import sqlbuilder.impl.mappers.DoubleMapper
import sqlbuilder.impl.mappers.EnumNameMapper
import sqlbuilder.impl.mappers.FloatMapper
import sqlbuilder.impl.mappers.InputStreamMapper
import sqlbuilder.impl.mappers.IntegerMapper
import sqlbuilder.impl.mappers.LocalDateMapper
import sqlbuilder.impl.mappers.LocalDateTimeMapper
import sqlbuilder.impl.mappers.LongMapper
import sqlbuilder.impl.mappers.ReaderMapper
import sqlbuilder.impl.mappers.ShortMapper
import sqlbuilder.impl.mappers.StringMapper
import sqlbuilder.impl.mappers.TimestampMapper
import sqlbuilder.mapping.ToObjectMapper
import sqlbuilder.mapping.ToSQLMapper
import sqlbuilder.meta.BeanFactory
import sqlbuilder.meta.FieldPropertyResolver
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.MethodPropertyResolver
import sqlbuilder.meta.PropertyResolver
import sqlbuilder.meta.ReflectionBeanFactory
import sqlbuilder.meta.ReflectionResolver
import java.util.ArrayList

/**
 * @author Laurent Van der Linden.
 */
open class DefaultConfiguration : Configuration {
    private val toObjectMappers: MutableList<ToObjectMapper> = ArrayList()
    private val toSQLMappers: MutableList<ToSQLMapper> = ArrayList()

    protected fun registerDefaultMappers() {
        val dualPurposeMappers = listOf(
                StringMapper(),
                CharMapper(),
                LongMapper(),
                IntegerMapper(),
                ShortMapper(),
                DoubleMapper(),
                FloatMapper(),
                BigDecimalMapper(),
                InputStreamMapper(),
                ByteArrayMapper(),
                ReaderMapper(),
                DateMapper(),
                TimestampMapper(),
                EnumNameMapper(),
                BooleanMapper(),
                LocalDateMapper(),
                LocalDateTimeMapper()
        )

        dualPurposeMappers.forEach {
            registerToObjectMapper(it)
            registerToSQLMapper(it)
        }
    }

    override fun createMetaResolver(): MetaResolver = ReflectionResolver(this)

    var escapeCharacter: Char? = null

    fun setEscapeCharacter(escapeCharacter: Char): Configuration {
        this.escapeCharacter = escapeCharacter
        return this
    }

    override fun escapeEntity(entity: String?): String? {
        if (entity != null) {
            if (escapeCharacter != null) {
                if (entity[0] != escapeCharacter) {
                    return escapeCharacter.toString() + entity + escapeCharacter.toString()
                }
            }
        }
        return entity
    }

    override fun objectMapperForType(targetType: Class<*>): ToObjectMapper? {
        return toObjectMappers.lastOrNull { it.handles(targetType) }
    }

    override fun sqlMapperForType(targetType: Class<*>): ToSQLMapper? {
        return toSQLMappers.lastOrNull { it.handles(targetType) }
    }

    override fun registerToObjectMapper(toObjectMapper: ToObjectMapper): DefaultConfiguration {
        toObjectMappers.add(toObjectMapper)
        return this
    }

    override fun unregisterToObjectMapper(toObjectMapper: ToObjectMapper): DefaultConfiguration {
        toObjectMappers.remove(toObjectMapper)
        return this
    }

    override fun registerToSQLMapper(toSQLMapper: ToSQLMapper): DefaultConfiguration {
        toSQLMappers.add(toSQLMapper)
        return this
    }

    override fun unregisterToSQLMapper(toSQLMapper: ToSQLMapper): DefaultConfiguration {
        toSQLMappers.remove(toSQLMapper)
        return this
    }

    override fun beanFactroy(): BeanFactory {
        return ReflectionBeanFactory()
    }

    override var propertyResolutionStrategies: List<PropertyResolver> = listOf(
            MethodPropertyResolver(),
            FieldPropertyResolver()
    )

    init {
        registerDefaultMappers()
    }
}