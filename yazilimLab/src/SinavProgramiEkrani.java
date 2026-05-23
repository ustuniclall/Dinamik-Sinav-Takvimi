import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node; // Değişti
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

// DEĞİŞİKLİK: Sınıf artık 'static' metotlar yerine 'nesne' (instance) tabanlı çalışıyor
public class SinavProgramiEkrani {

    // DEĞİŞİKLİK: Tümü 'static' olmayan (instance) alanlara dönüştü
    private TableView<DersRow> secilenDerslerTable;
    private ListView<CheckBox> dersListView;
    private TextArea logArea;
    private Button ekleBtn, olusturVeRaporlaBtn;
    private Kullanici aktifKullanici;
    private final RaporlamaService raporlamaService = new RaporlamaService();

    // DEĞİŞİKLİK: 'show' metodu 'getPanel' oldu. Artık 'Stage' ve 'Runnable' alıyor.
    public Node getPanel(Stage stage, Kullanici k, Runnable onBackAction) {
        aktifKullanici = k; // Instance değişkene atandı

        // === ÜST BAR ===
        HBox ustBar = new HBox();
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 15;");

        Label lblBaslik = new Label("🗓️ Sınav Programı Oluşturma ve Raporlama");
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Button geriBtn = new Button("⬅ Geri");
        geriBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1b8a3f; -fx-font-weight: bold; -fx-background-radius: 8;");
        HBox.setHgrow(lblBaslik, Priority.ALWAYS);
        ustBar.getChildren().addAll(lblBaslik, new Region(), geriBtn);

        // === SOL TARAF: DERS LİSTESİ ===
        dersListView = new ListView<>();
        // ... (butonlar ve sol panelin kalanı aynı)
        Button hepsiniSecBtn = new Button("Tümünü Seç");
        Button secimiKaldirBtn = new Button("Seçimi Kaldır");
        for (Button b : List.of(hepsiniSecBtn, secimiKaldirBtn)) {
            b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;"));
        }
        HBox secimButonlari = new HBox(10, hepsiniSecBtn, secimiKaldirBtn);
        secimButonlari.setAlignment(Pos.CENTER_LEFT);
        VBox dersSecimBox = new VBox(10, new Label("📚 Tüm Dersler (Seçim yapın):"), secimButonlari, dersListView);
        dersSecimBox.setPadding(new Insets(10));
        dersSecimBox.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; -fx-border-radius: 10; -fx-background-radius: 10;");

        // === ORTA: SEÇİLEN DERSLER TABLOSU ===
        secilenDerslerTable = new TableView<>();
        // ... (tablo sütunları aynı)
        TableColumn<DersRow, String> kodCol = new TableColumn<>("Kod - Ad");
        kodCol.setCellValueFactory(new PropertyValueFactory<>("kodAd"));
        kodCol.setPrefWidth(350);
        TableColumn<DersRow, Integer> sureCol = new TableColumn<>("Süre (dk)");
        sureCol.setCellValueFactory(new PropertyValueFactory<>("sure"));
        secilenDerslerTable.getColumns().addAll(kodCol, sureCol);
        secilenDerslerTable.setPlaceholder(new Label("📝 Sınavı yapılacak dersleri ekleyin"));
        secilenDerslerTable.setStyle("-fx-border-color: #1b8a3f;");
        VBox secilenDerslerBox = new VBox(5,
                new Label("✅ Sınavı Oluşturulacak Dersler (Süreyi çift tıklayarak değiştirin):"),
                secilenDerslerTable);
        secilenDerslerBox.setPadding(new Insets(10));
        secilenDerslerBox.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; -fx-border-radius: 10; -fx-background-radius: 10;");


        // === SAĞ: AYARLAR PANELİ ===
        DatePicker baslangicPicker = new DatePicker(LocalDate.now());
        // ... (sağ panelin kalanı aynı)
        DatePicker bitisPicker = new DatePicker(LocalDate.now().plusDays(7));
        CheckBox haftaSonuCheck = new CheckBox("Hafta sonu hariç tut");
        haftaSonuCheck.setSelected(true);
        ChoiceBox<String> turChoice = new ChoiceBox<>(FXCollections.observableArrayList("Vize", "Final", "Bütünleme"));
        turChoice.setValue("Vize");
        TextField beklemeField = new TextField("15");
        ekleBtn = new Button("➕ Seçilenleri Listeye Ekle");
        olusturVeRaporlaBtn = new Button("🚀 Programı Oluştur ve Raporla");
        for (Button b : List.of(ekleBtn, olusturVeRaporlaBtn)) {
            b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
        }
        VBox ayarBox = new VBox(10,
                new Label("📅 Tarih Aralığı:"), new HBox(5, baslangicPicker, new Label("—"), bitisPicker),
                haftaSonuCheck, new Separator(),
                new Label("🎯 Sınav Türü:"), turChoice, new Separator(),
                new Label("⏱ Bekleme Süresi (dk):"), beklemeField, new Separator(),
                ekleBtn, olusturVeRaporlaBtn
        );
        ayarBox.setPadding(new Insets(15));
        ayarBox.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; -fx-border-radius: 10; -fx-background-radius: 10;");

        // === ANA İÇERİK ===
        HBox anaIcerikBox = new HBox(15, dersSecimBox, secilenDerslerBox, ayarBox);
        HBox.setHgrow(dersSecimBox, Priority.ALWAYS);
        HBox.setHgrow(secilenDerslerBox, Priority.ALWAYS);

        // === LOG ALANI ===
        logArea = new TextArea();
        // ... (log alanı aynı)
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-control-inner-background: #f3fff5; -fx-text-fill: #1b8a3f; "
                + "-fx-font-weight: bold; -fx-border-color: #1b8a3f; -fx-border-radius: 8;");
        Label lblLog = new Label("🧾 İşlem Günlüğü:");
        lblLog.setStyle("-fx-text-fill: #1b8a3f; -fx-font-weight: bold;");

        VBox root = new VBox(10, ustBar, new Separator(), anaIcerikBox, new Separator(), lblLog, logArea);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f3fff5;"); // açık yeşil ton

        // === GERİ BUTONU AKSİYONU (DEĞİŞTİ) ===
        geriBtn.setOnAction(e -> onBackAction.run());

        // === SAHNE KODLARI KALDIRILDI ===
        // Scene scene = new Scene(root, 1250, 720);
        // stage.setScene(scene);
        // stage.show();

        // === BUTON AKSİYONLARI ===
        setKontrollerAktif(false);
        dersleriYukle();

        // DEĞİŞİKLİK: Metotlar artık 'static' değil, 'instance' metodu
        ekleBtn.setOnAction(e -> handleEkle());
        olusturVeRaporlaBtn.setOnAction(e -> handleOlusturVeRaporla(stage, baslangicPicker.getValue(),
                bitisPicker.getValue(), haftaSonuCheck.isSelected(), turChoice.getValue(), beklemeField.getText()));

        hepsiniSecBtn.setOnAction(e -> dersListView.getItems().forEach(cb -> cb.setSelected(true)));
        secimiKaldirBtn.setOnAction(e -> dersListView.getItems().forEach(cb -> cb.setSelected(false)));

        secilenDerslerTable.setRowFactory(tv -> {
            TableRow<DersRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    DersRow ders = row.getItem();
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(ders.getSure()));
                    dialog.setTitle("Süre Değiştir");
                    dialog.setHeaderText(ders.getKodAd());
                    dialog.setContentText("Yeni sınav süresi (dakika):");
                    dialog.showAndWait().ifPresent(yeniSureStr -> {
                        try {
                            ders.setSure(Integer.parseInt(yeniSureStr));
                            secilenDerslerTable.refresh();
                        } catch (NumberFormatException ignored) {}
                    });
                }
            });
            return row;
        });
        
        // DEĞİŞİKLİK: 'Scene' yerine 'root' (Node) döndürülüyor
        return root; 
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void dersleriYukle() {
        dersListView.getItems().clear();
        String sql = "ADMIN".equalsIgnoreCase(aktifKullanici.rol)
                ? "SELECT id, kod, ad FROM dersler"
                : "SELECT id, kod, ad FROM dersler WHERE bolum_id = ?";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!"ADMIN".equalsIgnoreCase(aktifKullanici.rol)) {
                ps.setInt(1, aktifKullanici.bolumId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CheckBox cb = new CheckBox(rs.getString("kod") + " - " + rs.getString("ad"));
                cb.setUserData(new DersInfo(rs.getInt("id"), rs.getString("kod"), rs.getString("ad")));
                dersListView.getItems().add(cb);
            }
            logla("📚 Dersler başarıyla yüklendi.");
            setKontrollerAktif(true);
        } catch (SQLException ex) {
            logla("❌ Dersler yüklenirken hata: " + ex.getMessage());
        }
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void handleEkle() {
        ObservableList<DersRow> secilenler = secilenDerslerTable.getItems();
        for (CheckBox cb : dersListView.getItems()) {
            if (cb.isSelected()) {
                DersInfo info = (DersInfo) cb.getUserData();
                boolean zatenVar = secilenler.stream().anyMatch(d -> d.getId() == info.id);
                if (!zatenVar) {
                    secilenler.add(new DersRow(info.id, info.kod, info.ad, 75));
                }
                cb.setSelected(false);
            }
        }
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void handleOlusturVeRaporla(Stage stage, LocalDate bas, LocalDate bit,
                                        boolean haftaSonu, String tur, String beklemeStr) {
        if (secilenDerslerTable.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen önce sınavı oluşturulacak dersleri listeye ekleyin.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sınav Programı Raporunu Kaydet");
        fileChooser.setInitialFileName(tur + "_Sinav_Programi.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Dosyası", "*.xlsx")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            logla("ℹ️ Kullanıcı dosya kaydetme işlemini iptal etti.");
            return;
        }

        int bekleme;
        try { bekleme = Integer.parseInt(beklemeStr.trim()); }
        catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Geçersiz Değer", "Bekleme süresi geçerli bir sayı olmalıdır.");
            return;
        }

        List<Integer> seciliIds = new ArrayList<>();
        Map<Integer, Integer> dersSureleri = new HashMap<>();
        secilenDerslerTable.getItems().forEach(dr -> {
            seciliIds.add(dr.getId());
            dersSureleri.put(dr.getId(), dr.getSure());
        });

        logla("🚀 Program oluşturma ve raporlama işlemi başlatıldı...");
        setKontrollerAktif(false);

        new Thread(() -> {
            try {
                // DEĞİŞİKLİK: 'SinavProgramiEkrani::logla' yerine 'this::logla' kullanıldı
                raporlamaService.programOlusturVeExceleAktar(
                        aktifKullanici,
                        seciliIds, bas, bit, haftaSonu, tur, bekleme, dersSureleri,
                        file, this::logla
                );
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı",
                                "Sınav programı başarıyla oluşturuldu ve Excel'e aktarıldı.")
                );
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    logla("❌ KRİTİK HATA: " + ex.getMessage());
                    showAlert(Alert.AlertType.ERROR, "İşlem Başarısız",
                            "Program oluşturulurken bir hata meydana geldi: " + ex.getMessage());
                });
                ex.printStackTrace();
            } finally {
                Platform.runLater(() -> setKontrollerAktif(true));
            }
        }).start();
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void setKontrollerAktif(boolean durum) {
        if (ekleBtn != null) ekleBtn.setDisable(!durum);
        if (olusturVeRaporlaBtn != null) olusturVeRaporlaBtn.setDisable(!durum);
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void logla(String mesaj) {
        Platform.runLater(() -> logArea.appendText(mesaj + "\n"));
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}