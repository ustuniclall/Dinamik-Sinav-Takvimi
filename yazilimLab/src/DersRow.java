import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class DersRow {

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty kod;
    private final SimpleStringProperty ad;
    private final SimpleStringProperty ogretmen;
    private final SimpleStringProperty tur;
    private final SimpleIntegerProperty sinif;
    private final SimpleIntegerProperty sure; // ✅ sınav süresi eklendi

    // 🔹 Ders listesi (Excel’den gelen veriler) için kurucu
    public DersRow(String kod, String ad, String ogretmen, String tur, int sinif) {
        this.id = new SimpleIntegerProperty(0);
        this.kod = new SimpleStringProperty(kod);
        this.ad = new SimpleStringProperty(ad);
        this.ogretmen = new SimpleStringProperty(ogretmen);
        this.tur = new SimpleStringProperty(tur);
        this.sinif = new SimpleIntegerProperty(sinif);
        this.sure = new SimpleIntegerProperty(75); // varsayılan 75 dk
    }

    // 🔹 Sınav programı tablosu (id, kod, ad, sure)
    public DersRow(int id, String kod, String ad, int sure) {
        this.id = new SimpleIntegerProperty(id);
        this.kod = new SimpleStringProperty(kod);
        this.ad = new SimpleStringProperty(ad);
        this.ogretmen = new SimpleStringProperty("-");
        this.tur = new SimpleStringProperty("-");
        this.sinif = new SimpleIntegerProperty(0);
        this.sure = new SimpleIntegerProperty(sure);
    }

    // === Getterlar ve Property’ler ===
    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public String getKod() { return kod.get(); }
    public SimpleStringProperty kodProperty() { return kod; }

    public String getAd() { return ad.get(); }
    public SimpleStringProperty adProperty() { return ad; }

    public String getOgretmen() { return ogretmen.get(); }
    public SimpleStringProperty ogretmenProperty() { return ogretmen; }

    public String getTur() { return tur.get(); }
    public SimpleStringProperty turProperty() { return tur; }

    public int getSinif() { return sinif.get(); }
    public SimpleIntegerProperty sinifProperty() { return sinif; }

    // ✅ Sınav süresi alanı
    public int getSure() { return sure.get(); }
    public void setSure(int sure) { this.sure.set(sure); }
    public SimpleIntegerProperty sureProperty() { return sure; }

    // 👁️ Görsel tablo için birleştirilmiş gösterim
    public String getKodAd() { return getKod() + " - " + getAd(); }
}
