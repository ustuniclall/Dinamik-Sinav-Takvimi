import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Objects;

public class AdminEkrani {

    public static HBox ortaAlan; // Admin ana panelindeki varsayılan orta alan

    public static Scene ekraniGetir(Kullanici k, Stage stage) {

        // === ÜST BAR (yeşil şerit) ===
        Label lblBaslik = new Label("🧑‍💼 Admin Paneli");
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Button btnCikisUst = new Button("🚪 Çıkış");
        btnCikisUst.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
        btnCikisUst.setOnAction(e -> new Main().start(stage));

        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(lblBaslik);
        headerPane.setRight(btnCikisUst);
        headerPane.setPadding(new Insets(10, 20, 10, 20));
        headerPane.setStyle("-fx-background-color: #1b8a3f;");

        Label lblHosgeldin = new Label("Hoş geldiniz, " + k.ad + " (ADMIN)");
        lblHosgeldin.setStyle("-fx-font-size: 14px; -fx-padding: 8 0 8 15;");

        VBox topBox = new VBox(headerPane, lblHosgeldin);

        // === SOL MENÜ ===
        Label lblKullaniciYonetimi = new Label("👥 Genel Yönetim");
        lblKullaniciYonetimi.getStyleClass().add("menu-header");

        Button btnKullaniciEkle = new Button("➕ Yeni Koordinatör Ekle");
        Button btnOgrenciEkle = new Button("🎓 Yeni Öğrenci Ekle");
        Button btnKullaniciListele = new Button("📋 Kullanıcıları Görüntüle");
        Button btnDersListele = new Button("📘 Tüm Dersleri Görüntüle");

        Label lblKoordinatorIslemleri = new Label("👨‍🏫 Bölüm Yönetimi");
        lblKoordinatorIslemleri.getStyleClass().add("menu-header");

        ComboBox<String> cmbBolumKoordinator = new ComboBox<>();
        cmbBolumKoordinator.setPromptText("Bölüm Seçin");
        bolumleriComboyaDoldur(cmbBolumKoordinator);

        Button btnKoordinatorPanelineGec = new Button("▶️ Seçili Bölümü Yönet");

        VBox solMenu = new VBox(12,
                lblKullaniciYonetimi,
                btnKullaniciEkle, btnOgrenciEkle, btnKullaniciListele, btnDersListele,
                new Separator(),
                lblKoordinatorIslemleri, cmbBolumKoordinator, btnKoordinatorPanelineGec
        );
        solMenu.setPadding(new Insets(15));
        solMenu.setAlignment(Pos.TOP_LEFT);
        solMenu.setPrefWidth(280);
        solMenu.getStyleClass().add("card");

        ScrollPane solScroll = new ScrollPane(solMenu);
        solScroll.setFitToWidth(true);

        // === ORTA ALAN (varsayılan) ===
        ortaAlan = new HBox(20, createOgrenciAramaPaneli(), createDersAramaPaneli());
        ortaAlan.setPadding(new Insets(15));
        ortaAlan.setAlignment(Pos.TOP_CENTER);

        // === ROOT ===
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setLeft(solScroll);
        root.setCenter(ortaAlan);
        root.setPadding(new Insets(10));

        // === CSS ===
        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(AdminEkrani.class.getResource("/styles/bilsis.css")).toExternalForm()
        );

        // === Buton Aksiyonları ===
        btnKullaniciEkle.setOnAction(e -> root.setCenter(KoordinatorEkleEkrani.getPanel(k, stage, root)));
        btnOgrenciEkle.setOnAction(e -> root.setCenter(OgrenciEkleEkrani.getPanel(k, stage, root)));
        btnKullaniciListele.setOnAction(e -> root.setCenter(KullanicilariGoruntuleEkrani.getPanel(k, stage, root)));
        btnDersListele.setOnAction(e -> root.setCenter(DersleriGoruntuleEkrani.getPanel(k, stage, root)));

        btnKoordinatorPanelineGec.setOnAction(e ->
                handleKoordinatorPaneliGecis(k, stage, root, cmbBolumKoordinator.getValue())
        );

        // === TAM EKRAN ===
        stage.setScene(scene);
        stage.setTitle("Admin Paneli");
        stage.setResizable(true);

        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        stage.setWidth(screenWidth);
        stage.setHeight(screenHeight);
        stage.setX(0);
        stage.setY(0);

        stage.show();

        Platform.runLater(() -> {
            stage.setX(0);
            stage.setY(0);
            stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
            stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        });

        return scene;
    }

    // === Koordinatör paneline geçiş ===
    private static void handleKoordinatorPaneliGecis(Kullanici admin, Stage stage, BorderPane root, String bolumAdi) {
        if (bolumAdi == null || bolumAdi.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Bölüm Seçilmedi", "Lütfen yönetmek için bir bölüm seçiniz.");
            return;
        }

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM bolumler WHERE ad = ?")) {

            ps.setString(1, bolumAdi);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int bolumId = rs.getInt("id");
                Kullanici koordinatorMaskesi = new Kullanici(admin.ad, "KOORDINATOR", bolumId, bolumAdi);

                // === DEĞİŞİKLİK BURADA ===
                // Artık VBox değil, StackPane alıyoruz.
                StackPane koordinatorPaneli = KoordinatorEkrani.getPanelEmbedded(
                        koordinatorMaskesi,
                        stage,
                        () -> root.setCenter(ortaAlan) // Geri dönme eylemi
                );

                // === Koordinatör panelini ortala ve kaydırılabilir hale getir ===
                ScrollPane scroll = new ScrollPane(koordinatorPaneli);
                scroll.setFitToWidth(true);
                //scroll.setFitToHeight(true);
                scroll.setStyle("-fx-background-color: transparent;");

                VBox kapsayici = new VBox(scroll);
                kapsayici.setAlignment(Pos.CENTER);
                kapsayici.setPadding(new Insets(20));
                VBox.setVgrow(scroll, Priority.ALWAYS);

                root.setCenter(kapsayici);

            } else {
                showAlert(Alert.AlertType.ERROR, "Hata", "Seçilen bölüm bulunamadı.");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Veritabanı Hatası", "Bölüm ID alınırken hata oluştu: " + e.getMessage());
        }
    }

    // === Yardımcı paneller ===
    private static VBox createOgrenciAramaPaneli() {
        Label lbl = new Label("👨‍🎓 Öğrenci Numarası ile Dersleri Listele");
        TextField txt = new TextField();
        txt.setPromptText("Örn: 210059017");
        Button btn = new Button("📘 Dersleri Listele");
        ListView<String> list = new ListView<>();

        btn.setOnAction(e -> {
            list.getItems().clear();
            String ogrNo = txt.getText().trim();
            if (ogrNo.isEmpty()) { list.getItems().add("⚠ Öğrenci no giriniz."); return; }
            ogrencininDersleriniYaz(ogrNo, list);
        });

        VBox box = new VBox(10, lbl, txt, btn, list);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(15));
        box.setPrefWidth(400);
        box.getStyleClass().add("card");
        return box;
    }

    private static VBox createDersAramaPaneli() {
        Label lbl = new Label("📖 Ders Seçerek Öğrenci Listesi");
        ComboBox<String> cmb = new ComboBox<>();
        cmb.setPromptText("Ders seçiniz (KOD - AD)");
        dersleriComboyaDoldur(cmb);
        Button btn = new Button("👥 Dersi Alan Öğrencileri Göster");
        ListView<String> list = new ListView<>();

        btn.setOnAction(e -> {
            list.getItems().clear();
            String ders = cmb.getSelectionModel().getSelectedItem();
            if (ders == null) { list.getItems().add("⚠ Ders seçiniz."); return; }
            String dersKod = ders.split(" - ")[0].trim();
            dersinOgrencileriniYaz(dersKod, list);
        });

        VBox box = new VBox(10, lbl, cmb, btn, list);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(15));
        box.setPrefWidth(400);
        box.getStyleClass().add("card");
        return box;
    }

    // === DB yardımcıları ===
    private static void bolumleriComboyaDoldur(ComboBox<String> cmb) {
        cmb.getItems().clear();
        String sql = "SELECT ad FROM bolumler ORDER BY ad";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cmb.getItems().add(rs.getString("ad"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void ogrencininDersleriniYaz(String ogrNo, ListView<String> list) {
        String sql = "SELECT d.kod, d.ad FROM ogrenci_ders od " +
                "JOIN ogrenciler o ON o.id = od.ogrenci_id " +
                "JOIN dersler d ON d.id = od.ders_id WHERE o.ogr_no = ? ORDER BY d.kod";
        try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ogrNo);
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) { any = true; list.getItems().add(rs.getString("kod") + " - " + rs.getString("ad")); }
            if (!any) list.getItems().add("ℹ Bu öğrenci için kayıtlı ders bulunamadı.");
        } catch (Exception ex) { list.getItems().add("❌ Hata: " + ex.getMessage()); }
    }

    private static void dersinOgrencileriniYaz(String dersKod, ListView<String> list) {
        String sql = "SELECT o.ogr_no, o.ad_soyad FROM ogrenci_ders od " +
                "JOIN dersler d ON d.id = od.ders_id " +
                "JOIN ogrenciler o ON o.id = od.ogrenci_id WHERE d.kod = ? ORDER BY o.ad_soyad";
        try (Connection conn = DBManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dersKod);
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) { any = true; list.getItems().add(rs.getString("ogr_no") + " - " + rs.getString("ad_soyad")); }
            if (!any) list.getItems().add("ℹ Bu derse kayıtlı öğrenci bulunamadı.");
        } catch (Exception ex) { list.getItems().add("❌ Hata: " + ex.getMessage()); }
    }

    private static void dersleriComboyaDoldur(ComboBox<String> cmb) {
        cmb.getItems().clear();
        String sql = "SELECT kod, ad FROM dersler ORDER BY kod";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cmb.getItems().add(rs.getString("kod") + " - " + rs.getString("ad"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
