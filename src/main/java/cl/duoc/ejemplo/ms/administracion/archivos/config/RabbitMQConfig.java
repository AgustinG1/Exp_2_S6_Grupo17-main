package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${guia.rabbitmq.queue}")
    private String queueName;

    @Bean
    public Queue queue() {
        // Crea la cola automáticamente de forma persistente (durable = true)
        return new Queue(queueName, true); 
    }
}
