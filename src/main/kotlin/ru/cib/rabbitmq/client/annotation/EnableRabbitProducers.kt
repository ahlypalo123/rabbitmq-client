package ru.cib.rabbitmq.client.annotation

import org.springframework.context.annotation.Import
import ru.cib.rabbitmq.client.RabbitProducersRegistrar

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(RabbitProducersRegistrar::class)
annotation class EnableRabbitProducers(
    val value: String = "",
    val basePackages: Array<String> = [],
)