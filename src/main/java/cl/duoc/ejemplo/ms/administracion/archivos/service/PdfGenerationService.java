package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaDespachoRequest;

@Service
public class PdfGenerationService {

    public byte[] generateGuiaDespachoPdf(GuiaDespachoRequest request) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Guía de Despacho", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" ")); // Blank line

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        
        document.add(new Paragraph("ID Guía: " + request.getId(), normalFont));
        document.add(new Paragraph("Transportista: " + request.getTransportista(), normalFont));
        document.add(new Paragraph("Fecha de Generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Detalle de la Carga: " + request.getDetalleCarga(), normalFont));
        document.add(new Paragraph("Peso Total: " + request.getPesoTotal() + " kg", normalFont));
        document.add(new Paragraph("Destino: " + request.getDestino(), normalFont));
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph("---", normalFont));
        document.add(new Paragraph("Documento generado automáticamente por Sistema Cloud Native - Grupo 17", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));

        document.close();
        
        return out.toByteArray();
    }
}
