import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node; // Değişti
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.util.*;
import javafx.scene.Scene;

// DEĞİŞİKLİK: Sınıf artık 'static' metotlar yerine 'nesne' (instance) tabanlı çalışıyor
public class OturmaPlaniDetayEkrani {

    // DEĞİŞİKLİK: Tümü 'static' olmayan (instance) alanlara dönüştü
    private Label lblStatus;
    private TableView<OturmaPlaniItem> tableView;
    private VBox visualPane;
    private Button btnOlustur, btnPdf;
    private ScrollPane scrollPane; // YENİ EKLENDİ
    private OturmaPlaniService planService = new OturmaPlaniService();
    private SinavListesiItem secilenSinav;
    private Stage mainStage;

    // DEĞİŞİKLİK: 'show' metodu 'getPanel' oldu. Artık 'Runnable onBackAction' alıyor.
    public Node getPanel(Stage stage, Kullanici k, SinavListesiItem sinav, Runnable onBackAction) {
        mainStage = stage;
        secilenSinav = sinav;

        // === ÜST YEŞİL BAR ===
        HBox ustBar = new HBox();
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setStyle("-fx-background-color: #1b8a3f; -fx-padding: 15;");

        Label lblBaslik = new Label("🪑 " + sinav.getDersAdi() + " - Oturma Planı");
        lblBaslik.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        ustBar.getChildren().add(lblBaslik);

        // === DURUM METNİ ===
        lblStatus = new Label("📋 Mevcut plan yükleniyor veya oluşturulmaya hazır...");
        lblStatus.setStyle("-fx-text-fill: #1b8a3f; -fx-font-size: 14px; -fx-font-weight: bold;");

        // === BUTONLAR ===
        btnOlustur = new Button("🔄 Oturma Planını Oluştur / Yenile");
        btnPdf = new Button("📄 PDF Olarak İndir");
        btnPdf.setDisable(true);
        Button btnGeri = new Button("⬅ Sınav Listesine Geri Dön");

        for (Button b : new Button[]{btnOlustur, btnPdf, btnGeri}) {
            b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15 8 15;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #166e33; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15 8 15;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #1b8a3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15 8 15;"));
        }

        // DEĞİŞİKLİK: 'btnGeri' eylemi artık 'onBackAction'ı çağırıyor
        btnGeri.setOnAction(e -> onBackAction.run());
        
        // DEĞİŞİKLİK: Metotlar artık 'static' değil
        btnOlustur.setOnAction(e -> planOlustur());
        btnPdf.setOnAction(e -> planiPdfYap());

        HBox buttonBar = new HBox(15, btnOlustur, btnPdf, btnGeri);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10));

        // === TABLO ===
        setupTableView(); // static olmayan metot
        tableView.setStyle("-fx-border-color: #1b8a3f; -fx-border-width: 2; -fx-background-color: white;");

        // === GÖRSEL PANEL ===
        visualPane = new VBox(15);
        visualPane.setPadding(new Insets(15));
        scrollPane = new ScrollPane(visualPane); // 'ScrollPane' tipi kaldırıldı
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #1b8a3f; -fx-border-width: 2;");

        SplitPane splitPane = new SplitPane(tableView, scrollPane);
        splitPane.setDividerPositions(0.40);

        // === ANA LAYOUT ===
        VBox root = new VBox(10, ustBar, lblStatus, splitPane, buttonBar);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f3fff5;");

        mevcutPlaniYukle(); // static olmayan metot

        // === SAHNE KODLARI KALDIRILDI ===
        // Scene scene = new Scene(root, 1366, 768);
        // stage.setScene(scene);
        // stage.show();

        return root; // DEĞİŞİKLİK: 'Node' döndürülüyor
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void setupTableView() {
        tableView = new TableView<>();
        // ... (metodun kalanı aynı)
        TableColumn<OturmaPlaniItem, String> colNo = new TableColumn<>("Öğrenci No");
        colNo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOgrNo()));
        TableColumn<OturmaPlaniItem, String> colAd = new TableColumn<>("Ad Soyad");
        colAd.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdSoyad()));
        TableColumn<OturmaPlaniItem, String> colDerslik = new TableColumn<>("Derslik");
        colDerslik.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDerslikAdi()));
        TableColumn<OturmaPlaniItem, Number> colSira = new TableColumn<>("Sıra");
        colSira.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSira()));
        TableColumn<OturmaPlaniItem, Number> colSutun = new TableColumn<>("Sütun");
        colSutun.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSutun()));
        tableView.getColumns().addAll(colNo, colAd, colDerslik, colSira, colSutun);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void renderVisualPlan(List<OturmaPlaniItem> plan, List<DerslikLayout> derslikler) {
        visualPane.getChildren().clear();
        // ... (metodun kalanı aynı)
        Map<String, OturmaPlaniItem> planMap = new HashMap<>();
        for (OturmaPlaniItem item : plan)
            planMap.put(item.getDerslikAdi() + "-" + item.getSira() + "-" + item.getSutun(), item);

        for (DerslikLayout derslik : derslikler) {
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            grid.setStyle("-fx-border-color: #1b8a3f; -fx-border-width: 2; -fx-background-color: white;");

            for (int satir = 1; satir <= derslik.satir; satir++) {
                for (int sutun = 1; sutun <= derslik.sutun; sutun++) {
                    StackPane seatPane = new StackPane();
                    Rectangle seat = new Rectangle(100, 50);
                    String key = derslik.ad + "-" + satir + "-" + sutun;
                    OturmaPlaniItem student = planMap.get(key);

                    if (student != null) {
                        seat.setFill(Color.web("#1b8a3f"));
                        seat.setStroke(Color.DARKGREEN);
                        Text text = new Text(student.getOgrNo() + "\n" + student.getAdSoyad());
                        text.setFont(Font.font(9));
                        text.setTextAlignment(TextAlignment.CENTER);
                        text.setFill(Color.WHITE);
                        seatPane.getChildren().addAll(seat, text);
                    } else {
                        seat.setFill(Color.web("#cce9d1"));
                        seat.setStroke(Color.web("#1b8a3f"));
                        seatPane.getChildren().add(seat);
                    }
                    grid.add(seatPane, sutun - 1, satir - 1);
                }
            }
            TitledPane titledPane = new TitledPane(derslik.ad + " Oturma Düzeni", grid);
            titledPane.setStyle("-fx-font-weight: bold; -fx-text-fill: #1b8a3f;");
            titledPane.setExpanded(true);
            visualPane.getChildren().add(titledPane);
        }
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void planOlustur() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Mevcut plan silinip yeniden oluşturulacak. Devam edilsin mi?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Onay");
        alert.setHeaderText("Oturma Planı Oluşturulacak");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            lblStatus.setText("Plan oluşturuluyor, lütfen bekleyin...");
            btnOlustur.setDisable(true);
            btnPdf.setDisable(true);

            new Thread(() -> {
                try {
                    List<OturmaPlaniItem> yeniPlan = planService.planOlusturVeKaydet(secilenSinav.getSinavId());
                    List<DerslikLayout> derslikler = planService.getDerslikLayoutsForSinav(secilenSinav.getSinavId());
                    Platform.runLater(() -> updateUIWithPlan(yeniPlan, derslikler, "✅ Plan başarıyla oluşturuldu."));
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Hata", "Plan oluşturulamadı: " + e.getMessage()));
                } finally {
                    Platform.runLater(() -> btnOlustur.setDisable(false));
                }
            }).start();
        }
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void mevcutPlaniYukle() {
        new Thread(() -> {
            try {
                List<OturmaPlaniItem> mevcutPlan;
                List<DerslikLayout> derslikler;
                try (Connection conn = DBManager.getConnection()) {
                    mevcutPlan = planService.getMevcutPlan(conn, secilenSinav.getSinavId());
                }
                derslikler = planService.getDerslikLayoutsForSinav(secilenSinav.getSinavId());
                Platform.runLater(() -> updateUIWithPlan(mevcutPlan, derslikler,
                        mevcutPlan.isEmpty() ? "⚠ Plan bulunamadı." : "📋 Mevcut plan yüklendi."));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Hata", "Plan yüklenirken hata: " + e.getMessage()));
            }
        }).start();
    }

    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void updateUIWithPlan(List<OturmaPlaniItem> plan, List<DerslikLayout> derslikler, String statusMessage) {
        tableView.setItems(FXCollections.observableArrayList(plan));
        renderVisualPlan(plan, derslikler);
        lblStatus.setText(statusMessage + "  (" + plan.size() + " öğrenci yerleştirildi)");
        btnPdf.setDisable(plan.isEmpty());
    }

// OturmaPlaniDetayEkrani.java içindesiniz...

    private void planiPdfYap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Oturma Planı PDF'ini Kaydet");
        fileChooser.setInitialFileName(secilenSinav.getDersAdi() + "_OturmaPlani.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Dosyası", "*.pdf"));
        File file = fileChooser.showSaveDialog(mainStage);

        if (file == null) {
            return; // Kullanıcı iptal etti
        }

        // === FİX BAŞLANGICI: Paneli geçici olarak ScrollPane'den ayır ===
        
        // 1. Paneli (visualPane) ScrollPane'den geçici olarak ayır
        scrollPane.setContent(null);

        // 2. Paneli hafızada yeni bir sahneye yerleştir.
        //    Bu, JavaFX'i paneli kısıtlama olmadan,
        //    gerçekte ihtiyaç duyduğu tam boyutta çizmeye zorlar.
        //    Arka planı beyaz yap ki PDF'te şeffaf görünmesin.
        visualPane.setStyle("-fx-background-color: white;");
        new Scene(visualPane); 

        try {
            // 3. Artık kısıtlanmamış ve tam boyutlu olan 'visualPane'in
            //    doğrudan kendisinin anlık görüntüsünü al.
            WritableImage image = visualPane.snapshot(null, null);
            
            // === FİX SONU ===

            // 4. Görüntüyü PDF'e işle
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            
            ByteArrayInputStream imageInputStream = new ByteArrayInputStream(baos.toByteArray());
            
            // Güncellenmiş PdfExporter'ı çağır
            PdfExporter.createSeatingPlanPdf(file, secilenSinav, tableView.getItems(), imageInputStream);
            
            showAlert(Alert.AlertType.INFORMATION, "Başarılı", "📄 Oturma planı kaydedildi:\n" + file.getAbsolutePath());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Hata", "PDF oluşturulurken hata: " + e.getMessage());
            e.printStackTrace(); 
        } finally {
            // === TEMİZLİK ===
            // 5. İşlem başarılı da olsa, hata da verse, paneli (visualPane)
            //    ait olduğu yere, ScrollPane'in içine geri koy.
            scrollPane.setContent(visualPane);
            visualPane.setStyle(null); // Geçici stili kaldır
        }
    }
    
    // DEĞİŞİKLİK: 'static' kaldırıldı
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}