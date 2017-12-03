package sqlbuilder.exceptions

import sqlbuilder.IncorrectMetadataException

class NoDefaultConstructorException(beanClass: Class<*>) : IncorrectMetadataException("provide a default constructor for class $beanClass")