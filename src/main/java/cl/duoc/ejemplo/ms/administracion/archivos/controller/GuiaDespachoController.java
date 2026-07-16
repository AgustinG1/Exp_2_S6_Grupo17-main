package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoRequest;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoResponse;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;
import cl.duoc.ejemplo.ms.administracion.archivos.service.PdfGenerationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

    private final PdfGenerationService pdfGenerationService;
    private final AwsS3Service awsS3Service;
    private final EfsService efsService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${aws.s3.bucket.name:grupo17-bucket}")
    private String defaultBucket;

    @Value("${guia.rabbitmq.queue}")
    private String queueName;

    /**
     * Generar guía y subir a EFS + S3.
     */
    @PostMapping("/generar")

    public ResponseEntity<GuiaDespachoResponse> generarGuia(@RequestBody GuiaDespachoRequest request) {
        try {
            if (request.getId() == null) {
                request.setId(UUID.randomUUID().toString());
            }

            // Generate PDF
            byte[] pdfBytes = pdfGenerationService.generateGuiaDespachoPdf(request);

            // Construct Key: YYYYMM/transportista_fecha_id.pdf
            String folder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String fileName = request.getTransportista().replaceAll("\\s+", "") + "_" 
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" 
                            + request.getId() + ".pdf";
            String s3Key = folder + "/" + fileName;

            // Save to EFS temp storage
            efsService.saveToEfsBytes(s3Key, pdfBytes);

            // Upload to S3
            awsS3Service.uploadBytes(defaultBucket, s3Key, pdfBytes, "application/pdf");

            // Enviar mensaje asíncrono a RabbitMQ
            String mensaje = "Nueva guía generada con ID: " + request.getId() + " con destino a: " + request.getDestino();
            rabbitTemplate.convertAndSend(queueName, mensaje);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GuiaDespachoResponse(request.getId(), request.getTransportista(), s3Key, null, "Guía generada y subida exitosamente"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Buscar guías filtradas por transportista y fecha en base al prefijo
     */
    @GetMapping("/buscar")

    public ResponseEntity<List<GuiaDespachoResponse>> buscarGuias(
            @RequestParam(required = false) String fechaYYYYMM,
            @RequestParam(required = false) String transportista) {

        String prefix = "";
        if (fechaYYYYMM != null && !fechaYYYYMM.isEmpty()) {
            prefix = fechaYYYYMM + "/";
            if (transportista != null && !transportista.isEmpty()) {
                prefix += transportista.replaceAll("\\s+", "");
            }
        }

        List<S3ObjectDto> objects = awsS3Service.listObjects(defaultBucket, prefix);

        List<GuiaDespachoResponse> response = objects.stream().map(obj -> {
            String id = obj.getKey().substring(obj.getKey().lastIndexOf("_") + 1).replace(".pdf", "");
            String trans = transportista != null ? transportista : "Desconocido (Extraer del Key)";
            return new GuiaDespachoResponse(id, trans, obj.getKey(), null, "Encontrado");
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Descargar guía (Limitado a ROL_DOWNLOAD o ADMIN)
     */
    @GetMapping("/descargar")

    public ResponseEntity<byte[]> descargarGuia(@RequestParam String key) {
        byte[] fileBytes = awsS3Service.downloadAsBytes(defaultBucket, key);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key.substring(key.lastIndexOf("/") + 1) + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }

    /**
     * Actualizar/Modificar guía (Pisa la anterior)
     */
    @PutMapping("/actualizar")

    public ResponseEntity<GuiaDespachoResponse> actualizarGuia(
            @RequestParam String key, 
            @RequestBody GuiaDespachoRequest request) {
        try {
            if (request.getId() == null) {
                request.setId(key.substring(key.lastIndexOf("_") + 1).replace(".pdf", ""));
            }

            byte[] pdfBytes = pdfGenerationService.generateGuiaDespachoPdf(request);

            // Overwrite in EFS and S3
            efsService.saveToEfsBytes(key, pdfBytes);
            awsS3Service.uploadBytes(defaultBucket, key, pdfBytes, "application/pdf");

            return ResponseEntity.ok(new GuiaDespachoResponse(request.getId(), request.getTransportista(), key, null, "Guía modificada exitosamente"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Eliminar guía
     */
    @DeleteMapping("/eliminar")

    public ResponseEntity<Void> eliminarGuia(@RequestParam String key) {
        awsS3Service.deleteObject(defaultBucket, key);
        return ResponseEntity.noContent().build();
    }
}
