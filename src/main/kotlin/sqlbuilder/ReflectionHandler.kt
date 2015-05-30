package sqlbuilder

import sqlbuilder.meta.MetaResolver

/**
 * Handler that uses reflection to map ResultSet values to bean properties.
 *
 * @author Laurent Van der Linden
 */
interface ReflectionHandler {
    var metaResolver: MetaResolver?
}
