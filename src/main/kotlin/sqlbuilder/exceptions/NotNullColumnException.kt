package sqlbuilder.exceptions

import sqlbuilder.PersistenceException

class NotNullColumnException(val columns: List<String>)
    : PersistenceException("column(s) ${columns.joinToString(prefix = "[", postfix = "]")} are not nullable, yet not included in your update/insert")