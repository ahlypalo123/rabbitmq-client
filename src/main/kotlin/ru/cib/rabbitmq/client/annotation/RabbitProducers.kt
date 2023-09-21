package ru.cib.rabbitmq.client.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RabbitProducers(
    val value: String,
)