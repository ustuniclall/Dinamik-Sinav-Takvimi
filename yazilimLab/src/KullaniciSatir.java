import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class KullaniciSatir {
    private final StringProperty ad;
    private final StringProperty email;
    private final StringProperty rol;
    private final StringProperty bolum;

    public KullaniciSatir(String ad, String email, String rol, String bolum) {
        this.ad = new SimpleStringProperty(ad);
        this.email = new SimpleStringProperty(email);
        this.rol = new SimpleStringProperty(rol);
        this.bolum = new SimpleStringProperty(bolum);
    }

    public StringProperty adProperty() { return ad; }
    public StringProperty emailProperty() { return email; }
    public StringProperty rolProperty() { return rol; }
    public StringProperty bolumProperty() { return bolum; }
}
