package cl.duoc.ejemplo.ms.administracion.archivos.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class GuiaDespachoConsumer {

    // Se queda escuchando la cola que definimos en application.properties
    @RabbitListener(queues = "${guia.rabbitmq.queue}")
    public void recibirMensaje(String mensaje) {
        System.out.println("=================================================");
        System.out.println("📩 [RabbitMQ Consumidor] Mensaje recibido de la cola:");
        System.out.println(mensaje);
        System.out.println("=================================================");
        
        // Aquí podrías agregar lógica extra, como enviar un email al cliente, etc.
    }
}
