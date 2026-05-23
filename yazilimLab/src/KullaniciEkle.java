import java.sql.*;
import java.util.Scanner;

public class KullaniciEkle {

    public static void main(String[] args) {
        try (Connection conn = DBManager.getConnection();
             Scanner sc = new Scanner(System.in)) {

            System.out.println("🔐 Kullanıcı / Öğrenci Ekleme İşlemi");

            // 🔒 Admin kontrolü
            System.out.print("📧 Giriş yapan e-posta: ");
            String email = sc.nextLine().trim();

            String rolSql = "SELECT rol FROM kullanicilar WHERE email = ?";
            PreparedStatement psRol = conn.prepareStatement(rolSql);
            psRol.setString(1, email);
            ResultSet rsRol = psRol.executeQuery();

            if (!rsRol.next()) {
                System.out.println("❌ Kullanıcı bulunamadı!");
                return;
            }

            String rol = rsRol.getString("rol");
            if (!rol.equals("ADMIN")) {
                System.out.println("⛔ Yetkisiz işlem! Yeni kullanıcı veya öğrenci sadece ADMIN tarafından eklenebilir.");
                return;
            }

            // 👇 Yeni kayıt türü seçimi
            System.out.println("\n1️⃣ Yeni Koordinatör Ekle");
            System.out.println("2️⃣ Yeni Öğrenci Ekle");
            System.out.print("Seçiminiz (1-2): ");
            int secim = sc.nextInt();
            sc.nextLine(); // buffer temizliği

            if (secim == 1) {
                // === KOORDİNATÖR EKLEME ===
                System.out.println("\n🧩 Yeni Koordinatör Bilgileri");
                System.out.print("👤 Ad Soyad: ");
                String ad = sc.nextLine().trim();
                System.out.print("📧 E-posta: ");
                String yeniEmail = sc.nextLine().trim();
                System.out.print("🔑 Şifre: ");
                String sifre = sc.nextLine().trim();
                System.out.print("🏫 Bölüm ID: ");
                int bolumId = sc.nextInt();

                String sql = "INSERT INTO kullanicilar (ad, email, sifre, rol, bolum_id) VALUES (?, ?, ?, 'KOORDINATOR', ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, ad);
                ps.setString(2, yeniEmail);
                ps.setString(3, sifre);
                ps.setInt(4, bolumId);
                ps.executeUpdate();

                System.out.println("✅ Yeni koordinatör başarıyla eklendi!");

            } else if (secim == 2) {
                // === ÖĞRENCİ EKLEME ===
                System.out.println("\n🎓 Yeni Öğrenci Bilgileri");
                System.out.print("👤 Ad Soyad: ");
                String adSoyad = sc.nextLine().trim();
                System.out.print("🎓 Öğrenci Numarası: ");
                String ogrNo = sc.nextLine().trim();
                System.out.print("📧 E-posta: ");
                String ogrEmail = sc.nextLine().trim();
                System.out.print("🔑 Şifre: ");
                String sifre = sc.nextLine().trim();
                System.out.print("🏫 Bölüm ID: ");
                int bolumId = sc.nextInt();

                String sql = "INSERT INTO ogrenciler (ad_soyad, ogr_no, email, sifre, bolum_id) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, adSoyad);
                ps.setString(2, ogrNo);
                ps.setString(3, ogrEmail);
                ps.setString(4, sifre);
                ps.setInt(5, bolumId);
                ps.executeUpdate();

                System.out.println("✅ Yeni öğrenci başarıyla eklendi!");
            } else {
                System.out.println("❌ Geçersiz seçim!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
