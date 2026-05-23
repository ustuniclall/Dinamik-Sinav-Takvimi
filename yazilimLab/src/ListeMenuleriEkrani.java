import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.sql.*;

public class ListeMenuleriEkrani {

    public static void show(Stage stage, String tur, Kullanici aktifKullanici) {

        VBox box = getPanel(tur, aktifKullanici);

        // === ÜST YEŞİL BAR ===
        Label lblBaslik = new Label("📋 Liste Görüntüleme - " + tur.toUpperCase());
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Button btnCikis = new Button("🚪 Çıkış");
        btnCikis.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 8; -fx-padding: 6 15 6 15;");
        btnCikis.setOnMouseEntered(e -> btnCikis.setStyle("-fx-background-color: #c9302c; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15;"));
        btnCikis.setOnMouseExited(e -> btnCikis.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 15 6 15;"));
        btnCikis.setOnAction(e -> {
            try {
                new Main().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox ustBar = new HBox(15, lblBaslik, spacer, btnCikis);
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 15;");

        // === ANA GÖVDE ===
        VBox anaKapsayici = new VBox(15, ustBar, box);
        anaKapsayici.setPadding(new Insets(20));
        anaKapsayici.setStyle("-fx-background-color: #f3fff5;");

        Scene scene = new Scene(anaKapsayici);

        // === ESC TUŞU İLE ÇIKIŞI ENGELLE ===
        scene.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ESCAPE")) {
                event.consume();
            }
        });

        // === TAM EKRAN BENZERİ AYAR ===
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.centerOnScreen();
        stage.setTitle("📋 Liste Görüntüleme - " + tur.toUpperCase());
        stage.show();
    }

    public static VBox getPanel(String tur, Kullanici aktifKullanici) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        if (tur.equalsIgnoreCase("ogrenci")) {
            TableView<OgrenciRow> ogrTable = new TableView<>();

            TableColumn<OgrenciRow, String> cNo = new TableColumn<>("Öğrenci No");
            cNo.setCellValueFactory(p -> p.getValue().ogrNoProperty());

            TableColumn<OgrenciRow, String> cAd = new TableColumn<>("Ad Soyad");
            cAd.setCellValueFactory(p -> p.getValue().adSoyadProperty());

            TableColumn<OgrenciRow, Number> cSinif = new TableColumn<>("Sınıf");
            cSinif.setCellValueFactory(p -> p.getValue().sinifProperty());

            ogrTable.getColumns().addAll(cNo, cAd, cSinif);
            ogrTable.setItems(loadOgrenciler(aktifKullanici));
            ogrTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            ogrTable.setStyle("-fx-border-color: #1b8a3f; -fx-border-width: 2; -fx-background-color: white;");

            Button btnYenile = new Button("↻ Yenile");
            btnYenile.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-background-radius: 8; -fx-padding: 8 15 8 15;");
            btnYenile.setOnMouseEntered(e -> btnYenile.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15 8 15;"));
            btnYenile.setOnMouseExited(e -> btnYenile.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15 8 15;"));
            btnYenile.setOnAction(e -> ogrTable.setItems(loadOgrenciler(aktifKullanici)));

            VBox.setVgrow(ogrTable, Priority.ALWAYS);
            box.getChildren().addAll(ogrTable, btnYenile);
        }

        else if (tur.equalsIgnoreCase("ders")) {
            TableView<DersRow> dersTable = new TableView<>();

            TableColumn<DersRow, String> cKod = new TableColumn<>("Ders Kodu");
            cKod.setCellValueFactory(p -> p.getValue().kodProperty());

            TableColumn<DersRow, String> cAd = new TableColumn<>("Ders Adı");
            cAd.setCellValueFactory(p -> p.getValue().adProperty());

            TableColumn<DersRow, String> cOgretmen = new TableColumn<>("Öğretmen");
            cOgretmen.setCellValueFactory(p -> p.getValue().ogretmenProperty());

            TableColumn<DersRow, String> cTur = new TableColumn<>("Tür");
            cTur.setCellValueFactory(p -> p.getValue().turProperty());

            TableColumn<DersRow, Number> cSinif = new TableColumn<>("Sınıf");
            cSinif.setCellValueFactory(p -> p.getValue().sinifProperty());

            dersTable.getColumns().addAll(cKod, cAd, cOgretmen, cTur, cSinif);
            dersTable.setItems(loadDersler(aktifKullanici));
            dersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            dersTable.setStyle("-fx-border-color: #1b8a3f; -fx-border-width: 2; -fx-background-color: white;");

            Button btnYenile = new Button("↻ Yenile");
            btnYenile.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-background-radius: 8; -fx-padding: 8 15 8 15;");
            btnYenile.setOnMouseEntered(e -> btnYenile.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15 8 15;"));
            btnYenile.setOnMouseExited(e -> btnYenile.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15 8 15;"));
            btnYenile.setOnAction(e -> dersTable.setItems(loadDersler(aktifKullanici)));

            VBox.setVgrow(dersTable, Priority.ALWAYS);
            box.getChildren().addAll(dersTable, btnYenile);
        }

        return box;
    }

    private static ObservableList<OgrenciRow> loadOgrenciler(Kullanici k) {
        ObservableList<OgrenciRow> data = FXCollections.observableArrayList();
        String sql = "SELECT ogr_no, ad_soyad, sinif_duzeyi FROM ogrenciler WHERE bolum_id = ? ORDER BY ogr_no ASC";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, k.bolumId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new OgrenciRow(
                        rs.getString("ogr_no"),
                        rs.getString("ad_soyad"),
                        rs.getInt("sinif_duzeyi")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static ObservableList<DersRow> loadDersler(Kullanici k) {
        ObservableList<DersRow> data = FXCollections.observableArrayList();
        String sql = "SELECT kod, ad, ogretmen, tur, sinif_duzeyi FROM dersler WHERE bolum_id = ? ORDER BY kod ASC";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, k.bolumId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new DersRow(
                        rs.getString("kod"),
                        rs.getString("ad"),
                        rs.getString("ogretmen"),
                        rs.getString("tur"),
                        rs.getInt("sinif_duzeyi")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }
}
