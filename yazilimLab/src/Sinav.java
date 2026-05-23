import java.time.LocalDate;
import java.time.LocalTime;

// Excel'deki her bir satırı temsil eden sınıf
public class Sinav {
    private final LocalDate tarih;
    private final LocalTime baslangicSaati;
    private final String dersAdi;
    private final String ogretimElemani;
    private final String derslikler; // YENİ EKLENDİ

    public Sinav(LocalDate tarih, LocalTime baslangicSaati, String dersAdi, String ogretimElemani, String derslikler) {
        this.tarih = tarih;
        this.baslangicSaati = baslangicSaati;
        this.dersAdi = dersAdi;
        this.ogretimElemani = ogretimElemani;
        this.derslikler = derslikler; // YENİ EKLENDİ
    }

    public LocalDate getTarih() { return tarih; }
    public LocalTime getBaslangicSaati() { return baslangicSaati; }
    public String getDersAdi() { return dersAdi; }
    public String getOgretimElemani() { return ogretimElemani; }
    public String getDerslikler() { return derslikler; } // YENİ EKLENDİ
}