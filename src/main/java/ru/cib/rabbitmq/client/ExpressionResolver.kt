package ru.cib.rabbitmq.client

import org.springframework.beans.factory.config.BeanExpressionContext
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.expression.StandardBeanExpressionResolver

class ExpressionResolver(private val configurableBeanFactory: ConfigurableBeanFactory) {

    private val resolver = StandardBeanExpressionResolver()
    private val expressionContext = BeanExpressionContext(configurableBeanFactory, null)

    fun resolve(str: String) : String? {
        val value = configurableBeanFactory.resolveEmbeddedValue(str)
        return resolver.evaluate(value, expressionContext)?.toString()
    }
}