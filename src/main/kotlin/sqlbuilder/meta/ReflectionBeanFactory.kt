package sqlbuilder.meta

import sqlbuilder.exceptions.NoDefaultConstructorException

class ReflectionBeanFactory : BeanFactory {
    override fun <T : Any> instantiate(beanClass: Class<T>): T {
        try {
            return beanClass.newInstance()
        } catch (e: Throwable) {
            val cause = e.cause ?: e
            if (cause is NoSuchMethodException || cause is InstantiationException) {
                throw NoDefaultConstructorException(beanClass)
            } else {
                throw e
            }
        }
    }
}