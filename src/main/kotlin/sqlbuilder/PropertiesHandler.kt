package sqlbuilder

import sqlbuilder.meta.PropertyReference

/**
 * Handler that needs a list of bean properties to map ResultSet values to bean properties.
 *
 * @author Laurent Van der Linden
 */
trait PropertiesHandler {
    var properties: List<PropertyReference>?
}