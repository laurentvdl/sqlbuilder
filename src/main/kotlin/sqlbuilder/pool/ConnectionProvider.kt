package sqlbuilder.pool

import java.sql.Connection

interface ConnectionProvider {
    val connection: Connection
    val identifier: String
}
