import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DersSatir {
    private final StringProperty kod;
    private final StringProperty ad;
    private final StringProperty bolum;

    public DersSatir(String kod, String ad, String bolum) {
        this.kod = new SimpleStringProperty(kod);
        this.ad = new SimpleStringProperty(ad);
        this.bolum = new SimpleStringProperty(bolum);
    }

    public StringProperty kodProperty() { return kod; }
    public StringProperty adProperty() { return ad; }
    public StringProperty bolumProperty() { return bolum; }
}
