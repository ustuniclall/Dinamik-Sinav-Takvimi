import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class OgrenciAramaEkrani {

    public static void show(Stage stage, Kullanici k) {
        Label lblTitle = new Label("🔍 Öğrenci Arama / Aldığı Dersler");
        lblTitle.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        TextField txtOgrNo = new TextField();
        txtOgrNo.setPromptText("Öğrenci numarasını giriniz...");
        Button btnAra = new Button("Ara");
        TextArea txtSonuc = new TextArea();
        txtSonuc.setEditable(false);

        btnAra.setOnAction(e -> ogrencininDersleriniYaz(txtOgrNo.getText().trim(), txtSonuc));

        VBox vbox = new VBox(10, lblTitle, txtOgrNo, btnAra, txtSonuc);
        vbox.setPadding(new Insets(15));
        vbox.setAlignment(Pos.TOP_CENTER);

        Button btnGeri = new Button("⬅ Geri");
        btnGeri.setOnAction(e -> stage.setScene(AdminEkrani.ekraniGetir(k, stage)));

        BorderPane root = new BorderPane();
        root.setCenter(vbox);
        root.setBottom(btnGeri);
        BorderPane.setAlignment(btnGeri, Pos.CENTER);
        BorderPane.setMargin(btnGeri, new Insets(10));

        stage.setScene(new Scene(root, 700, 600));
    }

    private static void ogrencininDersleriniYaz(String ogrNo, TextArea area) {
        if (ogrNo == null || ogrNo.isEmpty()) {
            area.setText("⚠️ Lütfen öğrenci numarası giriniz.");
            return;
        }

        String ogrSql = "SELECT id, ad_soyad FROM ogrenciler WHERE ogr_no = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(ogrSql)) {
            ps.setString(1, ogrNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    area.setText("🚫 Öğrenci bulunamadı.");
                    return;
                }
                int ogrId = rs.getInt("id");
                String adSoyad = rs.getString("ad_soyad");

                area.clear();
                area.appendText("👨‍🎓 Öğrenci: " + adSoyad + " (" + ogrNo + ")\n");
                area.appendText("📚 Aldığı Dersler:\n\n");

                String dersSql =
                    "SELECT d.kod, d.ad " +
                    "FROM ogrenci_ders od " +
                    "JOIN dersler d ON d.id = od.ders_id " +
                    "WHERE od.ogrenci_id = ? " +
                    "ORDER BY d.kod";

                try (PreparedStatement ps2 = conn.prepareStatement(dersSql)) {
                    ps2.setInt(1, ogrId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        boolean var = false;
                        while (rs2.next()) {
                            var = true;
                            area.appendText(" - " + rs2.getString("kod") + " — " + rs2.getString("ad") + "\n");
                        }
                        if (!var) area.appendText(" - Kayıtlı dersi bulunmuyor.\n");
                    }
                }
            }
        } catch (Exception ex) {
            area.setText("❌ Hata: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
