package sqlbuilder.pool

public interface Drivers {
    companion object {
        const public val MYSQL: String = "com.mysql.jdbc.Driver"
        const public val H2: String = "org.h2.Driver"
        const public val DB2: String = "com.ibm.db2.jcc.DB2Driver"
    }
}
