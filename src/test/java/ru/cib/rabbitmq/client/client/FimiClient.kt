package ru.cib.rabbitmq.client.client

import org.springframework.messaging.handler.annotation.Header
import ru.cib.rabbitmq.client.annotation.RabbitProducer
import ru.cib.rabbitmq.client.annotation.RabbitProducers

/**
 * Клиент для отправки сообщений в TWO с помощью fimi-transporter
 */
@RabbitProducers("fimi-client")
interface FimiClient {

    /**
     * @param request строка в формате UAMP. Может быть сформирована с помощью сервиса json2uamp
     * @param connection id подключения. Настраивается в конфигурации сервиса fimi-transporter
     * @return ответ от TWO в формате JSON
     */
    @RabbitProducer("fimi-gateway-uamp")
    fun onMessageUamp(request: String, @Header("connection") connection: String): String

    /**
     * @param connection id подключения. Настраивается в конфигурации сервиса fimi-transporter
     * @param operation название UserDefined операции
     * @param request строка в формате JSON. Полученный JSON оборачивается в UAMP
     * @return ответ от TWO в формате JSON
     */
    @RabbitProducer("fimi-gateway-json")
    fun onMessageJson(request: String, @Header("connection") connection: String, @Header("operation") operation: String): String
}