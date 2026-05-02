package tn.esprit.arctic.derbelmicroservice.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionItemResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.service.IPdfGeneratorService;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorServiceImpl implements IPdfGeneratorService {

    @Override
    public byte[] generatePrescriptionPdf(PrescriptionResponseDTO prescription) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Titre
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Paragraph title = new Paragraph("HOPITAL - ORDONNANCE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // Informations de la prescription
        document.add(new Paragraph("Prescription N° : " + prescription.getId()));
        document.add(new Paragraph("Date de création : " + prescription.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        document.add(new Paragraph("Statut : " + prescription.getStatus()));
        document.add(Chunk.NEWLINE);

        // Tableau des médicaments
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        PdfPCell c1 = new PdfPCell(new Phrase("Médicament"));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Dosage"));
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("Fréquence"));
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("Quantité"));
        c4.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c4);

        if (prescription.getItems() != null) {
            for (PrescriptionItemResponseDTO item : prescription.getItems()) {
                table.addCell(item.getMedicine().getName());
                table.addCell(item.getDosage() != null ? item.getDosage() : "-");
                table.addCell(item.getFrequency() != null ? item.getFrequency() : "-");
                table.addCell(String.valueOf(item.getQuantity()));
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        // Préparation du contenu du QR Code
        StringBuilder qrText = new StringBuilder();
        qrText.append("Prescription N°: ").append(prescription.getId()).append("\n");
        qrText.append("Statut: ").append(prescription.getStatus()).append("\n");
        if (prescription.getItems() != null) {
            qrText.append("Médicaments:\n");
            for (PrescriptionItemResponseDTO item : prescription.getItems()) {
                qrText.append("- ").append(item.getQuantity()).append("x ").append(item.getMedicine().getName()).append("\n");
            }
        }
        qrText.append("\nDocument officiel certifié.");

        // Génération de l'image QR Code
        byte[] qrCodeImage = generateQRCodeImage(qrText.toString(), 200, 200);
        Image image = Image.getInstance(qrCodeImage);
        image.setAlignment(Element.ALIGN_CENTER);
        document.add(image);

        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Scannez ce code QR pour vérifier l'authenticité de l'ordonnance.");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private byte[] generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}
