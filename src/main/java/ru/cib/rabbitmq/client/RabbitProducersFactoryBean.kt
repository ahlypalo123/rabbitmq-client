package ru.cib.rabbitmq.client

import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import java.lang.reflect.Proxy

class RabbitProducersFactoryBean(
    private val type: Class<*>,
    private val configurableBeanFactory: ConfigurableBeanFactory,
) : FactoryBean<Any> {

    lateinit var beanDefinition: AnnotatedBeanDefinition
    private val expressionResolver = ExpressionResolver(configurableBeanFactory)

    override fun getObject(): Any? {
        val invocationProcessor = configurableBeanFactory.getBeanProvider(InvocationProcessor::class.java).ifAvailable
        val invocationHandler = ClientInvocationHandler(expressionResolver, configurableBeanFactory, invocationProcessor)
        invocationHandler.prepareEndpoints(type)
        return Proxy.newProxyInstance(
            RabbitProducersFactoryBean::class.java.classLoader,
            arrayOf(type),
            invocationHandler)
    }

    override fun getObjectType(): Class<*> = type
}