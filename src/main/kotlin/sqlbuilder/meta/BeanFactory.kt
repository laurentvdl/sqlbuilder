package sqlbuilder.meta

interface BeanFactory {
    fun <T : Any> instantiate(beanClass: Class<T>): T
}