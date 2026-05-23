import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.sql.*;

public class DerslikGirisEkrani {

    private TextField txtKod, txtAd, txtKapasite, txtEnine, txtBoyuna, txtSiraYapisi, txtArama;
    private Canvas canvas;
    private Kullanici aktifKullanici;

    public Node getPanel(Kullanici k, Runnable onBackAction) {
        this.aktifKullanici = k;

        // === ÜST BAR ===
        HBox ustBar = new HBox();
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 12;");
        Label lblTitle = new Label("🏫 Derslik Girişi (" + k.bolumAdi + ")");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        ustBar.getChildren().add(lblTitle);

        Label lblBolumInfo = new Label("Bölüm: " + k.bolumAdi);
        lblBolumInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // === FORM ALANI (DEĞİŞTİ) ===
        txtKod = new TextField(); txtKod.setPromptText("Derslik Kodu");
        txtAd = new TextField(); txtAd.setPromptText("Derslik Adı");

        // DEĞİŞİKLİK: Prompt'lar (yardımcı metinler) yeni mantığa göre güncellendi
        txtEnine = new TextField(); txtEnine.setPromptText("Sıra Genişliği (Yan Yana Banka Sayısı)");
        txtBoyuna = new TextField(); txtBoyuna.setPromptText("Sıra Derinliği (Önden Arkaya Sıra Sayısı)");
        txtSiraYapisi = new TextField(); txtSiraYapisi.setPromptText("Sıra Yapısı (Bir banka kaç kişilik? 2, 3...)");

        // DEĞİŞİKLİK: Kapasite alanı artık düzenlenemez
        txtKapasite = new TextField(); txtKapasite.setPromptText("Derslik Kapasitesi (Otomatik Hesaplanır)");
        txtKapasite.setEditable(false);
        txtKapasite.setStyle("-fx-control-inner-background: #f4f4f4;");

        // DEĞİŞİKLİK: Otomatik kapasite hesaplaması için Listener'lar
        txtEnine.textProperty().addListener((obs, old, a) -> hesaplaVeGuncelle());
        txtBoyuna.textProperty().addListener((obs, old, a) -> hesaplaVeGuncelle());
        txtSiraYapisi.textProperty().addListener((obs, old, a) -> hesaplaVeGuncelle());

        // === BUTONLAR ===
        Button btnKaydet = new Button("💾 Kaydet");
        Button btnSil = new Button("🗑 Sil");
        Button btnCiz = new Button("📐 Görselleştir");
        Button btnGeri = new Button("⬅️ Geri Dön");
        // ... (Buton stilleri aynı)
        for (Button b : new Button[]{btnKaydet, btnSil, btnCiz, btnGeri}) {
            b.setStyle("-fx-background-color:#1b8a3f; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#166e33; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color:#1b8a3f; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;"));
        }

        btnKaydet.setOnAction(e -> derslikEkle());
        btnSil.setOnAction(e -> derslikSil());
        btnCiz.setOnAction(e -> oturmaCiz());
        btnGeri.setOnAction(e -> onBackAction.run());

        HBox btnBox = new HBox(10, btnKaydet, btnSil, btnCiz, btnGeri);
        btnBox.setAlignment(Pos.CENTER);

        // === ARAMA ALANI ===
        txtArama = new TextField(); txtArama.setPromptText("Derslik Kodu ile ara...");
        Button btnAra = new Button("🔍 Ara");
        btnAra.setStyle("-fx-background-color:#1b8a3f; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;");
        btnAra.setOnMouseEntered(e -> btnAra.setStyle("-fx-background-color:#166e33; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;"));
        btnAra.setOnMouseExited(e -> btnAra.setStyle("-fx-background-color:#1b8a3f; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8;"));
        btnAra.setOnAction(e -> derslikAra());
        HBox aramaBox = new HBox(10, txtArama, btnAra);
        aramaBox.setAlignment(Pos.CENTER_LEFT);

        // === OTURMA PLANI CANVAS ===
        canvas = new Canvas(420, 320);
        canvas.setStyle("-fx-border-color:#1b8a3f; -fx-border-width:2; -fx-background-color:white;");

        // === FORM PANELİ (DEĞİŞTİ) ===
        VBox form = new VBox(12,
                ustBar,
                new Label("🧾 Derslik Bilgileri:"),
                lblBolumInfo,
                txtKod, txtAd,
                txtEnine, // Banka Sayısı
                txtBoyuna, // Sıra Derinliği
                txtSiraYapisi, // Sıra Yapısı
                txtKapasite, // Otomatik Kapasite
                btnBox,
                new Separator(),
                new Label("🔎 Derslik Arama:"), aramaBox,
                new Label("🪑 Oturma Planı:"), canvas
        );
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.TOP_CENTER);
        form.setMaxWidth(500);
        form.setStyle("-fx-background-color:white; -fx-border-color:#1b8a3f; "
                + "-fx-border-radius:12; -fx-background-radius:12; "
                + "-fx-padding:25; -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f3fff5;");
        root.setCenter(form);
        BorderPane.setAlignment(form, Pos.CENTER);

        return root;
    }

    // === YENİ METOT: Otomatik Kapasite Hesaplama ===
    private int getKullanilabilirSira(int siraYapisi) {
        if (siraYapisi == 3) return 2;
        if (siraYapisi == 2) return 1;
        if (siraYapisi == 4) return 2;
        if (siraYapisi == 1) return 1;
        // Diğer durumlar için varsayılan (veya hata)
        if (siraYapisi <= 0) return 0;
        return siraYapisi / 2; // Varsayılan mantık
    }

    private void hesaplaVeGuncelle() {
        try {
            int bankaSayisi = Integer.parseInt(txtEnine.getText());
            int siraDerinligi = Integer.parseInt(txtBoyuna.getText());
            int siraYapisi = Integer.parseInt(txtSiraYapisi.getText());
            
            int kullanilabilirSira = getKullanilabilirSira(siraYapisi);
            
            int toplamKapasite = siraDerinligi * bankaSayisi * kullanilabilirSira;
            
            txtKapasite.setText(String.valueOf(toplamKapasite));
            
        } catch (NumberFormatException e) {
            txtKapasite.setText("0"); // Geçersiz giriş varsa 0 yaz
        }
    }
    // === YENİ METOT SONU ===


    // 💾 Derslik ekleme
    // DEĞİŞİKLİK: Veritabanına kaydedilen 'enine' ve 'boyuna' değerleri yeni mantığa göre ayarlandı.
    private void derslikEkle() {
        try {
            // Formdan yeni mantığa göre değerleri al
            int bankaSayisi = Integer.parseInt(txtEnine.getText()); // Örn: 3
            int siraDerinligi = Integer.parseInt(txtBoyuna.getText()); // Örn: 7
            int siraYapisi = Integer.parseInt(txtSiraYapisi.getText()); // Örn: 3
            int kapasite = Integer.parseInt(txtKapasite.getText()); // Otomatik hesaplanan (Örn: 42)

            // DEĞİŞİKLİK: Veritabanının 'OturmaPlaniService' ile uyumlu olması için
            // 'enine_sira_sayisi' (sütun) ve 'boyuna_sira_sayisi' (satır)
            // değerlerini doğru hesapla.
            
            // OturmaPlaniService 'boyuna_sira_sayisi'ni SATIR olarak alır.
            int db_boyuna_sira_sayisi = siraDerinligi; // (Örn: 7)
            
            // OturmaPlaniService 'enine_sira_sayisi'ni SÜTUN olarak alır.
            int db_enine_sira_sayisi = bankaSayisi * siraYapisi; // (Örn: 3 * 3 = 9)

            int bolumId = aktifKullanici.bolumId;
            String bolumAdi = aktifKullanici.bolumAdi;
            String derslikKodu = txtKod.getText();
            String derslikAdi = txtAd.getText();

            String sqlInsert = """
                INSERT INTO derslikler
                (bolum_id, bolum_adi, derslik_kodu, derslik_adi, kapasite,
                 enine_sira_sayisi, boyuna_sira_sayisi, sira_yapisi)
                VALUES (?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                 bolum_id = VALUES(bolum_id),
                 bolum_adi = VALUES(bolum_adi),
                 derslik_adi = VALUES(derslik_adi),
                 kapasite = VALUES(kapasite),
                 enine_sira_sayisi = VALUES(enine_sira_sayisi),
                 boyuna_sira_sayisi = VALUES(boyuna_sira_sayisi),
                 sira_yapisi = VALUES(sira_yapisi)
            """;

            try (Connection conn = DBManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

                ps.setInt(1, bolumId);
                ps.setString(2, bolumAdi);
                ps.setString(3, derslikKodu);
                ps.setString(4, derslikAdi);
                ps.setInt(5, kapasite);
                ps.setInt(6, db_enine_sira_sayisi); // Toplam Sütun (Örn: 9)
                ps.setInt(7, db_boyuna_sira_sayisi); // Toplam Satır (Örn: 7)
                ps.setInt(8, siraYapisi); // Sıra Yapısı (Örn: 3)
                
                int affectedRows = ps.executeUpdate();

                if (affectedRows == 1) {
                    uyar("✅ Derslik kaydedildi. (Kodu: " + derslikKodu + ")");
                } else if (affectedRows == 2 || affectedRows == 0) {
                     uyar("ℹ️ Derslik güncellendi. (Kodu: " + derslikKodu + ")");
                }
                txtArama.setText(derslikKodu);
            }

        } catch (NumberFormatException ex) {
            uyar("⚠️ Lütfen Banka Sayısı, Sıra Derinliği ve Sıra Yapısı alanlarını sayısal olarak girin.");
        } catch (SQLException ex) {
            uyar("💥 Veritabanı hatası: " + ex.getMessage());
        }
    }

    // 🔍 Arama
    // DEĞİŞİKLİK: Veritabanından okunan değerler yeni mantığa göre form alanlarına çevrildi.
    private void derslikAra() {
        String kod = txtArama.getText().trim();
        if (kod.isEmpty()) { uyar("⚠️ Lütfen bir Derslik Kodu girin."); return; }

        String sql = "SELECT * FROM derslikler WHERE derslik_kodu = ? AND bolum_id = ? LIMIT 1";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, kod);
            ps.setInt(2, aktifKullanici.bolumId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtKod.setText(rs.getString("derslik_kodu"));
                //txtKod.setEditable(false); // Güncellemeyi önerisi eklendi
                txtAd.setText(rs.getString("derslik_adi"));
                
                // DEĞİŞİKLİK: Veritabanı verisini form mantığına dönüştür
                int db_toplam_sutun = rs.getInt("enine_sira_sayisi"); // Örn: 9
                int db_toplam_satir = rs.getInt("boyuna_sira_sayisi"); // Örn: 7
                int db_sira_yapisi = rs.getInt("sira_yapisi"); // Örn: 3
                
                // Formdaki "Banka Sayısı"nı (txtEnine) hesapla
                int bankaSayisi = (db_sira_yapisi > 0) ? (db_toplam_sutun / db_sira_yapisi) : 0; // Örn: 9 / 3 = 3
                
                txtBoyuna.setText(String.valueOf(db_toplam_satir)); // Sıra Derinliği (Örn: 7)
                txtEnine.setText(String.valueOf(bankaSayisi)); // Banka Sayısı (Örn: 3)
                txtSiraYapisi.setText(String.valueOf(db_sira_yapisi)); // Sıra Yapısı (Örn: 3)
                
                // Kapasite alanı zaten listener'lar sayesinde otomatik dolacak
                // ama biz yine de veritabanındaki değeri yazalım:
                txtKapasite.setText(String.valueOf(rs.getInt("kapasite"))); // Örn: 42
                
                oturmaCiz();
            } else {
                uyar("❌ Bu bölüme ait ("+ aktifKullanici.bolumAdi +") böyle bir derslik kodu bulunamadı.");
                txtKod.setEditable(true); // Bulunamadıysa kod alanını aç
            }

        } catch (SQLException e) {
            uyar("💥 Veritabanı hatası: " + e.getMessage());
        }
    }

    // 🗑 Silme
    // (Bu metot aynı kaldı, 'bolum_id' filtresi zaten vardı)
    private void derslikSil() {
        String kod = txtArama.getText().trim();
        if (kod.isEmpty()) { uyar("⚠️ Silmek için bir Derslik Kodu girin."); return; }

        String sql = "DELETE FROM derslikler WHERE derslik_kodu = ? AND bolum_id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kod);
            ps.setInt(2, aktifKullanici.bolumId);
            int row = ps.executeUpdate();
            if (row > 0)
                uyar("🗑 Derslik (" + kod + ") bu bölümden silindi.");
            else
                uyar("❌ Bu bölüme ait böyle bir derslik kodu bulunamadı.");
        } catch (SQLException e) { uyar("💥 Veritabanı hatası: " + e.getMessage()); }
    }
    
    // 🎨 Oturma Planı Çizimi
    private void oturmaCiz() {
        try {
            // === DEĞİŞİKLİK BAŞLANGICI ===
            // Formdaki YENİ mantık değerlerini al
            int bankaSayisi = Integer.parseInt(txtEnine.getText());    // Örn: 3 (Banka Sayısı)
            int siraDerinligi = Integer.parseInt(txtBoyuna.getText()); // Örn: 7 (Sıra Derinliği)
            int siraYapisi = Integer.parseInt(txtSiraYapisi.getText());   // Örn: 3 (Sıra Yapısı)

            // Çizim için gereken ESKİ mantık değerlerini (satır ve sütun) HESAPLA
            int satir = siraDerinligi;             // Toplam Satır Sayısı (Örn: 7)
            int sutun = bankaSayisi * siraYapisi; // Toplam Sütun Sayısı (Örn: 3 * 3 = 9)
            // === DEĞİŞİKLİK SONU ===


            // --- Metodun geri kalanı aynı, sadece artık doğru 'satir' ve 'sutun'u kullanıyor ---
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setFill(Color.web("#f3fff5"));
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // Sıfır sütun hatasını önle
            if (sutun <= 0 || satir <= 0) {
                 g.setFill(Color.BLACK);
                 g.fillText("⚠️ Çizim için geçerli sıra/sütun bilgisi girilmedi.", 10, 20);
                 return; // Çizimi yapma
            }

            double cellW = canvas.getWidth() / sutun;
            double cellH = canvas.getHeight() / satir;
            int kullanilabilirSira = 0;

            for (int i = 0; i < satir; i++) {
                for (int j = 0; j < sutun; j++) {
                    boolean oturulabilir = true;
                    if (siraYapisi == 3 && j % 3 == 1) oturulabilir = false;
                    else if (siraYapisi == 2 && j % 2 == 1) oturulabilir = false;
                    else if (siraYapisi == 4) {
                        int poz = j % 4;
                        if (poz == 1 || poz == 3) oturulabilir = false;
                    }
                    if (oturulabilir) { g.setFill(Color.web("#1b8a3f")); kullanilabilirSira++; }
                    else g.setFill(Color.WHITE);
                    g.fillRect(j * cellW + 5, i * cellH + 5, cellW - 10, cellH - 10);
                }
            }
            g.setStroke(Color.web("#166e33"));
            g.setLineWidth(1.2);
            for (int i = 0; i < satir; i++)
                for (int j = 0; j < sutun; j += siraYapisi)
                    g.strokeRect(j * cellW + 2, i * cellH + 2, cellW * Math.min(siraYapisi, sutun - j) - 4, cellH - 4);

            g.setFill(Color.BLACK);
            g.fillText("✅ Yeşil: Oturulabilir, ⬜ Beyaz: Boş", 10, canvas.getHeight() - 30);
            g.fillText("Sınav kapasitesi: " + kullanilabilirSira, 10, canvas.getHeight() - 15);
            
            Platform.runLater(() -> {
                Node node = canvas;
                ScrollPane parentScrollPane = null;
                while (node != null) {
                    if (node instanceof ScrollPane) {
                        parentScrollPane = (ScrollPane) node;
                        break; 
                    }
                    node = node.getParent(); 
                }
                if (parentScrollPane != null) {
                    parentScrollPane.setVvalue(1.0);
                }
            });
        } catch (NumberFormatException e) { 
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
             g.setFill(Color.RED); // Hata durumunda mesajı kırmızı yap
             g.fillText("⚠️ Lütfen Banka Sayısı, Sıra Derinliği ve Sıra Yapısı alanlarını sayısal olarak girin.", 10, 20);
        }
        catch (Exception e) { uyar("💥 Çizim hatası: " + e.getMessage()); }
    }

    private void uyar(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}