package sqlbuilder.pool

interface Drivers {
    companion object {
        const val MYSQL = "com.mysql.jdbc.Driver"
        const val H2 = "org.h2.Driver"
        const val DB2 = "com.ibm.db2.jcc.DB2Driver"
        const val ORACLE = "oracle.jdbc.OracleDriver"
    }
}
