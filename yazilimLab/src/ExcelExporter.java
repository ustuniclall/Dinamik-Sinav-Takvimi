import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    public static void exportToExcel(List<Sinav> sinavListesi, String dosyaYolu) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sınav Programı");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);

            // --- SÜTUN BAŞLIKLARI GÜNCELLENDİ ---
            String[] columns = {"Tarih", "Sınav Saati", "Ders Adı", "Öğretim Elemanı", "Derslikler"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (Sinav sinav : sinavListesi) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sinav.getTarih().toString());
                row.createCell(1).setCellValue(sinav.getBaslangicSaati().toString());
                row.createCell(2).setCellValue(sinav.getDersAdi());
                row.createCell(3).setCellValue(sinav.getOgretimElemani());
                row.createCell(4).setCellValue(sinav.getDerslikler()); // --- YENİ DERSLİK BİLGİSİ EKLENDİ ---
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(dosyaYolu)) {
                workbook.write(fileOut);
            }
        }
    }
}