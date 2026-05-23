import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak; // YENİ EKLENDİ
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType; // YENİ EKLENDİ
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfExporter {

    public static final String FONT_PATH = "C:/Windows/Fonts/Arial.ttf";

    public static void createSeatingPlanPdf(File file,
                                            SinavListesiItem sinav,
                                            List<OturmaPlaniItem> planItems,
                                            ByteArrayInputStream imageStream) throws IOException {

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(30, 30, 30, 30);

        PdfFont font;
        PdfFont bold;
        try {
            font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            bold = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException e) {
            System.err.println("Uyarı: Font bulunamadı. Standart fonta dönülüyor.");
            font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        }

        // 1. Başlık Ekleme (Sayfa 1)
        Paragraph title = new Paragraph(sinav.getDersAdi() + " - Oturma Planı")
                .setFont(bold)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        Paragraph subTitle = new Paragraph(sinav.getTarih() + " - " + sinav.getSaat())
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(subTitle);

        document.add(new Paragraph("\n"));

        // 2. Görsel Düzen Resmini Ekleme (Sayfa 1, başlığın altı)
        try {
            byte[] imageBytes = imageStream.readAllBytes();
            Image seatingPlanImage = new Image(ImageDataFactory.create(imageBytes));
            
            // === DEĞİŞİKLİK: Görselin sığması için ölçeklendirme ===
            // 'setWidth' yerine 'scaleToFit' kullanarak en-boy oranını koru
            seatingPlanImage.setAutoScale(true); // Otomatik ölçeklemeyi aç
            seatingPlanImage.scaleToFit(PageSize.A4.getWidth() - 60, PageSize.A4.getHeight() - 100); // Sayfa marjlarına göre sığdır
            seatingPlanImage.setTextAlignment(TextAlignment.CENTER); // Ortala
            
            document.add(seatingPlanImage);
        } catch(Exception e) {
            document.add(new Paragraph("Görsel oturma planı yüklenemedi."));
        }

        // === DEĞİŞİKLİK: Tablo için YENİ SAYFAYA GEÇ ===
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // 3. Öğrenci Listesi Tablosunu Ekleme (Sayfa 2'den itibaren)
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Tablo başlıkları
        table.addHeaderCell(new Paragraph("Öğrenci No").setFont(bold));
        table.addHeaderCell(new Paragraph("Ad Soyad").setFont(bold));
        table.addHeaderCell(new Paragraph("Derslik").setFont(bold));
        table.addHeaderCell(new Paragraph("Sıra").setFont(bold));
        table.addHeaderCell(new Paragraph("Sütun").setFont(bold));

        // Tablo verileri
        for (OturmaPlaniItem item : planItems) {
            table.addCell(new Paragraph(item.getOgrNo()).setFont(font).setFontSize(9));
            table.addCell(new Paragraph(item.getAdSoyad()).setFont(font).setFontSize(9));
            table.addCell(new Paragraph(item.getDerslikAdi()).setFont(font).setFontSize(9));
            table.addCell(new Paragraph(String.valueOf(item.getSira())).setFont(font).setFontSize(9));
            table.addCell(new Paragraph(String.valueOf(item.getSutun())).setFont(font).setFontSize(9));
        }
        document.add(table);

        document.close();
    }
}