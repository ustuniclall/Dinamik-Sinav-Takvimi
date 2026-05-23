import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class OgrenciExcelOkuyucu {

    public static boolean run(Stage stage, Kullanici aktifKullanici) {
        try {
            // 📂 1) Excel dosyasını kullanıcıdan seçtir
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Öğrenci Listesi Excel Dosyasını Seç");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Dosyaları (*.xlsx)", "*.xlsx")
            );

            File file = fileChooser.showOpenDialog(stage);
            if (file == null) {
                showAlert("İptal Edildi", "Herhangi bir dosya seçilmedi.");
                return false;
            }

            // 🧮 2) Dosyayı oku ve veritabanına aktar
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis);
                 Connection conn = DBManager.getConnection()) {

                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                int bolumId = aktifKullanici.bolumId;
                int eklendi = 0;
                int atlandi = 0;

                System.out.println("✅ Veritabanına bağlandı. Öğrenci listesi okunuyor...");

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String no = getCellString(row.getCell(0), formatter);
                    String adsoyad = getCellString(row.getCell(1), formatter);
                    String rawSinif = getCellString(row.getCell(2), formatter);

                    if (no.isBlank() || adsoyad.isBlank()) continue;

                    // 🔢 Sınıf verisini dönüştür
                    int sinif = 0;
                    try {
                        rawSinif = rawSinif.replaceAll("[^0-9]", "");
                        if (!rawSinif.isEmpty()) sinif = Integer.parseInt(rawSinif);
                        if (sinif > 4) sinif = 4;
                    } catch (Exception e) {
                        System.out.println("⚠ Sınıf okunamadı: " + rawSinif);
                    }

                    // 📘 SQL sorgusu
                    String sql = """
                        INSERT INTO ogrenciler (bolum_id, ogr_no, ad_soyad, sinif_duzeyi)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            ad_soyad = VALUES(ad_soyad),
                            sinif_duzeyi = VALUES(sinif_duzeyi)
                    """;

                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, bolumId);
                        ps.setString(2, no);
                        ps.setString(3, adsoyad);
                        ps.setInt(4, sinif);

                        int result = ps.executeUpdate();
                        if (result > 0) eklendi++;
                        else atlandi++;

                    } catch (Exception ex) {
                        System.out.println("❌ Hata: " + ex.getMessage());
                    }
                }

                showAlert("✅ Öğrenci Yükleme Tamamlandı",
                        "Toplam " + eklendi + " öğrenci eklendi.\n"
                        + atlandi + " kayıt zaten mevcut olduğu için atlandı.");

                System.out.println("🎉 Öğrenci listesi işleme tamamlandı!");
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("❌ Hata", "Dosya okunurken bir hata oluştu:\n" + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ Hata", "İşlem başarısız oldu:\n" + e.getMessage());
            return false;
        }
    }

    // 🔹 Hücreden güvenli string okuma
    private static String getCellString(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    // 🔹 Kullanıcıya bilgi penceresi
    private static void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
