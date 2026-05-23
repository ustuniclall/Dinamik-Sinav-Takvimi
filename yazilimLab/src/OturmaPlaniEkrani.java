import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node; // Değişti
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

// DEĞİŞİKLİK: Sınıf artık 'static' metotlar yerine 'nesne' (instance) tabanlı çalışıyor
public class OturmaPlaniEkrani {

    private Kullanici aktifKullanici;
    private StackPane mainContainer; // DEĞİŞİKLİK: Navigasyon için StackPane
    private VBox listPanel;          // DEĞİŞİKLİK: Ana liste paneli

    // DEĞİŞİKLİK: 'show' metodu 'getPanel' oldu. Artık 'Runnable onBackAction' alıyor.
    public Node getPanel(Stage stage, Kullanici k, Runnable onBackAction) {
        aktifKullanici = k;

        // === ÜST YEŞİL BAR ===
        Label lblBaslik = new Label("🪑 Oturma Planı - Sınav Seçimi");
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        // DEĞİŞİKLİK: 'btnCikisUst' kaldırıldı. Artık gömülü çalışıyor.
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox ustBar = new HBox(15, lblBaslik, spacer);
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 15;");

        // === ALT BAŞLIK ===
        Label lblTitle = new Label("📋 Çift tıklayarak oturma planını görüntüleyebilirsiniz");
        lblTitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #1b8a3f; -fx-font-weight: bold;");
        lblTitle.setPadding(new Insets(10, 0, 10, 0));
        lblTitle.setAlignment(Pos.CENTER);

        // === TABLO ===
        TableView<SinavListesiItem> table = new TableView<>();
        // ... (tablo ve sütun tanımlamaları aynı)
        table.setPlaceholder(new Label("📭 Görüntülenecek güncel sınav bulunamadı."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-border-color: #1b8a3f; -fx-border-width: 2; -fx-background-color: white;");
        TableColumn<SinavListesiItem, String> dersCol = new TableColumn<>("Ders Adı");
        dersCol.setCellValueFactory(new PropertyValueFactory<>("dersAdi"));
        dersCol.setPrefWidth(300);
        TableColumn<SinavListesiItem, String> tarihCol = new TableColumn<>("Tarih");
        tarihCol.setCellValueFactory(new PropertyValueFactory<>("tarih"));
        TableColumn<SinavListesiItem, String> saatCol = new TableColumn<>("Saat");
        saatCol.setCellValueFactory(new PropertyValueFactory<>("saat"));
        TableColumn<SinavListesiItem, String> dersliklerCol = new TableColumn<>("Derslikler");
        dersliklerCol.setCellValueFactory(new PropertyValueFactory<>("derslikler"));
        dersliklerCol.setPrefWidth(350);
        table.getColumns().addAll(dersCol, tarihCol, saatCol, dersliklerCol);
        
        table.setItems(sinavlariYukle()); // DEĞİŞİKLİK: 'static' olmayan metot

        // === ÇİFT TIKLAMA OLAYI (DEĞİŞTİ) ===
        table.setRowFactory(tv -> {
            TableRow<SinavListesiItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    SinavListesiItem rowData = row.getItem();
                    
                    // DEĞİŞİKLİK: 'OturmaPlaniDetayEkrani.show' yerine panel navigasyonu
                    OturmaPlaniDetayEkrani detayEkrani = new OturmaPlaniDetayEkrani();
                    final StackPane panelWrapper = new StackPane();
                    
                    // Detay ekranının geri eylemi, bu wrapper'ı kaldırmaktır
                    Runnable detayGeriEylemi = () -> navigateBack(panelWrapper);
                    
                    Node detayPaneli = detayEkrani.getPanel(stage, aktifKullanici, rowData, detayGeriEylemi);
                    panelWrapper.getChildren().add(detayPaneli);
                    
                    // Ana StackPane'e yeni paneli ekle
                    navigateToPanel(panelWrapper);
                }
            });
            return row;
        });

        // === GERİ DÖN BUTONU (DEĞİŞTİ) ===
        Button btnGeri = new Button("⬅ Ana Menüye Geri Dön");
        // ... (buton stilleri aynı)
        btnGeri.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 20 8 20;");
        btnGeri.setOnMouseEntered(e -> btnGeri.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 20 8 20;"));
        btnGeri.setOnMouseExited(e -> btnGeri.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 20 8 20;"));
        
        // DEĞİŞİKLİK: 'btnGeri' eylemi artık 'onBackAction'ı çağırıyor
        btnGeri.setOnAction(e -> onBackAction.run());

        // === ALT BAR ===
        HBox altBar = new HBox(btnGeri);
        altBar.setAlignment(Pos.CENTER_RIGHT);
        altBar.setPadding(new Insets(15, 25, 25, 25));

        // === ANA LAYOUT (listPanel) ===
        listPanel = new VBox(15, ustBar, lblTitle, table, altBar);
        listPanel.setPadding(new Insets(20));
        listPanel.setStyle("-fx-background-color: #f3fff5;");

        // === SAHNE KODLARI KALDIRILDI ===
        
        // DEĞİŞİKLİK: Navigasyon için 'listPanel'i 'mainContainer'a ekle
        mainContainer = new StackPane();
        mainContainer.getChildren().add(listPanel);
        
        return mainContainer; // DEĞİŞİKLİK: 'StackPane' (Node) döndürülüyor
    }

    // === Sınavları Veritabanından Yükle ===
    // DEĞİŞİKLİK: 'static' kaldırıldı
    private ObservableList<SinavListesiItem> sinavlariYukle() {
        ObservableList<SinavListesiItem> sinavListesi = FXCollections.observableArrayList();
        // ... (SQL sorgusu aynı)
        String sql = "SELECT s.id AS sinav_id, d.ad AS ders_adi, s.tarih, s.baslangic_saati, GROUP_CONCAT(dl.derslik_adi SEPARATOR ', ') AS atanan_derslikler FROM sinavlar s JOIN dersler d ON s.ders_id = d.id LEFT JOIN sinav_derslik sd ON s.id = sd.sinav_id LEFT JOIN derslikler dl ON sd.derslik_id = dl.id WHERE d.bolum_id = ? AND s.tarih >= CURDATE() GROUP BY s.id, d.ad, s.tarih, s.baslangic_saati ORDER BY s.tarih, s.baslangic_saati";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, aktifKullanici.bolumId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sinavListesi.add(new SinavListesiItem(
                        rs.getInt("sinav_id"),
                        rs.getString("ders_adi"),
                        rs.getString("tarih"),
                        rs.getString("baslangic_saati"),
                        rs.getString("atanan_derslikler")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sinavListesi;
    }
    
    // === YENİ EKLENEN NAVİGASYON METOTLARI ===
    private void navigateToPanel(Node newPanel) {
        if (listPanel != null) {
            listPanel.setVisible(false);
        }
        mainContainer.getChildren().add(newPanel);
    }

    private void navigateBack(Node oldPanel) {
        mainContainer.getChildren().remove(oldPanel);
        if (listPanel != null) {
            listPanel.setVisible(true);
        }
    }
}