package ru.cib.rabbitmq.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.RemoteInvocationAwareMessageConverterAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {

    @Bean
    fun messageConverter() =
        RemoteInvocationAwareMessageConverterAdapter(Jackson2JsonMessageConverter(ObjectMapper()))
}