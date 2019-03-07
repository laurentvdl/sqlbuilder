package sqlbuilder.pool

import java.util.Properties

interface ConnectionConfigProvider {
    val username: String?
    val password: String?
    val url: String
    val driverClassName: String
    val properties: Properties?
    @Deprecated(message = "set the IdentityPlugin directly on the datasource implementation instead")
    val identityPlugin: IdentityPlugin?
}
