# Dinamik Sınav Takvimi Oluşturma Sistemi 🗓️🏫🎓

Bu proje, **Kocaeli Üniversitesi Bilgisayar Mühendisliği** Yazılım Laboratuvarı-I dersi kapsamında geliştirilmiş; üniversitelerdeki sınav planlama süreçlerini otomatikleştirmek, derslik kapasitelerini, öğretim üyelerini, öğrencileri ve ders kısıtlarını dikkate alarak çakışmasız, optimize sınav takvimleri ve ders bazlı sınıf oturma planları üreten JavaFX tabanlı bir masaüstü uygulamasıdır.

## 🚀 Projenin Amacı
Sınav planlama sürecinin manuel yapılışından kaynaklanan çakışmaları ve zaman kaybını önlemektir. Sistem; Excel dosyalarından veri okuma, verileri MySQL ilişkisel veritabanında saklama, kapasite ve zaman kısıtlarına göre dinamik sınav takvimi ile sınıf oturma düzeni oluşturma ve bu çıktıları PDF/Excel formatlarında dışa aktarma süreçlerini tamamen dijitalleştirmeyi hedefler.

## 🛠️ Teknolojik Altyapı ve Bağımlılıklar
* **Arayüz ve Framework:** JavaFX (Modern ve kullanıcı dostu masaüstü arayüz tasarımı)
* **Veritabanı (RDBMS):** MySQL & MySQL Connector/J (Veri tutarlılığı ve ilişkisel yönetim)
* **Excel İşlemleri:** Apache POI (`poi-ooxml`, `xmlbeans`) (Excel'den veri okuma ve Excel'e takvim aktarma)
* **PDF Raporlama:** iText Kütüphanesi (`kernel`, `layout`, `io`, `forms`) (Ders bazlı şematik oturma planı çıktısı)

## 🔐 Rol Tabanlı Yetkilendirme ve Sistem Akışı
Sistem, güvenli bir giriş ekranı ve BCrypt tabanlı şifreleme ile iki farklı kullanıcı rolünü destekler:

1. **Admin (Yönetici) Yetkileri:**
   * Tüm bölümlerin, dersliklerin (salonların) ve kullanıcı hesaplarının yönetimi.
   * Genel takvim parametrelerinin, tarih aralıklarının ve çakışma kısıtlarının belirlenmesi.
   * Otomatik oturma planı (oda birleştirme/kapasite kontrolü dahil) oluşturma ve sistem genelinde raporlama.
2. **Bölüm Koordinatörü Yetkileri:**
   * Excel üzerinden kendi bölümüne ait ders ve öğrenci listelerini sisteme toplu aktarma.
   * Bölüm bazlı kısıtları belirleme (Aynı gün sınav kısıtı, salon tercihleri, tarih dışlama vb.).
   * Bölüm bazlı takvim oluşturma, çakışma durumunda manuel düzeltme yapma ve PDF/Excel çıktıları alma.

## ⚙️ Sistem Nasıl Çalışır? (Modüler Yapı)
* **Main.java:** Uygulamanın başlangıç sınıfıdır, giriş ekranını ve pencere ayarlarını yapılandırır.
* **DersExcelOkuyucu & OgrenciExcelOkuyucu:** Dağıtık ham Excel verilerini ayrıştırıp veritabanına otomatik kaydeder.
* **SinavProgramiOlustur:** Çakışmasız sınav takvimi oluşturma algoritmasını yürüten çekirdek sınıftır.
* **OturmaPlaniDetayEkrani:** Öğrencileri derslik kapasitelerine göre satır ve sütun düzeninde yerleştirerek sınav anı oturma planı şemasını dinamik üretir.
* **RaporlamaService:** Hazırlanan takvimlerin Excel'e (`ExcelExporter`), oturma düzeni şemalarının ise PDF'e (`PdfExporter`) dökülmesini yönetir.

## 📊 Veritabanı İlişki Modeli (E/R)
Sistem, veri bütünlüğünü ve kısıt kontrollerini sağlamak amacıyla MySQL üzerinde şu ilişkisel model yapısını kullanır.
* `Bölüm` & `Kullanıcı`: Koordinatörlerin sadece kendi bölümlerinin verilerini yönetebilmesini sağlar.
* `Ders` & `Öğrenci`: `ogrenci_ders` ara tablosuyla öğrencilerin kayıtlı olduğu dersleri ve sınav çakışmalarını denetler.
* `Sınav` & `Derslik`: `sinav_derslik` tablosuyla bir sınavın hangi salonlarda yapılacağını ve salon doluluk kapasitelerini kontrol eder.
* `OturmaPlanı`: Sınava girecek öğrencilerin hangi derslikte, hangi sıra ve sütun numarasında oturacağını benzersiz şekilde saklar.

## 📸 Ekran Görüntüleri

| Giriş Ekranı (Login) | Bölüm Koordinatörü Paneli | Sınav Programı ve Raporlama |
| :---: | :---: | :---: |
| <img src="screenshots/giris_ekrani.png" width="230"> | <img src="screenshots/koordinator_paneli.png" width="230"> | <img src="screenshots/sinav_programi.png" width="230"> |

| Admin Genel Yönetim | Otomatik Oturma Planı Arayüzü | PDF Çıktısı (Oturma Planı) |
| :---: | :---: | :---: |
| <img src="screenshots/admin_paneli.png" width="230"> | <img src="screenshots/oturma_plani_gui.png" width="230"> | <img src="screenshots/oturma_plani.png" width="230"> | <img src="screenshots/derslik_girisi.png" width="230"> |

## 👥 Geliştiriciler
* **Merve Kübra ÖZTÜRK**
* **İclal ÜSTÜN**
