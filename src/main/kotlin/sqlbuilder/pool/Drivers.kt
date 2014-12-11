package sqlbuilder.pool

public trait Drivers {
    class object {
        public val MYSQL: String = "com.mysql.jdbc.Driver"
        public val H2: String = "org.h2.Driver"
        public val DB2: String = "com.ibm.db2.jcc.DB2Driver"
    }
}
