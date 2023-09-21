package ru.cib.rabbitmq.client.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RabbitProducer(
    val value: String = "",
    val returnExceptions: String = "false",
    val rabbitTemplate: String = "",
    val messageConverter: String = "",
    val headers: Array<String> = [],
)
