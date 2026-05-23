import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Objects;
import javafx.stage.Screen;

public class KoordinatorEkrani {

    private static Button listeBtn;
    private static Button programBtn; // YENİ
    private static Button oturmaBtn;  // YENİ
    private static VBox menuBox;

    public static Scene ekraniGetir(Kullanici k, Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #e8f5e9;");

        // 'getPanelEmbedded' artık bir StackPane döndürüyor
        StackPane panel = getPanelEmbedded(k, stage, null);
        root.setCenter(panel);

        Scene scene = new Scene(root, 1200, 700);
        // ... (css ve tam ekran kodları aynı) ...
        scene.getStylesheets().add(
                Objects.requireNonNull(KoordinatorEkrani.class.getResource("/styles/bilsis.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.setTitle("Koordinatör Paneli - " + k.bolumAdi);
        javafx.geometry.Rectangle2D ekran = Screen.getPrimary().getVisualBounds();
        stage.setX(ekran.getMinX()); stage.setY(ekran.getMinY());
        stage.setWidth(ekran.getWidth()); stage.setHeight(ekran.getHeight());
        stage.show();

        return scene;
    }

    // === EMBEDDED (Admin içi) ===
    // DEĞİŞİKLİK: Artık VBox değil, StackPane döndürüyor
    public static StackPane getPanelEmbedded(Kullanici k, Stage stage, Runnable onBack) {
        // 1. Ana menü VBox'ını oluştur
        menuBox = buildMenuBox(k, stage, onBack);

        // 2. Navigasyon için bir StackPane oluştur
        StackPane stackContainer = new StackPane();
        stackContainer.getChildren().add(menuBox); // Ana menüyü ekle

        // 3. Stilleri ayarla
        stackContainer.setPadding(new Insets(30));
        stackContainer.setAlignment(Pos.CENTER);
        stackContainer.setStyle("-fx-background-color: #e8f5e9;");
        return stackContainer;
    }

    // === Menü Yapısı ===
    private static VBox buildMenuBox(Kullanici k, Stage stage, Runnable onBack) {

        // ... (Üst Bar ve Geri Butonu kodları aynı, line 58-92 arası) ...
        // 'btnGeri.setOnAction' mantığı zaten 'onBack' sayesinde doğru çalışıyor.
        // (Dosyanızdaki bu kısım zaten doğruydu)
        HBox ustBar = new HBox(15);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 20;");
        ustBar.setAlignment(Pos.CENTER_LEFT);
        Button btnGeri = new Button("⬅ Geri");
        btnGeri.setStyle("-fx-background-color: white; -fx-text-fill: #1b8a3f; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnGeri.setOnMouseEntered(e -> btnGeri.setStyle("-fx-background-color: #e0f2e9; -fx-text-fill: #166e33; -fx-font-weight: bold; -fx-background-radius: 8;"));
        btnGeri.setOnMouseExited(e -> btnGeri.setStyle("-fx-background-color: white; -fx-text-fill: #1b8a3f; -fx-font-weight: bold; -fx-background-radius: 8;"));
        btnGeri.setOnAction(e -> {
            if (onBack != null) {
                onBack.run(); // Admin paneline geri döner
            } else {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Çıkış Onayı");
                    alert.setHeaderText("Oturumu kapatmak istiyor musunuz?");
                    alert.setContentText("Evet'e basarsanız giriş ekranına dönülür.");
                    if (alert.showAndWait().get() == ButtonType.OK) new Main().start(stage);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        Label lblBaslik = new Label("🧑‍🏫 Koordinatör Ekranı");
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        ustBar.getChildren().addAll(btnGeri, lblBaslik);
        Label lblAltBaslik = new Label("Sınav Takvimi Yönetim Paneli");
        lblAltBaslik.setStyle("-fx-text-fill: #333; -fx-font-size: 15px; -fx-padding: 10 0 15 0;");
        lblAltBaslik.setAlignment(Pos.CENTER);
        Label lblWelcome = new Label("👋 Hoş geldiniz, " + k.ad +
                ((k.bolumAdi != null && !k.bolumAdi.isBlank()) ? " (" + k.bolumAdi + " Koordinatörü)" : ""));
        lblWelcome.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1b8a3f;");
        lblWelcome.setAlignment(Pos.CENTER);
        
        // ... (Butonlar ve stilleri aynı, line 108-134 arası) ...
        Button derslikBtn = new Button("🏫 Derslik Yönetimi");
        Button dersExcelBtn = new Button("📘 Ders Listesi Yükle");
        Button ogrExcelBtn = new Button("🧾 Öğrenci Listesi Yükle");    
        listeBtn = new Button("📋 Listeleri Görüntüle");
        programBtn = new Button("🗓 Sınav Programı Oluştur"); // "Button" kelimesi kaldırıldı
        oturmaBtn = new Button("🪑 Oturma Planı Görüntüle");
        String yesilButon = "-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 0 10 0;";
        for (Button b : new Button[]{derslikBtn, dersExcelBtn, ogrExcelBtn, listeBtn, programBtn, oturmaBtn}) {
            b.setStyle(yesilButon); b.setPrefWidth(260); b.setPrefHeight(40);
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10;"));
            b.setOnMouseExited(e -> b.setStyle(yesilButon));
        }


        // === AKSİYONLAR (DEĞİŞTİ) ===

        dersExcelBtn.setOnAction(e -> { try { if (DersExcelOkuyucu.run(stage, k)) butonDurumlariniGuncelle(k); } catch (Exception ex){ ex.printStackTrace(); }});
        ogrExcelBtn.setOnAction(e -> { try { if (OgrenciDersExcelOkuyucu.run(stage, k)) butonDurumlariniGuncelle(k); } catch (Exception ex){ ex.printStackTrace(); }});        
        listeBtn.setDisable(true);
        // DEĞİŞİKLİK: 'listeBtn' aksiyonu artık panel içinde kalacak
        listeBtn.setOnAction(e -> {
            TabPane tabPane = new TabPane();
            Tab tabOgrenci = new Tab("🎓 Öğrenci Listesi"); tabOgrenci.setContent(ListeMenuleriEkrani.getPanel("ogrenci", k)); tabOgrenci.setClosable(false);
            Tab tabDers = new Tab("📚 Ders Listesi"); tabDers.setContent(ListeMenuleriEkrani.getPanel("ders", k)); tabDers.setClosable(false);
            tabPane.getTabs().addAll(tabOgrenci, tabDers);

            Button btnGeriListe = new Button("⬅ Menüye Dön");
            btnGeriListe.setStyle(yesilButon);

            VBox panelBox = new VBox(10, tabPane, btnGeriListe);
            panelBox.setPadding(new Insets(20));
            panelBox.setAlignment(Pos.CENTER);
            panelBox.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");

            // Navigasyon mantığı: Mevcut 'menuBox'ın parent'ı olan StackPane'i bul ve paneli oraya ekle
            StackPane container = (StackPane) menuBox.getParent();
            navigateToPanel(container, panelBox);

            btnGeriListe.setOnAction(ev -> {
                // Geri gelme mantığı: Paneli kaldır ve menüyü göster
                navigateBack(container, panelBox);
            });
        });

        programBtn.setDisable(true);


        derslikBtn.setOnAction(e -> {
            StackPane container = (StackPane) menuBox.getParent();

            // 1. Paneli bir ScrollPane içine sarmak için wrapper oluştur
            ScrollPane scrollWrapper = new ScrollPane();
            scrollWrapper.setFitToWidth(true); // Yatayda sığdır
            scrollWrapper.setStyle("-fx-background-color: transparent;"); // Arka planı ayarla

            // 2. Geri dönme eylemi artık 'scrollWrapper'ı kaldırmalı
            Runnable derslikGeriEylemi = () -> navigateBack(container, scrollWrapper);

            // 3. Panelin içeriğini al
            DerslikGirisEkrani derslikEkrani = new DerslikGirisEkrani();
            Node derslikPaneli = derslikEkrani.getPanel(k, derslikGeriEylemi);

            // 4. İçeriği ScrollPane'e ekle
            scrollWrapper.setContent(derslikPaneli);
            
            // 5. ScrollPane'i ana konteynere ekle
            navigateToPanel(container, scrollWrapper);
        });
        
        programBtn.setOnAction(e -> {
            StackPane container = (StackPane) menuBox.getParent();

            ScrollPane scrollWrapper = new ScrollPane();
            scrollWrapper.setFitToWidth(true);
            scrollWrapper.setStyle("-fx-background-color: transparent;");

            Runnable programGeriEylemi = () -> {
                navigateBack(container, scrollWrapper); // wrapper'ı 'scrollWrapper' olarak değiştir
                butonDurumlariniGuncelle(k);
            };

            SinavProgramiEkrani programEkrani = new SinavProgramiEkrani();
            Node programPaneli = programEkrani.getPanel(stage, k, programGeriEylemi);

            scrollWrapper.setContent(programPaneli);
            navigateToPanel(container, scrollWrapper);
        });    
        
        oturmaBtn.setOnAction(e -> {
            StackPane container = (StackPane) menuBox.getParent();
            final StackPane panelWrapper = new StackPane();
            Runnable oturmaGeriEylemi = () -> navigateBack(container, panelWrapper);

            // Yeniden düzenlenmiş sınıfı çağır
            OturmaPlaniEkrani oturmaEkrani = new OturmaPlaniEkrani();
            Node oturmaPaneli = oturmaEkrani.getPanel(stage, k, oturmaGeriEylemi);
            
            panelWrapper.getChildren().add(oturmaPaneli);
            navigateToPanel(container, panelWrapper);
        });


        // === 'VBox'u Oluştur ===
        VBox vbox = new VBox(15,
                ustBar, lblWelcome, lblAltBaslik,
                new Separator(),
                derslikBtn, dersExcelBtn, ogrExcelBtn, listeBtn,
                new Separator(),
                programBtn, oturmaBtn,
                new Separator()
        );

        // === DEĞİŞİKLİK: ÇIKIŞ BUTONUNU SADECE GEREKTİĞİNDE EKLE ===
        // 'onBack' null ise (yani normal koordinatör girişi ise) çıkış butonunu ekle
        // 'onBack' null değilse (yani admin panelinden açıldıysa) bu bloğu atla
        if (onBack == null) {
            Button cikisBtn = new Button("🚪 Çıkış");
            String kirmiziButon = "-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;";
            cikisBtn.setStyle(kirmiziButon);
            cikisBtn.setOnMouseEntered(e -> cikisBtn.setStyle("-fx-background-color: #b52b27;"));
            cikisBtn.setOnMouseExited(e -> cikisBtn.setStyle(kirmiziButon));
            cikisBtn.setOnAction(e -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Çıkış Onayı");
                    alert.setHeaderText("Oturumu kapatmak istiyor musunuz?");
                    alert.setContentText("Evet'e basarsanız giriş ekranına dönülür.");
                    if (alert.showAndWait().get() == ButtonType.OK) new Main().start(stage);
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            HBox altButonlar = new HBox(15, cikisBtn);
            altButonlar.setAlignment(Pos.CENTER);
            
            // Butonu VBox'a sadece bu koşulda ekliyoruz
            vbox.getChildren().add(altButonlar);
        }

        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setPadding(new Insets(25, 30, 25, 30));
        vbox.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");

        butonDurumlariniGuncelle(k); // İki eski metodun yerine bu tek metot yeterli

        return vbox;
    }

    // === Navigasyon Yardımcı Metotları ===
    private static void navigateToPanel(StackPane container, Node newPanel) {
        if (menuBox != null) {
            menuBox.setVisible(false); // Ana menüyü gizle
        }
        container.getChildren().add(newPanel); // Yeni paneli ekle
    }

    private static void navigateBack(StackPane container, Node oldPanel) {
         container.getChildren().remove(oldPanel); // Mevcut paneli kaldır
        if (menuBox != null) {
            menuBox.setVisible(true); // Ana menüyü göster
        }
    }

    // === Diğer Yardımcılar ===
    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ... (kontrolEtVeAktiflestir ve kontrolEtVeButonDurumunuAyarla metodları aynı kalıyor) ...
    // YENİ BİRLEŞTİRİLMİŞ METOT
    // (Eski 'kontrolEtVeAktiflestir' ve 'kontrolEtVeButonDurumunuAyarla' yerine)
    private static void butonDurumlariniGuncelle(Kullanici k) {
        String sqlDers = "SELECT COUNT(*) FROM dersler WHERE bolum_id = ?";
        String sqlOgr = "SELECT COUNT(*) FROM ogrenciler WHERE bolum_id = ?";
        String sqlSinav = "SELECT COUNT(s.id) FROM sinavlar s JOIN dersler d ON s.ders_id = d.id WHERE d.bolum_id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps1 = conn.prepareStatement(sqlDers);
             PreparedStatement ps2 = conn.prepareStatement(sqlOgr);
             PreparedStatement ps3 = conn.prepareStatement(sqlSinav)) {
            
            ps1.setInt(1, k.bolumId);
            ps2.setInt(1, k.bolumId);
            ps3.setInt(1, k.bolumId);
            
            ResultSet rs1 = ps1.executeQuery();
            ResultSet rs2 = ps2.executeQuery();
            ResultSet rs3 = ps3.executeQuery();
            
            int dersSayisi = rs1.next() ? rs1.getInt(1) : 0;
            int ogrSayisi = rs2.next() ? rs2.getInt(1) : 0;
            int sinavSayisi = rs3.next() ? rs3.getInt(1) : 0;

            // 1. Liste Butonu
            if (listeBtn != null) {
                listeBtn.setDisable(!(dersSayisi > 0 && ogrSayisi > 0));
            }
            
            // 2. Program Oluştur Butonu
            if (programBtn != null) {
                programBtn.setDisable(!(dersSayisi > 0 && ogrSayisi > 0));
            }
            
            // 3. Oturma Planı Butonu
            if (oturmaBtn != null) {
                oturmaBtn.setDisable(!(sinavSayisi > 0));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            // Hata durumunda butonları devre dışı bırak
            if (listeBtn != null) listeBtn.setDisable(true);
            if (programBtn != null) programBtn.setDisable(true);
            if (oturmaBtn != null) oturmaBtn.setDisable(true);
        }
    }
  
}
