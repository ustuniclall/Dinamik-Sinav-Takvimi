
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginTest {
    public static void main(String[] args) {
        // Test için sabit değerler
        String email = "admin@uni.com";
        String sifre = "1234";

        try (Connection conn = DBManager.getConnection()) {
            String sql = "SELECT * FROM kullanicilar WHERE email=? AND sifre=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, sifre);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("✅ Giriş başarılı! Hoşgeldin " 
                                   + rs.getString("ad") 
                                   + " | Rol: " + rs.getString("rol"));
            } else {
                System.out.println("❌ Hatalı giriş!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
