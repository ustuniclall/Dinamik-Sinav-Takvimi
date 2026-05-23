import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class KullanicilariGoruntuleEkrani {

    public static VBox getPanel(Kullanici admin, Stage stage, BorderPane root) {
        Label lblBaslik = new Label("📋 Kullanıcıları Görüntüle / Düzenle");
        lblBaslik.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        ComboBox<String> cmbBolum = new ComboBox<>();
        cmbBolum.setPromptText("📚 Bölüm Seçiniz");
        bolumleriYukle(cmbBolum);

        TableView<KullaniciSatir> tablo = new TableView<>();
        tablo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<KullaniciSatir, String> colAd = new TableColumn<>("Ad Soyad");
        colAd.setCellValueFactory(data -> data.getValue().adProperty());

        TableColumn<KullaniciSatir, String> colEmail = new TableColumn<>("E-posta");
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());

        TableColumn<KullaniciSatir, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(data -> data.getValue().rolProperty());

        TableColumn<KullaniciSatir, String> colBolum = new TableColumn<>("Bölüm");
        colBolum.setCellValueFactory(data -> data.getValue().bolumProperty());

        tablo.getColumns().addAll(colAd, colEmail, colRol, colBolum);

        // 🔴 LİSTEYİ YENİLE BUTONU (KIRMIZI)
        Button btnYenile = new Button("🔄 Listeyi Yenile");
        btnYenile.setStyle("-fx-background-color:#d9534f; -fx-text-fill:white; -fx-font-weight:bold;");
        btnYenile.setOnMouseEntered(e -> btnYenile.setStyle("-fx-background-color:#b52b27; -fx-text-fill:white; -fx-font-weight:bold;"));
        btnYenile.setOnMouseExited(e -> btnYenile.setStyle("-fx-background-color:#d9534f; -fx-text-fill:white; -fx-font-weight:bold;"));

        btnYenile.setOnAction(e -> {
            String secilen = cmbBolum.getValue();
            tablo.setItems(veritabanindanKullanicilariVeOgrencileriYukle(secilen));
        });

        // 🔙 Geri Dön Butonu
        Button btnGeri = new Button("⬅️ Geri Dön");
        btnGeri.setOnAction(e -> root.setCenter(AdminEkrani.ekraniGetir(admin, stage).getRoot()));

        VBox form = new VBox(15, lblBaslik, cmbBolum, btnYenile, tablo, btnGeri);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);
        form.setStyle("-fx-background-color:#fafafa; -fx-border-color:#ccc; -fx-border-radius:10;");

        return form;
    }

    private static void bolumleriYukle(ComboBox<String> combo) {
        try (Connection conn = DBManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT ad FROM bolumler ORDER BY ad")) {
            while (rs.next()) combo.getItems().add(rs.getString("ad"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ObservableList<KullaniciSatir> veritabanindanKullanicilariVeOgrencileriYukle(String bolumAdi) {
        ObservableList<KullaniciSatir> liste = FXCollections.observableArrayList();

        String sql = """
            SELECT k.ad AS ad, k.email, k.rol, b.ad AS bolum
            FROM kullanicilar k
            LEFT JOIN bolumler b ON k.bolum_id = b.id
            UNION ALL
            SELECT o.ad_soyad AS ad, o.email, 'OGRENCI' AS rol, b.ad AS bolum
            FROM ogrenciler o
            LEFT JOIN bolumler b ON o.bolum_id = b.id
        """;

        if (bolumAdi != null && !bolumAdi.isBlank()) {
            sql = """
                SELECT * FROM (
                    SELECT k.ad AS ad, k.email, k.rol, b.ad AS bolum
                    FROM kullanicilar k
                    LEFT JOIN bolumler b ON k.bolum_id = b.id
                    UNION ALL
                    SELECT o.ad_soyad AS ad, o.email, 'OGRENCI' AS rol, b.ad AS bolum
                    FROM ogrenciler o
                    LEFT JOIN bolumler b ON o.bolum_id = b.id
                ) AS tum
                WHERE bolum = ?
            """;
        }

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (bolumAdi != null && !bolumAdi.isBlank())
                ps.setString(1, bolumAdi);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(new KullaniciSatir(
                        rs.getString("ad"),
                        rs.getString("email"),
                        rs.getString("rol"),
                        rs.getString("bolum")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }
}
