package ru.cib.rabbitmq.client

import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.cib.rabbitmq.client.client.FimiClient
import ru.cib.rabbitmq.client.client.TestClient

@SpringBootTest
class ApplicationTest {

    @MockBean(name = "TestClient")
    private lateinit var testClient: TestClient
    @MockBean
    private lateinit var fimiClient: FimiClient
    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Test
    fun sayHello() {
        try {
            println(testClient.sayHello("Ricardo Milos", "application/text"))
            val captor = ArgumentCaptor.forClass(String::class.java)
            verify(testClient).sayHello(captor.capture(), anyString())
            println(captor.value)
        } catch (e: Exception) {
            println("Поймал!!!")
            e.printStackTrace()
        }
    }

    @Test
    fun sayGoodbye() {
        println(testClient.sayGoodbye("Ricardo Milos"))
    }

    @Test
    fun sayGoodbyeWithTemplate() {
        println(rabbitTemplate.convertSendAndReceive("", "sayHello", "Ricardo Milos"))
    }
}