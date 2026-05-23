import java.sql.*;
import java.util.Scanner;
import javafx.stage.Stage;

public class GirisSistemi {

    void start(Stage stage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 🔹 Giriş yapan kullanıcının bilgilerini tutan küçük model
    public static class AktifKullanici {
        public static int id;
        public static String ad;
        public static String rol;
        public static Integer bolumId;
        public static String bolumAdi; // 🔹 eklendi
        public static String email;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== 🏫 Sınav Takvimi Sistemi Giriş Ekranı ===");
        System.out.print("📧 E-posta: ");
        String email = sc.nextLine().trim();
        System.out.print("🔑 Şifre: ");
        String sifre = sc.nextLine().trim();

        try (Connection conn = DBManager.getConnection()) {

            // ✅ Bölüm adını da çekecek şekilde JOIN yapıldı
            String sql = """
                SELECT k.id, k.ad, k.rol, k.bolum_id, k.email, b.ad AS bolum_adi
                FROM kullanicilar k
                LEFT JOIN bolumler b ON k.bolum_id = b.id
                WHERE k.email = ? AND k.sifre = ?
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, sifre);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // 🔹 Giriş başarılı → kullanıcıyı kaydet
                AktifKullanici.id = rs.getInt("id");
                AktifKullanici.ad = rs.getString("ad");
                AktifKullanici.rol = rs.getString("rol");
                AktifKullanici.bolumId = rs.getInt("bolum_id");
                AktifKullanici.email = rs.getString("email");
                AktifKullanici.bolumAdi = rs.getString("bolum_adi"); // 🔹 eklendi

                System.out.println("\n👋 Hoş geldiniz, " + AktifKullanici.ad +
                        " (" + AktifKullanici.rol +
                        (AktifKullanici.bolumAdi != null ? " - " + AktifKullanici.bolumAdi + ")" : ")"));

                // 🔹 Rol bazlı yönlendirme
                if (AktifKullanici.rol.equals("ADMIN")) {
                    adminPanel(conn);
                } else if (AktifKullanici.rol.equals("KOORDINATOR")) {
                    koordinatorPanel(conn);
                } else {
                    System.out.println("❌ Bilinmeyen rol tipi!");
                }
            } else {
                System.out.println("❌ Geçersiz e-posta veya şifre!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🧑‍💼 Admin menüsü
    private static void adminPanel(Connection conn) throws SQLException {
        Scanner sc = new Scanner(System.in);
        int secim;
        do {
            System.out.println("\n🧑‍💼 [Admin Paneli]");
            System.out.println("1 - Tüm bölümleri listele");
            System.out.println("2 - Sınav Ekle (Tüm dersler için)");
            System.out.println("3 - Çıkış");
            System.out.print("Seçiminiz: ");
            secim = sc.nextInt();

            switch (secim) {
                case 1 -> {
                    ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM bolumler");
                    System.out.println("\n📚 Bölümler:");
                    while (rs.next()) {
                        System.out.println("• " + rs.getInt("id") + " - " + rs.getString("ad"));
                    }
                }
                case 2 -> {
                    System.out.println("\n📘 Sınav ekleme ekranı açılıyor...");
                    SinavEkleme.calistir(AktifKullanici.email);
                }
                case 3 -> System.out.println("🔚 Çıkış yapıldı.");
                default -> System.out.println("❌ Geçersiz seçim!");
            }
        } while (secim != 3);
    }

    // 👨‍🏫 Koordinatör menüsü
    private static void koordinatorPanel(Connection conn) throws SQLException {
        Scanner sc = new Scanner(System.in);
        int secim;
        do {
            System.out.println("\n👨‍🏫 [Koordinatör Paneli] (" + AktifKullanici.bolumAdi + ")");
            System.out.println("1 - Dersleri Listele");
            System.out.println("2 - Sınav Ekle");
            System.out.println("3 - Çıkış");
            System.out.print("Seçiminiz: ");
            secim = sc.nextInt();

            switch (secim) {
                case 1 -> {
                    String sql = "SELECT kod, ad FROM dersler WHERE bolum_id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, AktifKullanici.bolumId);
                    ResultSet rs = ps.executeQuery();
                    System.out.println("\n📚 Dersler:");
                    while (rs.next()) {
                        System.out.println("• " + rs.getString("kod") + " - " + rs.getString("ad"));
                    }
                }
                case 2 -> {
                    System.out.println("\n📘 Sınav ekleme ekranı açılıyor...");
                    SinavEkleme.calistir(AktifKullanici.email);
                }
                case 3 -> System.out.println("🔚 Çıkış yapıldı.");
                default -> System.out.println("❌ Geçersiz seçim!");
            }
        } while (secim != 3);
    }
}
