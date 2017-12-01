package sqlbuilder.exceptions

import sqlbuilder.PersistenceException

class CacheException(message: String, cause: Exception? = null) : PersistenceException(message, cause)