import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Sınav Takvimi Sistemi Giriş");

        // 🖼️ Pencere ikonu
        var logoStream = Main.class.getResourceAsStream("/styles/logo.png"); 
        if (logoStream != null) {
            stage.getIcons().add(new Image(logoStream));
        }
        
        // === 🖼️ FORMA EKLENECEK LOGO ===
        ImageView formLogo = null;
        var formLogoStream = Main.class.getResourceAsStream("/styles/logo.png"); 
        if (formLogoStream != null) {
            formLogo = new ImageView(new Image(formLogoStream));
            formLogo.setFitWidth(150); // Logonun boyutu (isteğe bağlı)
            formLogo.setPreserveRatio(true);
        }
        
        // === GİRİŞ FORMU ===
        Label emailLabel = new Label("E-posta:");
        TextField emailField = new TextField();
        emailField.setPromptText("E-posta adresinizi girin");
        emailField.setMaxWidth(250);

        Label sifreLabel = new Label("Şifre:");
        PasswordField sifreField = new PasswordField();
        sifreField.setPromptText("Şifrenizi girin");
        sifreField.setMaxWidth(250);

        Button girisBtn = new Button("Giriş Yap");
        girisBtn.getStyleClass().add("login-btn");
        girisBtn.setDefaultButton(true);
        girisBtn.setPrefWidth(120);

        VBox formBox;
        if (formLogo != null) {
            // Eğer logo bulunduysa, onu VBox'ın en üstüne ekle
            formBox = new VBox(12, formLogo, emailLabel, emailField, sifreLabel, sifreField, girisBtn);
        } else {
            // Bulunamadıysa, eski haliyle devam et
            formBox = new VBox(12, emailLabel, emailField, sifreLabel, sifreField, girisBtn);
        }
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(25));
        formBox.getStyleClass().add("card");

        // === ORTA KAPSAYICI (formu ortalamak için) ===
        StackPane centerPane = new StackPane(formBox);
        centerPane.setAlignment(Pos.CENTER);

      // === ANA STACKPANE: sadece form (arka plan CSS'den gelecek) ===
        StackPane root = new StackPane();
        root.getChildren().add(centerPane); // Sadece giriş formunu ekle        

        root.getStyleClass().add("login-pane");
        Scene girisScene = new Scene(root, 900, 600);
        
        // CSS'İ SAHNEYE (SCENE) EKLİYORUZ (DOĞRU YÖNTEM BUDUR)
        try {
            girisScene.getStylesheets().add(
                    Objects.requireNonNull(Main.class.getResource("/styles/bilsis.css")).toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("⚠️ CSS bulunamadı.");
        }
        
      

        // === SAHNE ===
        //Scene girisScene = new Scene(root, 900, 600);
        stage.setScene(girisScene);
        stage.setMaximized(true);
        stage.show();

        // === GİRİŞ İŞLEMİ ===
        girisBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String sifre = sifreField.getText().trim();

            if (email.isEmpty() || sifre.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen e-posta ve şifrenizi giriniz.");
                return;
            }

            try (Connection conn = DBManager.getConnection()) {
                String sql = """
                        SELECT k.ad, k.rol, k.bolum_id, b.ad AS bolum_adi
                        FROM kullanicilar k
                        LEFT JOIN bolumler b ON k.bolum_id = b.id
                        WHERE k.email = ? AND k.sifre = ?
                        """;
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, email);
                ps.setString(2, sifre);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    Kullanici k = new Kullanici(
                            rs.getString("ad"),
                            rs.getString("rol"),
                            rs.getInt("bolum_id"),
                            rs.getString("bolum_adi")
                    );
                    showAnaEkran(stage, k);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Hatalı Giriş", "E-posta veya şifre hatalı!");
                }

            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Veritabanı Hatası", ex.getMessage());
            }
        });
    }

    private void showAnaEkran(Stage stage, Kullanici k) {
        if ("ADMIN".equalsIgnoreCase(k.rol)) {
            stage.setScene(AdminEkrani.ekraniGetir(k, stage));
        } else if ("KOORDINATOR".equalsIgnoreCase(k.rol)) {
            stage.setScene(KoordinatorEkrani.ekraniGetir(k, stage));
        } else {
            showAlert(Alert.AlertType.ERROR, "Bilinmeyen Rol", "Bu rol için arayüz tanımlı değil: " + k.rol);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
