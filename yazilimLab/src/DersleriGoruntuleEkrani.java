import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class DersleriGoruntuleEkrani {

    public static VBox getPanel(Kullanici admin, Stage stage, BorderPane root) {
        Label lblBaslik = new Label("📘 Dersleri Görüntüle / Düzenle");
        lblBaslik.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        ComboBox<String> cmbBolum = new ComboBox<>();
        cmbBolum.setPromptText("📚 Bölüm Seçiniz");
        bolumleriYukle(cmbBolum);

        TableView<DersSatir> tablo = new TableView<>();
        tablo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DersSatir, String> colKod = new TableColumn<>("Ders Kodu");
        colKod.setCellValueFactory(data -> data.getValue().kodProperty());

        TableColumn<DersSatir, String> colAd = new TableColumn<>("Ders Adı");
        colAd.setCellValueFactory(data -> data.getValue().adProperty());

        TableColumn<DersSatir, String> colBolum = new TableColumn<>("Bölüm");
        colBolum.setCellValueFactory(data -> data.getValue().bolumProperty());

        tablo.getColumns().addAll(colKod, colAd, colBolum);

        // 🔴 KIRMIZI "Listeyi Yenile" Butonu
        Button btnYenile = new Button("🔄 Listeyi Yenile");
        btnYenile.setStyle("-fx-background-color:#d9534f; -fx-text-fill:white; -fx-font-weight:bold;");
        btnYenile.setOnMouseEntered(e -> btnYenile.setStyle("-fx-background-color:#b52b27; -fx-text-fill:white; -fx-font-weight:bold;"));
        btnYenile.setOnMouseExited(e -> btnYenile.setStyle("-fx-background-color:#d9534f; -fx-text-fill:white; -fx-font-weight:bold;"));

        btnYenile.setOnAction(e -> {
            String secilen = cmbBolum.getValue();
            tablo.setItems(dersleriYukle(secilen));
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

    // 🔹 Bölümleri comboya yükle
    private static void bolumleriYukle(ComboBox<String> combo) {
        try (Connection conn = DBManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT ad FROM bolumler ORDER BY ad")) {
            while (rs.next()) combo.getItems().add(rs.getString("ad"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Dersleri veritabanından yükle
    private static ObservableList<DersSatir> dersleriYukle(String bolumAdi) {
        ObservableList<DersSatir> liste = FXCollections.observableArrayList();
        String sql = """
            SELECT d.kod, d.ad, b.ad AS bolum
            FROM dersler d
            LEFT JOIN bolumler b ON d.bolum_id = b.id
        """;

        if (bolumAdi != null && !bolumAdi.isBlank()) {
            sql += " WHERE b.ad = ?";
        }

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (bolumAdi != null && !bolumAdi.isBlank())
                ps.setString(1, bolumAdi);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(new DersSatir(
                        rs.getString("kod"),
                        rs.getString("ad"),
                        rs.getString("bolum")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return liste;
    }
}
