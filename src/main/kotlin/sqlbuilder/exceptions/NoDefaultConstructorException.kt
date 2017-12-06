package sqlbuilder.exceptions

import sqlbuilder.IncorrectMetadataException

class NoDefaultConstructorException(val beanClass: Class<*>) : IncorrectMetadataException("provide a default constructor for class $beanClass")