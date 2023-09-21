package ru.cib.rabbitmq.client.listener

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service

@Service
class TestListener {

    @RabbitListener(queues = ["sayGoodbye"])
    fun sayGoodbye(name: String, message: org.springframework.amqp.core.Message) : String {
//        println("Получено: $message")
        return "Пока, $name"
    }

    @RabbitListener(queues = ["sayHello"])
    fun sayHello(name: String, @Header(value = "Content-Type", required = false) contentType: String) : Message<String> {
//        throw Exception("Я упал")
//        println("Получено: $name, replyTo: ${message.messageProperties.replyToAddress}")
        return GenericMessage("Привет, $name", mapOf("Content-Type" to contentType))
    }
}