import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;

public class OgrenciRow {
    private final IntegerProperty id; 
    private final StringProperty ogrNo;
    private final StringProperty adSoyad;
    private final IntegerProperty sinif;

    // Mevcut Constructor (yapıcı metot)
    public OgrenciRow(String ogrNo, String adSoyad, int sinif) {
        this.id = new SimpleIntegerProperty(0); // Varsayılan ID
        this.ogrNo = new SimpleStringProperty(ogrNo);
        this.adSoyad = new SimpleStringProperty(adSoyad);
        this.sinif = new SimpleIntegerProperty(sinif);
    }
    
    // YENİ EKLENEN CONSTRUCTOR (ID ile birlikte)
    public OgrenciRow(int id, String ogrNo, String adSoyad) {
        this.id = new SimpleIntegerProperty(id);
        this.ogrNo = new SimpleStringProperty(ogrNo);
        this.adSoyad = new SimpleStringProperty(adSoyad);
        this.sinif = new SimpleIntegerProperty(0); // Sınıf bilgisi bu senaryoda önemli değil
    }


    // Getter & Setter’lar
    public int getId() { return id.get(); } // YENİ EKLENDİ

    public String getOgrNo() { return ogrNo.get(); }
    public void setOgrNo(String value) { ogrNo.set(value); }
    public StringProperty ogrNoProperty() { return ogrNo; }

    public String getAdSoyad() { return adSoyad.get(); }
    public void setAdSoyad(String value) { adSoyad.set(value); }
    public StringProperty adSoyadProperty() { return adSoyad; }

    public int getSinif() { return sinif.get(); }
    public void setSinif(int value) { sinif.set(value); }
    public IntegerProperty sinifProperty() { return sinif; }
}
