package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDespachoResponse {
    private String id;
    private String transportista;
    private String s3Key;
    private String url; // Optional pre-signed url if needed
    private String message;
}
