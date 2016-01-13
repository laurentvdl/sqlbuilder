package sqlbuilder.impl

import sqlbuilder.Configuration
import sqlbuilder.impl.mappers.*
import sqlbuilder.mapping.ToObjectMapper
import sqlbuilder.mapping.ToSQLMapper
import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.StaticJavaResolver
import java.util.*

/**
 * @author Laurent Van der Linden.
 */
public open class DefaultConfiguration : Configuration {
    private val toObjectMappers: MutableList<ToObjectMapper> = ArrayList()
    private val toSQLMappers: MutableList<ToSQLMapper> = ArrayList()

    constructor() {
        registerDefaultMappers()
    }

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
                EnumMapper(),
                BooleanMapper()
        )

        dualPurposeMappers.forEach {
            registerToObjectMapper(it)
            registerToSQLMapper(it)
        }
    }

    override var metaResolver: MetaResolver = StaticJavaResolver(this)
    var escapeCharacter: Char? = null

    public fun setEscapeCharacter(escapeCharacter: Char): Configuration {
        this.escapeCharacter = escapeCharacter
        return this
    }

    public override fun escapeEntity(entity: String?): String? {
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
        val lastOrNull = toObjectMappers.lastOrNull { it.handles(targetType) }
        return lastOrNull
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
}