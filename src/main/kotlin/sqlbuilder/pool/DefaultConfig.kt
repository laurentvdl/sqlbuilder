package sqlbuilder.pool

import java.util.Properties

class DefaultConfig(override val username: String?, override val password: String?,
                           override val url: String, override val driverClassName: String) : ConnectionConfigProvider {

    override val properties: Properties? = null
    override val identityPlugin: IdentityPlugin? = null
}