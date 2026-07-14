package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import lombok.Data;

@Data
public class GuiaDespachoRequest {
    private String id; // Optional, will be auto-generated if null
    private String transportista;
    private String detalleCarga;
    private Double pesoTotal;
    private String destino;
}
