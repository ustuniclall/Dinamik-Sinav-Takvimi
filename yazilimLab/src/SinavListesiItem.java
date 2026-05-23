import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SinavListesiItem {
    private final SimpleIntegerProperty sinavId;
    private final SimpleStringProperty dersAdi;
    private final SimpleStringProperty tarih;
    private final SimpleStringProperty saat;
    private final SimpleStringProperty derslikler;

    public SinavListesiItem(int sinavId, String dersAdi, String tarih, String saat, String derslikler) {
        this.sinavId = new SimpleIntegerProperty(sinavId);
        this.dersAdi = new SimpleStringProperty(dersAdi);
        this.tarih = new SimpleStringProperty(tarih);
        this.saat = new SimpleStringProperty(saat);
        this.derslikler = new SimpleStringProperty(derslikler);
    }

    // TableView tarafından kullanılacak Getter'lar
    public int getSinavId() { return sinavId.get(); }
    public String getDersAdi() { return dersAdi.get(); }
    public String getTarih() { return tarih.get(); }
    public String getSaat() { return saat.get(); }
    public String getDerslikler() { return derslikler.get(); }
}