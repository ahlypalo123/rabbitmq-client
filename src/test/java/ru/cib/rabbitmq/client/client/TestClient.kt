package ru.cib.rabbitmq.client.client

import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import ru.cib.rabbitmq.client.annotation.RabbitProducer
import ru.cib.rabbitmq.client.annotation.RabbitProducers

@RabbitProducers("TestClient")
interface TestClient {

    @RabbitProducer("sayGoodbye")
    fun sayGoodbye(name: String) : String

    @RabbitProducer("sayHello")
    fun sayHello(name: String, @Header("Content-Type") contentType: String) : Message<String>
}