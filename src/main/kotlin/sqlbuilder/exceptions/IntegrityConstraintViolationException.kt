package sqlbuilder.exceptions

import sqlbuilder.PersistenceException
import java.sql.SQLIntegrityConstraintViolationException

class IntegrityConstraintViolationException(message: String, cause: SQLIntegrityConstraintViolationException) : PersistenceException(message, cause)