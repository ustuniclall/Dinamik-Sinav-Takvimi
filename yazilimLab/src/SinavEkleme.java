
import java.sql.*;
import java.time.*;
import java.util.*;

public class SinavEkleme {

    // Bu metod artık doğrudan giriş yapan kullanıcının e-postasını parametre olarak alıyor
    public static void calistir(String email) {
        try (Connection conn = DBManager.getConnection();
             Scanner sc = new Scanner(System.in)) {

            // 📌 Kullanıcı bilgilerini e-posta üzerinden al
            String userSql = "SELECT rol, bolum_id, ad FROM kullanicilar WHERE email = ?";
            PreparedStatement psUser = conn.prepareStatement(userSql);
            psUser.setString(1, email);
            ResultSet rsUser = psUser.executeQuery();

            if (!rsUser.next()) {
                System.out.println("❌ Kullanıcı bulunamadı!");
                return;
            }

            String rol = rsUser.getString("rol");
            int bolumId = rsUser.getInt("bolum_id");
            String ad = rsUser.getString("ad");

            System.out.println("\n👋 Hoş geldiniz, " + ad + " (" + rol + ")");

            // 🎓 Dersleri listele
            String dersSql = rol.equals("ADMIN")
                    ? "SELECT id, kod, ad FROM dersler"
                    : "SELECT id, kod, ad FROM dersler WHERE bolum_id = ?";
            PreparedStatement psDers = conn.prepareStatement(dersSql);
            if (rol.equals("KOORDINATOR")) psDers.setInt(1, bolumId);
            ResultSet rsDers = psDers.executeQuery();

            List<Integer> tumDersler = new ArrayList<>();
            System.out.println("\n🎓 Tüm dersler:");
            while (rsDers.next()) {
                System.out.println(rsDers.getInt("id") + " - " + rsDers.getString("kod") + " | " + rsDers.getString("ad"));
                tumDersler.add(rsDers.getInt("id"));
            }

            // ❌ Hariç tutulacak dersler
            System.out.print("\n🚫 Programa dahil edilmeyecek ders ID'lerini virgülle girin (boş bırak = hepsi dahil): ");
            String haricStr = sc.nextLine().trim();
            Set<Integer> haricTutulan = new HashSet<>();
            if (!haricStr.isEmpty()) {
                for (String s : haricStr.split(",")) haricTutulan.add(Integer.parseInt(s.trim()));
            }

            List<Integer> dahilDersler = new ArrayList<>();
            for (int id : tumDersler)
                if (!haricTutulan.contains(id))
                    dahilDersler.add(id);

            if (dahilDersler.isEmpty()) {
                System.out.println("❌ Hiç ders seçilmedi!");
                return;
            }

            // 📅 Tarih aralığı seçimi
            System.out.print("\n📆 Başlangıç tarihi (YYYY-MM-DD): ");
            LocalDate baslangicTarihi = LocalDate.parse(sc.nextLine().trim());
            System.out.print("📆 Bitiş tarihi (YYYY-MM-DD): ");
            LocalDate bitisTarihi = LocalDate.parse(sc.nextLine().trim());

            if (bitisTarihi.isBefore(baslangicTarihi)) {
                System.out.println("❌ Bitiş tarihi başlangıç tarihinden önce olamaz!");
                return;
            }

            // 🛑 Hafta sonu hariç tutulacak mı?
            System.out.print("🗓️ Cumartesi/Pazar hariç tutulsun mu? (E/H): ");
            boolean haftaSonuYasak = sc.nextLine().trim().equalsIgnoreCase("E");

            // 📄 Sınav türü
            System.out.print("\n📄 Sınav türü (Vize/Final/Bütünleme): ");
            String tur = sc.nextLine().trim();

            // ⏱ Varsayılan sınav süresi
            int varsayilanSure = 75;
            int sure = varsayilanSure;
            System.out.print("\n⚙️ Süre istisnası var mı? (E/H): ");
            if (sc.nextLine().trim().equalsIgnoreCase("E")) {
                System.out.print("⏱ Yeni sınav süresini girin (dakika): ");
                sure = Integer.parseInt(sc.nextLine().trim());
            } else {
                System.out.println("🔸 Varsayılan sınav süresi (75 dk) kullanılacak.");
            }

            // ⏳ Bekleme süresi — sadece KOORDİNATÖR değiştirebilir
            int bekleme = 15;
            if (rol.equals("KOORDINATOR")) {
                System.out.print("\n⚙️ Bekleme süresi istisnası var mı? (E/H): ");
                if (sc.nextLine().trim().equalsIgnoreCase("E")) {
                    System.out.print("⏳ Yeni bekleme süresini girin (dakika): ");
                    bekleme = Integer.parseInt(sc.nextLine().trim());
                } else {
                    System.out.println("🔸 Varsayılan bekleme süresi (15 dk) kullanılacak.");
                }
            } else {
                System.out.println("\n🔒 Bekleme süresi sadece KOORDİNATÖR tarafından değiştirilebilir. Varsayılan (15 dk) kullanılacak.");
            }

            // 🔒 Aynı anda sınav olmasın mı?
            System.out.print("\n🔒 Aynı anda sınav olmasın mı? (E/H): ");
            boolean cakismaEngel = sc.nextLine().trim().equalsIgnoreCase("E");

            // 🕘 Günün ilk sınavı
            LocalTime baslangicSaati = LocalTime.of(9, 0);
            LocalTime bitisSaati = baslangicSaati.plusMinutes(sure);

            // 📆 Tarih sırası
            LocalDate tarih = baslangicTarihi;

            for (int dersId : dahilDersler) {
                boolean sinavEklendi = false;

                while (!tarih.isAfter(bitisTarihi)) {
                    DayOfWeek gun = tarih.getDayOfWeek();

                    // Hafta sonlarını atla (isteğe bağlı)
                    if (haftaSonuYasak && (gun == DayOfWeek.SATURDAY || gun == DayOfWeek.SUNDAY)) {
                        tarih = tarih.plusDays(1);
                        continue;
                    }

                    // 🧠 Çakışma kontrolü
                    if (cakismaEngel) {
                        String kontrolSql = """
                            SELECT COUNT(*) FROM sinavlar
                            WHERE tarih = ?
                            AND (? < bitis_saati AND ? > baslangic_saati)
                        """;
                        PreparedStatement psKontrol = conn.prepareStatement(kontrolSql);
                        psKontrol.setString(1, tarih.toString());
                        psKontrol.setString(2, baslangicSaati.toString());
                        psKontrol.setString(3, bitisSaati.toString());
                        ResultSet rsKontrol = psKontrol.executeQuery();
                        rsKontrol.next();

                        if (rsKontrol.getInt(1) > 0) {
                            // O gün doluysa sonraki güne geç
                            tarih = tarih.plusDays(1);
                            baslangicSaati = LocalTime.of(9, 0);
                            bitisSaati = baslangicSaati.plusMinutes(sure);
                            continue;
                        }
                    }

                    // ✅ Sınav ekle
                    String insertSql = """
                        INSERT INTO sinavlar (ders_id, sinav_turu, tarih, baslangic_saati, bitis_saati)
                        VALUES (?, ?, ?, ?, ?)
                    """;
                    PreparedStatement psInsert = conn.prepareStatement(insertSql);
                    psInsert.setInt(1, dersId);
                    psInsert.setString(2, tur);
                    psInsert.setString(3, tarih.toString());
                    psInsert.setString(4, baslangicSaati.toString());
                    psInsert.setString(5, bitisSaati.toString());
                    psInsert.executeUpdate();

                    System.out.printf("✅ Ders ID %d için sınav eklendi → %s (%s - %s)%n",
                            dersId, tarih, baslangicSaati, bitisSaati);

                    // Sonraki sınav için saatleri güncelle
                    baslangicSaati = bitisSaati.plusMinutes(bekleme);
                    bitisSaati = baslangicSaati.plusMinutes(sure);

                    // Eğer 17:00 sonrası olduysa ertesi güne geç
                    if (bitisSaati.isAfter(LocalTime.of(17, 0))) {
                        tarih = tarih.plusDays(1);
                        baslangicSaati = LocalTime.of(9, 0);
                        bitisSaati = baslangicSaati.plusMinutes(sure);
                    }

                    sinavEklendi = true;
                    break;
                }

                if (!sinavEklendi) {
                    System.out.printf("⚠️ %d numaralı ders belirtilen tarih aralığında yerleştirilemedi.%n", dersId);
                }
            }

            System.out.println("\n🎉 Tüm sınavlar belirtilen tarih aralığına başarıyla dağıtıldı!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
