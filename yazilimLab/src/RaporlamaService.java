import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RaporlamaService {

    private record DerslikInfo(int id, String ad, int kapasite) {}
    
public void programOlusturVeExceleAktar(
        Kullanici aktifKullanici,
        List<Integer> dersIdList,
        LocalDate bas,
        LocalDate bit,
        boolean haftaSonuHaric,
        String tur,
        int beklemeDakika,
        Map<Integer, Integer> dersSureleri,
        File outFile,
        Consumer<String> logger
) throws Exception {

    // --- YENİ KURAL: Bir güne atanacak maksimum sınav sayısı sabit olarak 2'ye ayarlandı ---
    final int MAX_SINAV_PER_GUN = 2;

    logger.accept("▶️ Sınav oluşturma servisi başlatıldı.");
    Connection conn = null;
    try {
        conn = DBManager.getConnection();
        conn.setAutoCommit(false);
        logger.accept("✔️ Veritabanı bağlantısı kuruldu, otomatik commit kapatıldı.");

        // 1. ADIM: ESKİ SINAVLARI SİL
        logger.accept("...'" + aktifKullanici.bolumAdi + "' bölümüne ait mevcut '" + tur + "' türündeki sınav kayıtları siliniyor...");
        String subSelectQuery = "SELECT s.id FROM sinavlar s JOIN dersler d ON s.ders_id = d.id WHERE d.bolum_id = ? AND s.sinav_turu = ?";
        String deleteOturmaPlaniSql = "DELETE FROM oturma_plani WHERE sinav_id IN (" + subSelectQuery + ")";
        String deleteSinavDerslikSql = "DELETE FROM sinav_derslik WHERE sinav_id IN (" + subSelectQuery + ")";
        String deleteSinavlarSql = "DELETE FROM sinavlar WHERE ders_id IN (SELECT id FROM dersler WHERE bolum_id = ?) AND sinav_turu = ?";
        
        try (PreparedStatement psOturma = conn.prepareStatement(deleteOturmaPlaniSql);
             PreparedStatement psDerslik = conn.prepareStatement(deleteSinavDerslikSql);
             PreparedStatement psSinav = conn.prepareStatement(deleteSinavlarSql)) {
            
            psOturma.setInt(1, aktifKullanici.bolumId);
            psOturma.setString(2, tur);
            int oturmaSilindi = psOturma.executeUpdate();

            psDerslik.setInt(1, aktifKullanici.bolumId);
            psDerslik.setString(2, tur);
            int derslikSilindi = psDerslik.executeUpdate();

            psSinav.setInt(1, aktifKullanici.bolumId);
            psSinav.setString(2, tur);
            int sinavSilindi = psSinav.executeUpdate();
            
            logger.accept(String.format("...Temizlendi: %d oturma planı, %d derslik ataması, %d sınav kaydı.", oturmaSilindi, derslikSilindi, sinavSilindi));
        } catch (SQLException e) {
            logger.accept("❌ Eski kayıtları silerken veritabanı hatası: " + e.getMessage());
            conn.rollback();
            throw new Exception("Eski sınav kayıtları silinemedi.", e);
        }
            
        // 2. ADIM: YENİ ALGORİTMA İLE PLAN OLUŞTUR
        logger.accept("🗓️ Sınavlar günlere dengeli şekilde dağıtılıyor...");

        // 2a. Uygun günleri bul
        List<LocalDate> uygunGunler = getUygunGunler(bas, bit, haftaSonuHaric);
        if (uygunGunler.isEmpty()) {
            throw new Exception("Belirtilen tarih aralığında sınav yapılabilecek uygun gün bulunamadı.");
        }
        logger.accept("...Tarih aralığında " + uygunGunler.size() + " uygun gün bulundu.");

        if (dersIdList.size() > uygunGunler.size() * MAX_SINAV_PER_GUN) {
            throw new Exception("Sınav sayısı (" + dersIdList.size() + "), mevcut gün (" + uygunGunler.size() + ") ve günlük sınav limitine ("+MAX_SINAV_PER_GUN+") göre kapasiteyi aşıyor. Lütfen tarih aralığını genişletin.");
        }
        
        // 2b. Sınavları günlere round-robin (dairesel) yöntemiyle dağıt
        Map<LocalDate, List<Integer>> gunlukPlan = new LinkedHashMap<>();
        for (LocalDate gun : uygunGunler) {
            gunlukPlan.put(gun, new ArrayList<>());
        }

        int dersIndex = 0;
        int gunIndex = 0;
        while(dersIndex < dersIdList.size()){
            LocalDate mevcutGun = uygunGunler.get(gunIndex);
            // O güne atanmış sınav sayısı limiti aşmıyorsa, sınavı o güne ekle
            if(gunlukPlan.get(mevcutGun).size() < MAX_SINAV_PER_GUN){
                gunlukPlan.get(mevcutGun).add(dersIdList.get(dersIndex));
                dersIndex++;
            }
            // Bir sonraki güne geç (listenin sonuna gelince başa döner)
            gunIndex = (gunIndex + 1) % uygunGunler.size(); 
        }
        
        logger.accept("...Sınavlar günlere başarıyla dağıtıldı.");

        // 3. ADIM: OLUŞTURULAN PLANI VERİTABANINA KAYDET
        for (Map.Entry<LocalDate, List<Integer>> entry : gunlukPlan.entrySet()) {
            LocalDate tarih = entry.getKey();
            List<Integer> gununDersleri = entry.getValue();

            if (gununDersleri.isEmpty()) continue; 

            LocalTime baslangicSaati = LocalTime.of(9, 0);

            for (int dersId : gununDersleri) {
                String dersAdiVeKodu = getDersAdiVeKodu(conn, dersId);
                logger.accept("--- " + dersAdiVeKodu + " işleniyor [" + tarih + "] ---");

                int sure = dersSureleri.getOrDefault(dersId, 75);

                int ogrenciSayisi = getOgrenciSayisi(conn, dersId);
                if (ogrenciSayisi == 0) {
                    logger.accept("⚠️ Öğrenci bulunamadığı için bu ders atlanıyor.");
                    continue;
                }

                List<DerslikInfo> bolumunDerslikleri = getBolumDerslikleri(conn, dersId);
                if (bolumunDerslikleri.isEmpty()) {
                    logger.accept("❌ HATA: " + dersAdiVeKodu + " dersi için bölüme atanmış hiç derslik bulunamadı! Bu ders atlanıyor.");
                    continue;
                }
                
                List<Integer> atanacakDerslikIdleri = new ArrayList<>();
                List<String> atanacakDerslikAdlari = new ArrayList<>();
                int kalanOgrenci = ogrenciSayisi;

                for(DerslikInfo derslik : bolumunDerslikleri) {
                    if (kalanOgrenci <= 0) break;
                    atanacakDerslikIdleri.add(derslik.id());
                    atanacakDerslikAdlari.add(derslik.ad());
                    kalanOgrenci -= derslik.kapasite();
                }

                if (kalanOgrenci > 0) {
                    logger.accept("❌ HATA: " + dersAdiVeKodu + " dersi için derslikler bulundu ancak kapasite yetersiz! " + kalanOgrenci + " öğrenci açıkta kaldı. Bu ders atlanıyor.");
                    continue;
                }
                
                LocalTime bitisSaati = baslangicSaati.plusMinutes(sure);
                if (bitisSaati.isAfter(LocalTime.of(17, 0))) {
                    logger.accept(String.format("❌ HATA: %s dersi, %s günü saat 17:00'ı aştığı için yerleştirilemedi!", dersAdiVeKodu, tarih));
                    continue;
                }

                long yeniSinavId;
                String insertSql = "INSERT INTO sinavlar (ders_id, sinav_turu, tarih, baslangic_saati, bitis_saati) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, dersId);
                    ps.setString(2, tur);
                    ps.setObject(3, tarih);
                    ps.setObject(4, baslangicSaati);
                    ps.setObject(5, bitisSaati);
                    ps.executeUpdate();
                    ResultSet generatedKeys = ps.getGeneratedKeys();
                    if (generatedKeys.next()) yeniSinavId = generatedKeys.getLong(1);
                    else throw new SQLException("Sınav ID'si oluşturulamadı.");
                }

                for (int derslikId : atanacakDerslikIdleri) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO sinav_derslik (sinav_id, derslik_id) VALUES (?, ?)")) {
                        ps.setLong(1, yeniSinavId);
                        ps.setInt(2, derslikId);
                        ps.executeUpdate();
                    }
                }
                logger.accept(String.format("✅ Başarılı: %s → %s (%s) | Derslikler: %s", dersAdiVeKodu, tarih, baslangicSaati, String.join(", ", atanacakDerslikAdlari)));
                baslangicSaati = bitisSaati.plusMinutes(beklemeDakika);
            }
        }

        conn.commit();
        logger.accept("✔️ COMMIT BAŞARILI.");

    } catch (Exception ex) {
        logger.accept("‼️ KRİTİK HATA OLUŞTU: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        if (conn != null) conn.rollback();
        throw ex;
    } finally {
        if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // 4. ADIM: EXCEL RAPORU OLUŞTUR
    try {
        logger.accept("📄 Excel raporu oluşturuluyor...");
        List<Sinav> olusturulanPlan = veritabanindanSinavlariCek(tur, dersIdList);
        ExcelExporter.exportToExcel(olusturulanPlan, outFile.getAbsolutePath());
        logger.accept("✅ Excel raporu başarıyla kaydedildi: " + outFile.getAbsolutePath());
    } catch (Exception e) {
        logger.accept("❌ Excel raporu oluşturulurken hata: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    // YARDIMCI METOTLAR (DEĞİŞİKLİK YOK)
    private List<LocalDate> getUygunGunler(LocalDate bas, LocalDate bit, boolean haftaSonuHaric) {
        List<LocalDate> gunler = new ArrayList<>();
        LocalDate anlikTarih = bas;
        while (!anlikTarih.isAfter(bit)) {
            if (haftaSonuHaric) {
                DayOfWeek gun = anlikTarih.getDayOfWeek();
                if (gun != DayOfWeek.SATURDAY && gun != DayOfWeek.SUNDAY) {
                    gunler.add(anlikTarih);
                }
            } else {
                gunler.add(anlikTarih);
            }
            anlikTarih = anlikTarih.plusDays(1);
        }
        return gunler;
    }

    private String getDersAdiVeKodu(Connection conn, int dersId) throws SQLException {
        try(PreparedStatement psDers = conn.prepareStatement("SELECT kod, ad FROM dersler WHERE id = ?")) {
            psDers.setInt(1, dersId);
            ResultSet rs = psDers.executeQuery();
            if(rs.next()) {
                return "'" + rs.getString("kod") + " - " + rs.getString("ad") + "'";
            }
        }
        return "Ders ID: " + dersId;
    }

    private int getOgrenciSayisi(Connection conn, int dersId) throws SQLException {
        try (PreparedStatement psOgr = conn.prepareStatement("SELECT COUNT(*) FROM ogrenci_ders WHERE ders_id = ?")) {
            psOgr.setInt(1, dersId);
            ResultSet rsOgr = psOgr.executeQuery();
            if (rsOgr.next()) return rsOgr.getInt(1);
        }
        return 0;
    }

    private List<DerslikInfo> getBolumDerslikleri(Connection conn, int dersId) throws SQLException {
        List<DerslikInfo> bolumunDerslikleri = new ArrayList<>();
        String derslikleriGetirSql = "SELECT d.id, d.derslik_adi, kapasite FROM derslikler d " +
                                      "JOIN bolumler b ON d.bolum_id = b.id " +
                                      "JOIN dersler ders ON b.id = ders.bolum_id " +
                                      "WHERE ders.id = ? ORDER BY kapasite DESC";
        try (PreparedStatement psDerslikler = conn.prepareStatement(derslikleriGetirSql)) {
            psDerslikler.setInt(1, dersId);
            ResultSet rsDerslikler = psDerslikler.executeQuery();
            while (rsDerslikler.next()) {
                bolumunDerslikleri.add(new DerslikInfo(
                    rsDerslikler.getInt("id"),
                    rsDerslikler.getString("derslik_adi"),
                    rsDerslikler.getInt("kapasite")
                ));
            }
        }
        return bolumunDerslikleri;
    }

    private List<Sinav> veritabanindanSinavlariCek(String sinavTuru, List<Integer> dersIdList) throws SQLException {
        List<Sinav> sonuclar = new ArrayList<>();
        if (dersIdList == null || dersIdList.isEmpty()) return sonuclar;
        String placeholders = String.join(",", Collections.nCopies(dersIdList.size(), "?"));
        String sql = "SELECT s.tarih, s.baslangic_saati, d.ad AS ders_adi, d.ogretmen, " +
                     "GROUP_CONCAT(dl.derslik_adi SEPARATOR ', ') AS atanan_derslikler " +
                     "FROM sinavlar s " +
                     "JOIN dersler d ON s.ders_id = d.id " +
                     "LEFT JOIN sinav_derslik sd ON s.id = sd.sinav_id " +
                     "LEFT JOIN derslikler dl ON sd.derslik_id = dl.id " +
                     "WHERE s.sinav_turu = ? AND s.ders_id IN (" + placeholders + ") " +
                     "GROUP BY s.id, s.tarih, s.baslangic_saati, d.ad, d.ogretmen " +
                     "ORDER BY s.tarih, s.baslangic_saati";
        try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sinavTuru);
            for (int i = 0; i < dersIdList.size(); i++) ps.setInt(i + 2, dersIdList.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sonuclar.add(new Sinav(
                        rs.getDate("tarih").toLocalDate(),
                        rs.getTime("baslangic_saati").toLocalTime(),
                        rs.getString("ders_adi"),
                        rs.getString("ogretmen"),
                        rs.getString("atanan_derslikler")
                ));
            }
        }
        return sonuclar;
    }

    public void exportToExcel(Kullanici aktifKullanici, Stage stage) {
                List<Sinav> sinavListesi = new ArrayList<>();
        String sql = "SELECT s.tarih, s.baslangic_saati, d.ad AS ders_adi, d.ogretmen, " +
                     "GROUP_CONCAT(dl.derslik_adi SEPARATOR ', ') AS atanan_derslikler " +
                     "FROM sinavlar s " +
                     "JOIN dersler d ON s.ders_id = d.id " +
                     "LEFT JOIN sinav_derslik sd ON s.id = sd.sinav_id " +
                     "LEFT JOIN derslikler dl ON sd.derslik_id = dl.id " +
                     "WHERE d.bolum_id = ? " +
                     "GROUP BY s.id, s.tarih, s.baslangic_saati, d.ad, d.ogretmen " +
                     "ORDER BY s.tarih, s.baslangic_saati";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, aktifKullanici.bolumId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sinavListesi.add(new Sinav(
                        rs.getDate("tarih").toLocalDate(),
                        rs.getTime("baslangic_saati").toLocalTime(),
                        rs.getString("ders_adi"),
                        rs.getString("ogretmen"),
                        rs.getString("atanan_derslikler")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Raporlama Hatası", "Sınav verileri çekilirken bir hata oluştu: " + e.getMessage());
            return;
        }

        if (sinavListesi.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Bilgi", "Bölümünüze ait raporlanacak bir sınav programı bulunamadı.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sınav Programı Raporunu Kaydet");
        fileChooser.setInitialFileName(aktifKullanici.bolumAdi + "_Sinav_Programi.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Dosyası", "*.xlsx"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                ExcelExporter.exportToExcel(sinavListesi, file.getAbsolutePath());
                showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Sınav programı başarıyla dışa aktarıldı:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Dosya Yazma Hatası", "Rapor dosyası yazılırken bir hata oluştu: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}