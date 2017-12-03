package sqlbuilder.meta

import sqlbuilder.exceptions.NoDefaultConstructorException

class ReflectionBeanFactory : BeanFactory {
    override fun <T : Any> instantiate(beanClass: Class<T>): T {
        try {
            return beanClass.newInstance()
        } catch (e: InstantiationException) {
            if (e.cause is NoSuchMethodException) {
                throw NoDefaultConstructorException(beanClass)
            } else {
                throw e
            }
        }
    }
}