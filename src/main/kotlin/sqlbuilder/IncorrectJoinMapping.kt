package sqlbuilder

class IncorrectJoinMapping(query: String, newIndex: Int, oldIndex: Int, table: String, column: String) : PersistenceException(
        "$query failed: the resultset column at index $oldIndex has the same signature as the column at " +
        "index $newIndex: table $table and column $column, try to distinguish both columns by using prefixed aliases manually or " +
        "using select macros, for instance 'select {${table.toLowerCase().capitalize()}.* as ${table.toLowerCase()}}'")
