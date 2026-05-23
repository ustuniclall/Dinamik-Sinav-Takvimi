import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SinavProgramiOlustur {

    public static void olusturProgram(Connection conn,
                                      List<Integer> dahilDersler,
                                      LocalDate baslangicTarihi,
                                      LocalDate bitisTarihi,
                                      boolean haftaSonuYasak,
                                      String tur, // "Vize", "Final" etc.
                                      int bekleme,
                                      Map<Integer, Integer> dersSureleri,
                                      Consumer<String> logger) throws SQLException {

        conn.setAutoCommit(false);
        try {
            // ----- YENİ EKLENEN VE EN ÖNEMLİ KISIM BAŞLANGICI -----
            // Yeni sınavları eklemeden ÖNCE, aynı türe ait eski sınavları veritabanından temizle.
            // Bu, her seferinde temiz bir başlangıç yapmamızı sağlar.
            String deleteSql = "DELETE FROM sinavlar WHERE sinav_turu = ?";
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                psDelete.setString(1, tur);
                int silinenSatirlar = psDelete.executeUpdate();
                if (silinenSatirlar > 0) {
                    logger.accept("🧹 Önceki '" + tur + "' türündeki " + silinenSatirlar + " sınav kaydı temizlendi.");
                }
            }
            // ----- YENİ EKLENEN KISIM SONU -----


            LocalDate tarih = baslangicTarihi;
            LocalTime baslangicSaati = LocalTime.of(9, 0);

            for (int dersId : dahilDersler) {
                int sure = dersSureleri.getOrDefault(dersId, 75);
                boolean sinavEklendi = false;

                String dersKodu = "";
                String dersAdi = "";
                try (PreparedStatement p = conn.prepareStatement("SELECT kod, ad FROM dersler WHERE id = ?")) {
                    p.setInt(1, dersId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            dersKodu = r.getString("kod");
                            dersAdi = r.getString("ad");
                        }
                    }
                }

                while (!tarih.isAfter(bitisTarihi)) {
                    DayOfWeek gun = tarih.getDayOfWeek();

                    if (haftaSonuYasak && (gun == DayOfWeek.SATURDAY || gun == DayOfWeek.SUNDAY)) {
                        tarih = tarih.plusDays(1);
                        baslangicSaati = LocalTime.of(9, 0);
                        continue;
                    }

                    LocalTime bitisSaati = baslangicSaati.plusMinutes(sure);

                    if (bitisSaati.isAfter(LocalTime.of(17, 0))) {
                        tarih = tarih.plusDays(1);
                        baslangicSaati = LocalTime.of(9, 0);
                        continue;
                    }

                    String insertSql = "INSERT INTO sinavlar (ders_id, sinav_turu, tarih, baslangic_saati, bitis_saati) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setInt(1, dersId);
                        ps.setString(2, tur);
                        ps.setObject(3, tarih); // setString yerine setObject kullanmak daha güvenlidir
                        ps.setObject(4, baslangicSaati);
                        ps.setObject(5, bitisSaati);
                        ps.executeUpdate();
                    }

                    logger.accept(String.format("✅ %s - %s eklendi → %s (%s - %s)",
                            dersKodu.isEmpty() ? ("DersID " + dersId) : dersKodu,
                            dersAdi,
                            tarih, baslangicSaati, bitisSaati));

                    baslangicSaati = bitisSaati.plusMinutes(bekleme);

                    if (baslangicSaati.isAfter(LocalTime.of(16, 59))) {
                        tarih = tarih.plusDays(1);
                        baslangicSaati = LocalTime.of(9, 0);
                    }

                    sinavEklendi = true;
                    break;
                }

                if (!sinavEklendi) {
                    logger.accept(String.format("⚠️ %s - %s yerleştirilemedi.", dersKodu, dersAdi));
                }
            }

            conn.commit();
            logger.accept("🎉 Sınav programı başarıyla oluşturuldu ve veritabanına kaydedildi.");
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}