package ru.cib.rabbitmq.client

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Address
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.support.GenericMessage
import ru.cib.rabbitmq.client.annotation.RabbitProducer
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

data class Invocation(
    val headers: MutableMap<String, Any> = mutableMapOf(),
    var body: Any? = null,
    val params: Array<out Any>
)

interface InvocationProcessor {
    fun process(obj: Any, method: Method, invocation: Invocation)
}

class ClientInvocationHandler(
    private val expressionResolver: ExpressionResolver,
    private val configurableBeanFactory: ConfigurableBeanFactory,
    private val invocationProcessor: InvocationProcessor?,
) : InvocationHandler {

    private val log = LoggerFactory.getLogger(ClientInvocationHandler::class.java)

    private val endpoints: MutableMap<String, Endpoint> = mutableMapOf()
    private val hashCode: Method
    private val toString: Method
    private val equals: Method

    init {
        val obj = Object::class.java
        hashCode = obj.getDeclaredMethod("hashCode")
        toString = obj.getDeclaredMethod("toString")
        equals = obj.getDeclaredMethod("equals", obj)
    }

    data class Endpoint(
        val method: Method,
        val parameterResolvers: List<(Invocation) -> Unit>,
        val callback: (Invocation) -> Any?
    )

    fun prepareEndpoints(type: Class<*>) {
        type.declaredMethods.forEach {
            prepareEndpoint(it)
        }
    }

    private fun prepareEndpoint(method: Method) {
        val parameterResolvers: MutableList<(Invocation) -> Unit> = mutableListOf()

        val annotation = AnnotationUtils.findAnnotation(method, RabbitProducer::class.java)
            ?: throw Exception("The @RabbitProducer annotation is required for the method $method")
        val returnExceptions = annotation.returnExceptions.let { expressionResolver.resolve(it).toBoolean() }
        val headers = mutableMapOf<String, String>()
        annotation.headers.forEach {
            it.split("=").let { spl ->
                val key = expressionResolver.resolve(spl.first())
                val value = expressionResolver.resolve(spl.last())
                headers[key!!] = value!!
            }
        }
        val address = Address(expressionResolver.resolve(annotation.value))
        val rabbitTemplate = if (annotation.rabbitTemplate.isBlank()) {
            configurableBeanFactory.getBean(RabbitTemplate::class.java)
        } else {
            configurableBeanFactory.getBean(annotation.rabbitTemplate) as RabbitTemplate
        }
        val messageConverter = if (annotation.messageConverter.isBlank()) {
            configurableBeanFactory.getBean(MessageConverter::class.java)
        } else {
            configurableBeanFactory.getBean(annotation.messageConverter) as MessageConverter
        }

        method.parameters.forEachIndexed { i, param ->
            val header = param.findAnnotation(Header::class.java, Headers::class.java)
            if (header == null) {
                parameterResolvers.add { invocation ->
                    invocation.headers.putAll(headers)
                    invocation.body = invocation.params[i]
                }
            } else {
                when (header) {
                    is Header -> {
                        parameterResolvers.add { invocation ->
                            val name = header.value.ifBlank { param.name }
                            invocation.headers[name] = invocation.params[i]
                        }
                    }
                    is Headers -> {
                        parameterResolvers.add { invocation ->
                            @Suppress("UNCHECKED_CAST")
                            invocation.headers.putAll(invocation.params[i] as Map<String, Any>)
                        }
                    }
                }
            }
        }

        val callback: (Invocation) -> Any? = if (returnExceptions == false && method.returnType == Void.TYPE) {
            func@{ invocation ->
                val m = toMessage(invocation.body, invocation.headers, messageConverter)
                if (m.messageProperties.correlationId.isNullOrBlank()) {
                    m.messageProperties.correlationId = UUID.randomUUID().toString()
                }
                log.debug("--> sending async message to the exchange ${address.exchangeName} with key ${address.routingKey}, message: $m")
                rabbitTemplate.send(address.exchangeName, address.routingKey, m)
                return@func null
            }
        } else {
            when (method.returnType) {
                org.springframework.amqp.core.Message::class.java -> {
                    func@{ invocation ->
                        val m = toMessage(invocation.body, invocation.headers, messageConverter)
                        log.debug("--> sending sync message to the exchange ${address.exchangeName} with key ${address.routingKey}, message: $m")
                        return@func rabbitTemplate.sendAndReceive(address.exchangeName, address.routingKey, m).also {
                            log.debug("<-- received correlated response message $it")
                        }
                    }
                }
                Message::class.java, GenericMessage::class.java -> {
                    func@{ invocation ->
                        val m = toMessage(invocation.body, invocation.headers, messageConverter)
                        log.debug("--> sending sync message to the exchange ${address.exchangeName} with key ${address.routingKey}, message: $m")
                        val res = rabbitTemplate.sendAndReceive(address.exchangeName, address.routingKey, m)
                        return@func res?.let {
                            log.debug("<-- received correlated response message $it")
                            GenericMessage(messageConverter.fromMessage(res), res.messageProperties.headers)
                        }
                    }
                }
                else -> {
                    func@{ invocation ->
                        val m = toMessage(invocation.body, invocation.headers, messageConverter)
                        log.debug("--> sending sync message to the exchange ${address.exchangeName} with key ${address.routingKey}, message: $m")
                        val res = rabbitTemplate.sendAndReceive(address.exchangeName, address.routingKey, m).also {
                            log.debug("<-- received correlated response message $it")
                        }
                        return@func res?.let { messageConverter.fromMessage(it) }
                    }
                }
            }
        }

        val endpoint = Endpoint(method, parameterResolvers, callback)
        endpoints[method.toString()] = endpoint
    }

    private fun toMessage(obj: Any?, headers: Map<String, Any>, messageConverter: MessageConverter) : org.springframework.amqp.core.Message {
        val m = if (obj is org.springframework.amqp.core.Message) {
            obj
        } else {
            messageConverter.toMessage(obj ?: "", MessageProperties())
        }
        headers.forEach { (k, v) ->
            m.messageProperties.headers[k] = v
        }
        if (m.messageProperties.messageId.isNullOrBlank()) {
            m.messageProperties.messageId = UUID.randomUUID().toString()
        }
        return m
    }

    override fun invoke(obj: Any, method: Method, params: Array<out Any>?): Any? {
        if (method.declaringClass == Object::class.java) {
            return when (method) {
                hashCode -> super.hashCode()
                toString -> super.toString()
                equals -> super.equals(params?.first())
                else -> null
            }
        }
        val invocation = Invocation(params = params ?: arrayOf())
        val endpoint = endpoints[method.toString()]
        endpoint?.parameterResolvers?.forEach { resolver ->
            resolver(invocation)
        }
        invocationProcessor?.process(obj, method, invocation)
        return endpoint?.callback?.invoke(invocation)
    }

    private fun AnnotatedElement.findAnnotation(vararg annotationTypes: Class<*>) : Annotation? =
        annotations.find { annotationTypes.contains(it.annotationClass.java) }
}