package ru.cib.rabbitmq.client

import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.ClassUtils
import ru.cib.rabbitmq.client.annotation.EnableRabbitProducers
import ru.cib.rabbitmq.client.annotation.RabbitProducers

class RabbitProducersRegistrar : ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private lateinit var resourceLoader: ResourceLoader
    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    private lateinit var environment: Environment
    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    override fun registerBeanDefinitions(metadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        registerClients(metadata, registry)
    }

    private fun getScanner(): ClassPathScanningCandidateComponentProvider {
        return object : ClassPathScanningCandidateComponentProvider(false, this.environment) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                var isCandidate = false
                if (beanDefinition.metadata.isIndependent) {
                    if (!beanDefinition.metadata.isAnnotation) {
                        isCandidate = true
                    }
                }
                return isCandidate
            }
        }
    }

    private fun getBasePackages(importingClassMetadata: AnnotationMetadata): ArrayList<String> {
        val attributes = importingClassMetadata
            .getAnnotationAttributes(EnableRabbitProducers::class.java.canonicalName)
        val basePackages = arrayListOf<String>()
        val value = attributes?.get("value") as String
        if (value.isNotBlank()) {
            basePackages.add(value)
        }
        basePackages.addAll(attributes["basePackages"] as Array<String>)
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.className))
        }
        return basePackages
    }

    private fun registerClients(metadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val candidateComponents = LinkedHashSet<BeanDefinition>()
        val scanner: ClassPathScanningCandidateComponentProvider = getScanner()
        scanner.resourceLoader = this.resourceLoader
        scanner.addIncludeFilter(AnnotationTypeFilter(RabbitProducers::class.java))
        getBasePackages(metadata).forEach { basePackage ->
            candidateComponents.addAll(scanner.findCandidateComponents(basePackage))
        }
        candidateComponents.forEach {
            if (it is AnnotatedBeanDefinition) {
                registerClientBeanDefinition(it, registry)
            }
        }
    }

    private fun registerClientBeanDefinition(beanDefinition: AnnotatedBeanDefinition, registry: BeanDefinitionRegistry) {
        val metadata = beanDefinition.metadata
        val className = metadata.className
        @Suppress("UNCHECKED_CAST")
        val type: Class<Any> = ClassUtils.resolveClassName(className, null) as Class<Any>
        val factory = RabbitProducersFactoryBean(type, (registry as ConfigurableBeanFactory))
        val builder = BeanDefinitionBuilder.genericBeanDefinition(type) {
            factory.beanDefinition = beanDefinition
            factory.getObject()
        }
        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
        builder.setLazyInit(true)
        val definition = builder.beanDefinition
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className)
        definition.setAttribute("cibIntegrationClientFactoryBean", factory)

        val name = metadata.getAnnotationAttributes(RabbitProducers::class.qualifiedName!!)?.get("value")?.toString()!!
        val holder = BeanDefinitionHolder(definition, name)
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry)
    }
}