import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class DersAramaEkrani {

    public static void show(Stage stage, Kullanici k) {
        Label lblTitle = new Label("📘 Ders Arama / Öğrenci Listesi");
        lblTitle.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        TextField txtKod = new TextField();
        txtKod.setPromptText("Ders kodunu giriniz (Örn: BLM205)...");
        Button btnAra = new Button("Ara");
        TextArea txtSonuc = new TextArea();
        txtSonuc.setEditable(false);

        btnAra.setOnAction(e -> dersiAlanOgrencileriYaz(txtKod.getText().trim(), txtSonuc));

        VBox vbox = new VBox(10, lblTitle, txtKod, btnAra, txtSonuc);
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

    private static void dersiAlanOgrencileriYaz(String dersKod, TextArea area) {
        if (dersKod == null || dersKod.isEmpty()) {
            area.setText("⚠️ Lütfen ders kodu giriniz.");
            return;
        }
        area.clear();

        try (Connection conn = DBManager.getConnection()) {
            String dersSql = "SELECT id, ad FROM dersler WHERE kod = ?";
            int dersId;
            String dersAd;
            try (PreparedStatement ps = conn.prepareStatement(dersSql)) {
                ps.setString(1, dersKod);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        area.setText("🚫 Ders bulunamadı.");
                        return;
                    }
                    dersId = rs.getInt("id");
                    dersAd = rs.getString("ad");
                }
            }

            area.appendText("📗 " + dersKod + " - " + dersAd + " dersini alan öğrenciler:\n\n");

            String ogrSql =
                "SELECT o.ogr_no, o.ad_soyad " +
                "FROM ogrenci_ders od " +
                "JOIN ogrenciler o ON o.id = od.ogrenci_id " +
                "WHERE od.ders_id = ? " +
                "ORDER BY o.ad_soyad";

            try (PreparedStatement ps2 = conn.prepareStatement(ogrSql)) {
                ps2.setInt(1, dersId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    boolean var = false;
                    while (rs2.next()) {
                        var = true;
                        area.appendText(rs2.getString("ogr_no") + " — " + rs2.getString("ad_soyad") + "\n");
                    }
                    if (!var) area.appendText("❌ Bu derse kayıtlı öğrenci bulunamadı.\n");
                }
            }
        } catch (Exception e) {
            area.setText("❌ Hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
