package sqlbuilder

import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.StaticJavaResolver

/**
 * Configuration container for all pluggable aspects of a SqlBuilder instance.
 * User: Laurent Van der Linden
 */
public class Configuration() {
    var metaResolver: MetaResolver = StaticJavaResolver()
    var escapeCharacter: Char? = null

    public fun setEscapeCharacter(escapeCharacter: Char): Configuration {
        this.escapeCharacter = escapeCharacter
        return this
    }

    public fun escapeEntity(entity: String?): String? {
        if (entity != null) {
            if (escapeCharacter != null) {
                if (entity.charAt(0) != escapeCharacter) {
                    return escapeCharacter.toString() + entity + escapeCharacter.toString()
                }
            }
        }
        return entity
    }
}
