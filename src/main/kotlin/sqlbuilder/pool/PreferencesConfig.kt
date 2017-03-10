package sqlbuilder.pool

import java.util.Properties
import java.util.prefs.Preferences

/**
 * Create a datasource and configure it from the user Preferences node specified:
 * <ul>
 *   <li>url</li>
 *   <li>username</li>
 *   <li>password</li>
 *   <li>driver</li>
 * </ul>
 * @param preferencesNodePath
 *
 * @author Laurent Van der Linden
 */
class PreferencesConfig(preferencesPath: String, user: Boolean) : ConnectionConfigProvider {
    private val preferencesNode = if (user) {
        Preferences.userRoot()!!.node(preferencesPath)!!
    } else {
        Preferences.systemRoot()!!.node(preferencesPath)!!
    }

    override val username: String? = preferencesNode.get("username", null)


    override val password: String? = preferencesNode.get("password", null)


    override val url: String = preferencesNode.get("url", null)!!


    override val driverClassName: String = preferencesNode.get("driver", null)!!

    override val properties: Properties? = null

    override val identityPlugin: IdentityPlugin? = null
}