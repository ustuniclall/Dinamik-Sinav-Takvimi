import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException; // YENİ EKLENDİ
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DersExcelOkuyucu {

    public static boolean run(Stage stage, Kullanici aktifKullanici) {
        try {
            // 📂 1) Excel dosyasını kullanıcıdan seçtir
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Ders Listesi Excel Dosyasını Seç");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Dosyaları (*.xlsx)", "*.xlsx")
            );

            File file = fileChooser.showOpenDialog(stage);
            if (file == null) {
                showAlert(Alert.AlertType.WARNING, "İptal Edildi", "Herhangi bir dosya seçilmedi.");
                return false;
            }

            // 📘 2) Dosyayı oku ve veritabanına kaydet
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis);
                 Connection conn = DBManager.getConnection()) {

                Sheet sheet = workbook.getSheetAt(0); // İlk sayfa
                DataFormatter formatter = new DataFormatter();

                System.out.println("✅ Veritabanına bağlandı. Dersler Excel'den okunuyor...");

                String currentTur = "Zorunlu";
                int currentSinif = 0;
                int bolumId = aktifKullanici.bolumId;

                int eklendi = 0;
                int atlandi = 0;

                // --- DEĞİŞİKLİK BAŞLANGICI: Hata yakalama döngüsü güncellendi ---
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    // Satır numarasını (1-tabanlı) hata mesajları için sakla
                    String satirMesaji = "Hata (Satır " + (i + 1) + "): ";
                    
                    try {
                        Row row = sheet.getRow(i);
                        if (row == null) continue; // Boş satır, atla

                        String kod = getCellString(row.getCell(0), formatter);
                        String ad = getCellString(row.getCell(1), formatter);
                        String ogretmen = getCellString(row.getCell(2), formatter);

                        // Başlık satırlarını atla
                        if (kod.equalsIgnoreCase("DERS KODU") || ad.equalsIgnoreCase("DERSİN ADI"))
                            continue;

                        // 📘 Sınıf satırlarını yakala ("1. Sınıf", "2. Sınıf" vb.)
                        String combined = kod + " " + ad;
                        if (combined.toLowerCase().contains("sınıf")) {
                            try {
                                currentSinif = Integer.parseInt(combined.replaceAll("[^0-9]", ""));
                                System.out.println("📌 Mevcut sınıf: " + currentSinif + ". Sınıf");
                                currentTur = "Zorunlu";
                            } catch (NumberFormatException e) {
                                // HATA: Sınıf başlığı okunamadı
                                showAlert(Alert.AlertType.ERROR, "❌ Geçersiz Sınıf Başlığı", 
                                          satirMesaji + "Sınıf başlığı okunamadı (Örn: '1. Sınıf' bekleniyordu). " +
                                          "Alınan: '" + combined + "'");
                                return false; // İşlemi durdur
                            }
                            continue; // Bu bir başlık satırıydı, sonraki satıra geç
                        }

                        // 📗 Seçmeli bölüm başlığı
                        if (combined.toUpperCase().contains("SEÇMELİ") || combined.toUpperCase().contains("SEÇİMLİK")) {
                            currentTur = "Seçmeli";
                            System.out.println("📌 Ders türü 'Seçmeli' olarak ayarlandı.");
                            continue; // Bu bir başlık satırıydı, sonraki satıra geç
                        }

                        if (kod.isEmpty() || ad.isEmpty()) continue; // Boş veri satırı, atla

                        // --- Artık burası bir DATA satırı ---
                        String tur = currentTur.equalsIgnoreCase("Zorunlu") ? "Zorunlu" : "Seçmeli";

                        String sql = """
                            INSERT IGNORE INTO dersler 
                            (bolum_id, kod, ad, ogretmen, sinif_duzeyi, tur) 
                            VALUES (?, ?, ?, ?, ?, ?)
                        """;

                        // Hata yakalama bloğu artık bu 'try-with-resources' bloğunu kapsamıyor
                        // Hataları dışarıdaki 'catch' yakalayacak
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, bolumId);
                            ps.setString(2, kod);
                            ps.setString(3, ad);
                            ps.setString(4, ogretmen);
                            ps.setInt(5, currentSinif);
                            ps.setString(6, tur);
    
                            int result = ps.executeUpdate();
                            if (result > 0) eklendi++;
                            else atlandi++;
                        }
                        
                    } catch (SQLException sqlEx) {
                        // HATA: Veritabanı hatası (örn: veri çok uzun, sütun adı yanlış vb.)
                        showAlert(Alert.AlertType.ERROR, "❌ Veritabanı Hatası", 
                                  satirMesaji + "Veri kaydedilemedi.\n" +
                                  "Hata: " + sqlEx.getMessage());
                        return false; // İşlemi durdur
                    } catch (Exception ex) {
                        // HATA: Genel satır işleme hatası (örn: beklenmedik veri tipi)
                        showAlert(Alert.AlertType.ERROR, "❌ Satır İşleme Hatası", 
                                  satirMesaji + "Satır işlenirken beklenmedik bir hata oluştu.\n" +
                                  "Hata: " + ex.getMessage());
                        return false; // İşlemi durdur
                    }
                } // 'for' döngüsü biter
                // --- DEĞİŞİKLİK SONU ---

                showAlert(Alert.AlertType.INFORMATION, "✅ Ders Yükleme Tamamlandı",
                        "Toplam " + eklendi + " ders eklendi.\n"
                        + atlandi + " kayıt zaten mevcut olduğu için atlandı.");

                System.out.println("🎉 Ders listesi işleme tamamlandı!");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Bu, dosya açma veya veritabanına bağlanma gibi genel bir hatadır
            showAlert(Alert.AlertType.ERROR, "❌ Hata", "Yükleme başarısız: " + e.getMessage());
            return false;
        }
    }

    // 🔹 Hücre okuma yardımcı metodu
    private static String getCellString(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    // 🔹 Bilgi / uyarı penceresi
    // DEĞİŞİKLİK: Farklı hata türleri (Error, Warning) için Alert tipini parametre aldı
    private static void showAlert(Alert.AlertType alertType, String title, String msg) {
        Alert a = new Alert(alertType);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    
    // Eski showAlert (geriye uyumluluk için, isterseniz silebilirsiniz)
    private static void showAlert(String title, String msg) {
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }
}