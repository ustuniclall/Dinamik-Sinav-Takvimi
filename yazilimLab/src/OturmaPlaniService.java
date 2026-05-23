import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class OturmaPlaniItem {
    private final String ogrNo, adSoyad, derslikAdi;
    private final int sira, sutun;
    public OturmaPlaniItem(String ogrNo, String adSoyad, String derslikAdi, int sira, int sutun) {
        this.ogrNo = ogrNo; this.adSoyad = adSoyad; this.derslikAdi = derslikAdi; this.sira = sira; this.sutun = sutun;
    }
    public String getOgrNo() { return ogrNo; }
    public String getAdSoyad() { return adSoyad; }
    public String getDerslikAdi() { return derslikAdi; }
    public int getSira() { return sira; }
    public int getSutun() { return sutun; }
}

class DerslikLayout {
    public final int id;
    public final String ad;
    public final int satir, sutun, siraYapisi;
    public final List<int[]> yerlesimYerleri = new ArrayList<>();

    public DerslikLayout(int id, String ad, int satir, int sutun, int siraYapisi) {
        this.id = id; this.ad = ad; this.satir = satir; this.sutun = sutun; this.siraYapisi = siraYapisi;
        for (int i = 0; i < this.satir; i++) {
            for (int j = 0; j < this.sutun; j++) {
                boolean oturulabilir = true;
                if (siraYapisi == 3 && j % 3 == 1) oturulabilir = false;
                else if (siraYapisi == 2 && j % 2 == 1) oturulabilir = false;
                else if (siraYapisi == 4) {
                    if (j % 4 == 1 || j % 4 == 3) oturulabilir = false;
                }
                if (oturulabilir) yerlesimYerleri.add(new int[]{i + 1, j + 1});
            }
        }
    }
}

public class OturmaPlaniService {

    public List<OturmaPlaniItem> planOlusturVeKaydet(int sinavId) throws Exception {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM oturma_plani WHERE sinav_id = ?")) {
                    ps.setInt(1, sinavId);
                    ps.executeUpdate();
                }
                List<OgrenciRow> ogrenciler = new ArrayList<>();
                String sqlOgr = "SELECT o.id, o.ogr_no, o.ad_soyad FROM ogrenciler o JOIN ogrenci_ders od ON o.id = od.ogrenci_id JOIN sinavlar s ON s.ders_id = od.ders_id WHERE s.id = ? ORDER BY o.ogr_no";
                try (PreparedStatement ps = conn.prepareStatement(sqlOgr)) {
                    ps.setInt(1, sinavId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) ogrenciler.add(new OgrenciRow(rs.getInt("id"), rs.getString("ogr_no"), rs.getString("ad_soyad")));
                }
                if (ogrenciler.isEmpty()) throw new Exception("Bu sınava kayıtlı öğrenci bulunamadı.");
                
                List<DerslikLayout> derslikler = new ArrayList<>();
                String sqlDerslik = "SELECT d.id, d.derslik_adi, d.boyuna_sira_sayisi, d.enine_sira_sayisi, d.sira_yapisi FROM derslikler d JOIN sinav_derslik sd ON d.id = sd.derslik_id WHERE sd.sinav_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlDerslik)) {
                    ps.setInt(1, sinavId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) derslikler.add(new DerslikLayout(rs.getInt("id"), rs.getString("derslik_adi"), rs.getInt("boyuna_sira_sayisi"), rs.getInt("enine_sira_sayisi"), rs.getInt("sira_yapisi")));
                }
                if (derslikler.isEmpty()) throw new Exception("Bu sınava atanmış derslik bulunamadı.");
                
                int mevcutOgrenciIndex = 0;
                String sqlInsert = "INSERT INTO oturma_plani (sinav_id, ogrenci_id, derslik_id, sira_no, sutun_no) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    for (DerslikLayout derslik : derslikler) {
                        for (int[] yer : derslik.yerlesimYerleri) {
                            if (mevcutOgrenciIndex >= ogrenciler.size()) break;
                            OgrenciRow ogrenci = ogrenciler.get(mevcutOgrenciIndex);
                            psInsert.setInt(1, sinavId);
                            psInsert.setInt(2, ogrenci.getId());
                            psInsert.setInt(3, derslik.id);
                            psInsert.setInt(4, yer[0]);
                            psInsert.setInt(5, yer[1]);
                            psInsert.addBatch();
                            mevcutOgrenciIndex++;
                        }
                        if (mevcutOgrenciIndex >= ogrenciler.size()) break;
                    }
                    psInsert.executeBatch();
                }
                conn.commit();
                return getMevcutPlan(conn, sinavId);
            } catch(Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<OturmaPlaniItem> getMevcutPlan(Connection conn, int sinavId) throws SQLException {
        List<OturmaPlaniItem> plan = new ArrayList<>();
        String sql = "SELECT o.ogr_no, o.ad_soyad, d.derslik_adi, op.sira_no, op.sutun_no FROM oturma_plani op JOIN ogrenciler o ON op.ogrenci_id = o.id JOIN derslikler d ON op.derslik_id = d.id WHERE op.sinav_id = ? ORDER BY d.derslik_adi, op.sira_no, op.sutun_no";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sinavId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) plan.add(new OturmaPlaniItem(rs.getString("ogr_no"), rs.getString("ad_soyad"), rs.getString("derslik_adi"), rs.getInt("sira_no"), rs.getInt("sutun_no")));
        }
        return plan;
    }
    // Bu metodu OturmaPlaniService sınıfının içine ekleyin
public List<DerslikLayout> getDerslikLayoutsForSinav(int sinavId) throws SQLException {
    List<DerslikLayout> derslikler = new ArrayList<>();
    String sqlDerslik = "SELECT d.id, d.derslik_adi, d.boyuna_sira_sayisi, d.enine_sira_sayisi, d.sira_yapisi FROM derslikler d JOIN sinav_derslik sd ON d.id = sd.derslik_id WHERE sd.sinav_id = ?";
    
    try (Connection conn = DBManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sqlDerslik)) {
        ps.setInt(1, sinavId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            derslikler.add(new DerslikLayout(
                rs.getInt("id"),
                rs.getString("derslik_adi"),
                rs.getInt("boyuna_sira_sayisi"), // satir
                rs.getInt("enine_sira_sayisi"), // sutun
                rs.getInt("sira_yapisi")
            ));
        }
    }
    return derslikler;
}
}