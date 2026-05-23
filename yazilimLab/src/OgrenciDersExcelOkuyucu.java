import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class OgrenciDersExcelOkuyucu {

    public static boolean run(Stage stage, Kullanici aktifKullanici) {
        // 1. ADIM: Kullanıcıdan Excel dosyasını seçmesini iste
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Öğrenci ve Ders Kayıt Listesi Excel Dosyasını Seç");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Dosyaları (*.xlsx)", "*.xlsx"));
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) {
            showAlert(Alert.AlertType.WARNING, "İptal Edildi", "Dosya seçme işlemi iptal edildi.");
            return false;
        }

        int bolumId = aktifKullanici.bolumId;
        int ogrenciEklendi = 0;
        int dersAtandi = 0;
        int currentRowNum = 0; // Hata takibi için satır numarasını tut

        // 2. ADIM: Excel'i oku ve veritabanına işle
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection conn = DBManager.getConnection()) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Önce sadece öğrencileri ekle/güncelle
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                currentRowNum = i + 1; // 1-tabanlı Excel satır numarası
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String ogrNo = getCellString(row.getCell(0), formatter);
                    String adSoyad = getCellString(row.getCell(1), formatter);
                    if (ogrNo.isEmpty() || adSoyad.isEmpty()) continue;
                    
                    String sinifStr = getCellString(row.getCell(2), formatter);
                    int sinif = 1; // Varsayılan
                    try { 
                        if (sinifStr != null && !sinifStr.isEmpty()) {
                            sinif = Integer.parseInt(sinifStr.replaceAll("[^0-9]","")); 
                        }
                    } catch(NumberFormatException e){
                        // HATA: Sınıf bilgisi geçersiz
                         showAlert(Alert.AlertType.ERROR, "❌ Geçersiz Veri Tipi", 
                              "Hata (Satır " + currentRowNum + "): 'Sınıf' bilgisi ('" + sinifStr + "') sayıya dönüştürülemedi.");
                        return false; // İşlemi durdur
                    }

                    String sqlOgrenci = "INSERT INTO ogrenciler (ogr_no, ad_soyad, sinif_duzeyi, bolum_id) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ad_soyad=VALUES(ad_soyad), sinif_duzeyi=VALUES(sinif_duzeyi)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlOgrenci)) {
                        ps.setString(1, ogrNo);
                        ps.setString(2, adSoyad);
                        ps.setInt(3, sinif);
                        ps.setInt(4, bolumId);
                        ps.executeUpdate();
                        ogrenciEklendi++;
                    }
                } catch (SQLException sqlEx) {
                    showAlert(Alert.AlertType.ERROR, "❌ Veritabanı Hatası (Öğrenci)", 
                              "Hata (Satır " + currentRowNum + "): Öğrenci kaydedilemedi.\n" +
                              "Hata: " + sqlEx.getMessage());
                    return false;
                } catch (Exception ex) {
                     showAlert(Alert.AlertType.ERROR, "❌ Satır İşleme Hatası (Öğrenci)", 
                              "Hata (Satır " + currentRowNum + "): Beklenmedik bir hata oluştu.\n" +
                              "Hata: " + ex.getMessage());
                    return false;
                }
            } // İlk döngü bitti
            
            // Şimdi öğrenci-ders ilişkilerini kur
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                currentRowNum = i + 1;
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                String ogrNo = ""; // Hata mesajı için
                try {
                    ogrNo = getCellString(row.getCell(0), formatter);
                    if (ogrNo.isEmpty()) continue;

                    int ogrId = getId(conn, "SELECT id FROM ogrenciler WHERE ogr_no = ? AND bolum_id = ?", ogrNo, String.valueOf(bolumId));
                    if (ogrId == -1) {
                         System.err.println("Uyarı (Satır " + currentRowNum + "): Öğrenci '" + ogrNo + "' bulunamadı, dersleri atlanıyor.");
                         continue; // Öğrenci bulunamadıysa bu satırı atla
                    }

                    // 3. sütundan sonraki tüm sütunları ders kodu olarak kabul et
                    for (int c = 3; c < row.getLastCellNum(); c++) {
                        String dersKod = ""; // Hata mesajı için
                        try {
                            dersKod = getCellString(row.getCell(c), formatter).replaceAll("\\s+", "").toUpperCase();
                            if (dersKod.isEmpty()) continue;

                            int dersId = getId(conn, "SELECT id FROM dersler WHERE UPPER(REPLACE(kod, ' ', '')) = ? AND bolum_id = ?", dersKod, String.valueOf(bolumId));
                            
                            if (dersId != -1) {
                                String sqlIliski = "INSERT IGNORE INTO ogrenci_ders (ogrenci_id, ders_id) VALUES (?, ?)";
                                try (PreparedStatement ps = conn.prepareStatement(sqlIliski)) {
                                    ps.setInt(1, ogrId);
                                    ps.setInt(2, dersId);
                                    if(ps.executeUpdate() > 0) dersAtandi++;
                                }
                            } else {
                                // HATA: Ders kodu bulunamadı
                                showAlert(Alert.AlertType.ERROR, "❌ Geçersiz Ders Kodu", 
                                      "Hata (Satır " + currentRowNum + "): Öğrenci '" + ogrNo + "' için '" + dersKod + "' kodlu ders veritabanında bulunamadı.\n" +
                                      "Lütfen önce Ders Listesi'ni yüklediğinizden emin olun.");
                                return false; // İşlemi durdur
                            }
                        } catch (SQLException sqlEx_inner) {
                             showAlert(Alert.AlertType.ERROR, "❌ Veritabanı Hatası (Ders Atama)", 
                              "Hata (Satır " + currentRowNum + "): Öğrenci '" + ogrNo + "', Ders '" + dersKod + "' atanırken hata.\n" +
                              "Hata: " + sqlEx_inner.getMessage());
                            return false;
                        }
                    } // Hücre (ders) döngüsü bitti
                } catch (Exception ex) {
                     showAlert(Alert.AlertType.ERROR, "❌ Satır İşleme Hatası (Ders Atama)", 
                              "Hata (Satır " + currentRowNum + "): Beklenmedik bir hata oluştu.\n" +
                              "Hata: " + ex.getMessage());
                    return false;
                }
            } // İkinci döngü bitti

            showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı",
                    ogrenciEklendi + " öğrenci kaydı işlendi.\n" +
                    dersAtandi + " yeni ders-öğrenci ataması yapıldı.");
            return true;

        } catch (Exception e) { // Bu, dosya açma veya veritabanına bağlanma gibi genel bir hatadır
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Hata", "Excel dosyası işlenirken bir hata oluştu: " + e.getMessage());
            return false;
        }
    }

    private static int getId(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static String getCellString(Cell cell, DataFormatter f) {
        return cell == null ? "" : f.formatCellValue(cell).trim();
    }

    private static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}