package sqlbuilder.pool

import java.util.Properties

trait ConnectionConfigProvider {
    val username: String?
    val password: String?
    val url: String
    val driverClassName: String
    val properties: Properties?
    val identityPlugin: IdentityPlugin?
}
