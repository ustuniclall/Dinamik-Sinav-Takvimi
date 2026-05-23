import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class KoordinatorEkleEkrani {

    public static VBox getPanel(Kullanici admin, Stage stage, BorderPane root) {
        Label lblBaslik = new Label("👨‍🏫 Yeni Koordinatör Ekle");
        lblBaslik.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField txtAdSoyad = new TextField();  txtAdSoyad.setPromptText("Ad Soyad");
        TextField txtEmail   = new TextField();  txtEmail.setPromptText("E-posta");
        PasswordField txtSifre = new PasswordField(); txtSifre.setPromptText("Şifre");

        ComboBox<String> cmbBolum = new ComboBox<>();
        cmbBolum.setPromptText("Bölüm Seçiniz");
        bolumleriYukle(cmbBolum);

        Button btnKaydet = new Button("💾 Kaydet");
        btnKaydet.setStyle("-fx-background-color:#5cb85c; -fx-text-fill:white;");
        btnKaydet.setOnAction(e -> {
            String ad = txtAdSoyad.getText().trim();
            String email = txtEmail.getText().trim();
            String sifre = txtSifre.getText().trim();
            String bolumAdi = cmbBolum.getValue();

            if (ad.isEmpty() || email.isEmpty() || sifre.isEmpty() || bolumAdi == null) {
                show("⚠️ Eksik Bilgi","Lütfen tüm alanları doldurun!"); return;
            }
            try (Connection conn = DBManager.getConnection()) {
                int bolumId = lookupId(conn, "SELECT id FROM bolumler WHERE ad = ?", bolumAdi);
                if (bolumId == -1) { show("Hata","Bölüm bulunamadı."); return; }

                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO kullanicilar (ad,email,sifre,rol,bolum_id) VALUES (?,?,?,'KOORDINATOR',?)");
                ps.setString(1, ad); ps.setString(2, email); ps.setString(3, sifre); ps.setInt(4, bolumId);
                ps.executeUpdate();

                show("✅ Başarılı","Koordinatör eklendi.");
                txtAdSoyad.clear(); txtEmail.clear(); txtSifre.clear(); cmbBolum.setValue(null);
            } catch (Exception ex) { show("❌ Hata", ex.getMessage()); ex.printStackTrace(); }
        });

        Button btnGeri = new Button("⬅️ Geri");
        btnGeri.setOnAction(e -> root.setCenter(AdminEkrani.ortaAlan));

        VBox form = new VBox(10, lblBaslik, txtAdSoyad, txtEmail, txtSifre, cmbBolum, btnKaydet, btnGeri);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);
        form.setStyle("-fx-background-color:#fafafa; -fx-border-color:#ccc; -fx-border-radius:10;");
        form.setPrefWidth(500);
        return form;
    }

    private static void bolumleriYukle(ComboBox<String> combo) {
        try (Connection c = DBManager.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT ad FROM bolumler ORDER BY ad")) {
            while (rs.next()) combo.getItems().add(rs.getString("ad"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static int lookupId(Connection c, String sql, String val) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, val);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    private static void show(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
        }
}
