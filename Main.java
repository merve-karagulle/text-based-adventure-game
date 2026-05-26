import java.util.*;

/**
 * YMÜ227 Nesne Tabanlı Programlama
 * Metin Tabanlı Mini–Macera Oyunu
 *
 * Komutlar (SADECE TÜRKÇE):
 *   bak                       : Odaya bak
 *   git <yon>                 : kuzey/guney/dogu/bati yönüne git (k/g/d/b de olur)
 *   al <esya_kodu>            : Odanın içindeki eşyayı al (örn: al kirmizi_anahtar)
 *   kullan <esya_kodu>        : Envanterdeki eşyayı kullan (örn: kullan kirmizi_anahtar)
 *   konus <npc_ad>            : Dost NPC ile konuş veya düşmanla karşılaş
 *   sec <n>                   : Konuşma seçeneği seç (örn: sec 1)
 *   env / envanter            : Envanterini göster
 *   durum                     : Can ve saldırı gücünü göster
 *   yardim                    : Yardım metnini göster
 *   cikis                     : Oyundan çık
 *
 * Hata mesajları:
 *   - Bilinmeyen komut.
 *   - Böyle bir eşya/karakter bulunmuyor.
 *   - Bu yönde çıkış yok.
 *   - Bu eşya sende yok.
 */
public class Main {
    public static void main(String[] args) {
        OyunMotoru motor = new OyunMotoru();
        motor.baslat();
    }
}

/* ========================= OYUN MOTORU ============================== */

/**
 * Oyun döngüsünü, komutları ve dünyayı yöneten sınıf.
 */
class OyunMotoru {

    private Scanner tarayici;
    private Oyuncu oyuncu;
    private boolean calisiyor = true;

    // Kilitli çıkışlar: "odaId-yon" -> true/false
    private Map<String, Boolean> kilitliCikislar = new HashMap<>();

    // Konuşma sistemi: sadece aktif konuşma düğümü tutulur
    private KonusmaDugumu aktifKonusmaDugumu = null;

    public OyunMotoru() {
        this.tarayici = new Scanner(System.in);
    }

    /**
     * Oyun ana döngüsü.
     */
    public void baslat() {
        dunyayiHazirla();

        System.out.println("=== Mini Macera ===");
        System.out.println("Bu bir metin tabanlı mini macera oyunudur.");
        System.out.println("Yardım için istediğin zaman 'yardim' yazabilirsin.");
        System.out.println("Örnek komutlar: 'bak', 'git kuzey', 'al kirmizi_anahtar', 'konus Muhafız', 'env', 'durum'.");
        System.out.println("Şimdi bir komut yazmayı deneyebilirsin.\n");

        oyuncu.getBulunduguOda().detayYaz();
        oyuncu.durumYaz();

        while (calisiyor) {
            komutSatiriYaz(); // Dinamik komut ipuçları
            String giris;
            try {
                giris = tarayici.nextLine();
            } catch (Exception e) {
                break;
            }

            try {
                komutIsle(giris.trim());
            } catch (Exception e) {
                System.out.println("Beklenmeyen bir hata oluştu, ama oyun devam ediyor.");
                System.out.println("Hata mesajı: " + e.getMessage());
            }

            if (oyuncu.getCan() <= 0) {
                System.out.println("Canın tükendi. Oyun bitti!");
                calisiyor = false;
            }
        }

        System.out.println("\nOyun sona erdi. Teşekkürler!");
    }

    /**
     * Her turda kullanıcının verebileceği komutlara dair örnekleri yazar.
     * Bulunduğu odaya göre eşya ve NPC önerilerini de gösterir.
     */
    private void komutSatiriYaz() {
        Oda oda = oyuncu.getBulunduguOda();

        List<String> ipuclari = new ArrayList<>();
        ipuclari.add("bak");
        ipuclari.add("git kuzey");
        ipuclari.add("git guney");
        ipuclari.add("env");
        ipuclari.add("durum");

        // Odada eşya varsa bir tanesini önerelim
        if (!oda.getEsyalar().isEmpty()) {
            Esya e = oda.getEsyalar().get(0);
            ipuclari.add("al " + e.getKomutAdi());
        }

        // Odada NPC varsa bir tanesini önerelim
        if (!oda.getNpcler().isEmpty()) {
            NPC n = oda.getNpcler().get(0);
            ipuclari.add("konus " + n.getAd());
        }

        System.out.print("\nÖrnek komutlar: ");
        for (int i = 0; i < ipuclari.size(); i++) {
            System.out.print(ipuclari.get(i));
            if (i < ipuclari.size() - 1) System.out.print(" | ");
        }
        System.out.print("\nKomut bekleniyor (yardim için 'yardim'): ");
    }

    /* ==================== DÜNYA OLUŞTURMA ==================== */

    /**
     * Odalar, eşyalar ve NPC’lerin oluşturulduğu yer.
     * Kalıtım kullanan özel odalar: IyilesmeOdasi, TuzakOdasi.
     */
    private void dunyayiHazirla() {
        // En az 5 oda
        Oda salon = new Oda("salon", "Salon",
                "Eski bir şatonun giriş salonundasın. Duvarlarda solmuş tablolar var.");
        Oda koridor = new Oda("koridor", "Koridor",
                "Uzun, loş bir koridor. Zeminde gıcırdayan tahtalar var.");
        Oda silahOdasi = new Oda("silah_odasi", "Silah Odası",
                "Çeşitli kılıçlar, kalkanlar ve zırhlarla dolu bir oda.");
        Oda buyukSalon = new IyilesmeOdasi("buyuk_salon", "Büyük Salon",
                "Yüksek tavanlı büyük bir salon. Ortada parlayan bir iyileştirme çeşmesi var.");
        Oda zindan = new TuzakOdasi("zindan", "Zindan",
                "Nemli ve karanlık bir zindan. Zincir sesleri duyuyorsun...");

        // Odalar arası yön bağlantıları (TÜRKÇE YÖNLER)
        salon.bagla("kuzey", koridor);
        salon.bagla("guney", zindan);
        salon.bagla("dogu", silahOdasi);   // Kırmızı kapı buraya açılıyor

        koridor.bagla("guney", salon);
        koridor.bagla("kuzey", buyukSalon);

        buyukSalon.bagla("guney", koridor);

        zindan.bagla("kuzey", salon);

        // Salon -> dogu (Silah Odası) kapısını kilitle
        cikisKilitle(salon, "dogu");

        // Eşyalar (soyut Esya sınıfından türemiş)
        Esya kirmiziAnahtar = new AnahtarEsya(
                "key_red",
                "kirmizi_anahtar",
                "Üzerinde garip semboller olan kırmızı bir anahtar.",
                "dogu"   // bulunduğu odadaki yön
        );

        Esya kucukIksir = new IksirEsya(
                "potion_small",
                "kucuk_iksir",
                "İçince biraz can yeniler.",
                20
        );

        Esya demirKilic = new SilahEsya(
                "sword_iron",
                "demir_kilic",
                "Basit ama etkili bir kılıç.",
                5
        );

        salon.esyaEkle(kirmiziAnahtar);
        zindan.esyaEkle(kucukIksir);
        silahOdasi.esyaEkle(demirKilic);

        // NPC’ler
        DostNPC muhafiz = muhafizOlustur();
        DusmanNPC goblin = new DusmanNPC("Goblin", 25, 4);

        salon.npcEkle(muhafiz);
        zindan.npcEkle(goblin);

        // Oyuncu
        oyuncu = new Oyuncu("Oyuncu", salon);
    }

    /**
     * Muhafız için diyalog ağacı oluşturan yardımcı metot.
     */
    private DostNPC muhafizOlustur() {
        KonusmaDugumu kok = new KonusmaDugumu(
                "Muhafız: \"Dur! Bu şatoda ne işin var?\"");

        KonusmaDugumu acikla = new KonusmaDugumu(
                "Muhafız: \"Hmm... Şatoda kaybolduğunu söylüyorsun. Sana yardım edebilirim.\"");
        KonusmaDugumu kaba = new KonusmaDugumu(
                "Muhafız: \"Saygısızlık etme! Sana yardım etmeyeceğim.\"");
        KonusmaDugumu yardim = new KonusmaDugumu(
                "Muhafız: \"Pekâlâ. Salonun doğusundaki kırmızı kapı kilitli. " +
                        "Kırmızı anahtarı bulursan Silah Odası'na geçebilirsin.\"");

        kok.secenekEkle(new KonusmaSecenegi(
                "Sakin ol, sadece çıkış yolunu arıyorum.", acikla));
        kok.secenekEkle(new KonusmaSecenegi(
                "Sen kimsin bana soru soruyorsun?", kaba));

        acikla.secenekEkle(new KonusmaSecenegi(
                "Yardımına ihtiyacım var, ne yapmalıyım?", yardim));
        acikla.secenekEkle(new KonusmaSecenegi(
                "Boşver, kendi yolumu bulurum.", null));

        yardim.secenekEkle(new KonusmaSecenegi(
                "Teşekkür ederim.", null));

        return new DostNPC("Muhafız", kok);
    }

    /* ==================== KOMUT İŞLEME ==================== */

    /**
     * Kullanıcının yazdığı komutu yorumlar ve uygun işlemi yapar.
     */
    private void komutIsle(String giris) {
        if (giris.isEmpty()) {
            System.out.println("Lütfen bir komut gir. Örneğin: 'bak' veya 'yardim'.");
            return;
        }

        String[] parcalar = giris.split("\\s+");
        String komut = parcalar[0].toLowerCase();

        switch (komut) {
            case "bak":
                oyuncu.getBulunduguOda().detayYaz();
                break;

            case "git":
                if (parcalar.length < 2) {
                    System.out.println("Hangi yöne gitmek istiyorsun? (kuzey/guney/dogu/bati)");
                } else {
                    String yon = parcalar[1].toLowerCase();
                    String normalYon = yonNormalize(yon);
                    if (normalYon == null) {
                        System.out.println("Geçersiz yön. Kullanabileceğin yönler: kuzey, guney, dogu, bati.");
                    } else {
                        oyuncu.hareketEt(normalYon, this);
                    }
                }
                break;

            case "al":
                if (parcalar.length < 2) {
                    System.out.println("Hangi eşyayı almak istiyorsun?");
                } else {
                    String esyaId = parcalar[1];
                    oyuncu.esyaAl(esyaId);
                }
                break;

            case "kullan":
                if (parcalar.length < 2) {
                    System.out.println("Hangi eşyayı kullanmak istiyorsun?");
                } else {
                    String esyaId = parcalar[1];
                    oyuncu.esyaKullan(esyaId, this);
                }
                break;

            case "konus":
                if (parcalar.length < 2) {
                    System.out.println("Kiminle konuşmak istiyorsun?");
                } else {
                    String npcAdi = birlestir(parcalar, 1);
                    konus(npcAdi);
                }
                break;

            case "sec":
                if (parcalar.length < 2) {
                    System.out.println("Hangi seçeneği söylemek istiyorsun? Örn: sec 1");
                } else {
                    secenekSec(parcalar[1]);
                }
                break;

            case "env":
            case "envanter":
                oyuncu.envanteriYaz();
                break;

            case "durum":
                oyuncu.durumYaz();
                break;

            case "yardim":
                yardimYazdir();
                break;

            case "cikis":
                calisiyor = false;
                break;

            default:
                System.out.println("Bilinmeyen komut. Yardım için 'yardim' yazabilirsin.");
        }
    }

    /**
     * Yönü kısaltmalardan normalize eder.
     * k -> kuzey, g -> guney, d -> dogu, b -> bati
     */
    private String yonNormalize(String yon) {
        switch (yon) {
            case "k":
            case "kuzey":
                return "kuzey";
            case "g":
            case "guney":
                return "guney";
            case "d":
            case "dogu":
                return "dogu";
            case "b":
            case "bati":
                return "bati";
            default:
                return null;
        }
    }

    /**
     * Komut argümanlarını boşluklarla birleştiren yardımcı metot.
     */
    private String birlestir(String[] parcalar, int baslangic) {
        StringBuilder sb = new StringBuilder();
        for (int i = baslangic; i < parcalar.length; i++) {
            if (i > baslangic) sb.append(" ");
            sb.append(parcalar[i]);
        }
        return sb.toString();
    }

    /**
     * "konus" komutunun işini yapar.
     */
    private void konus(String npcAdi) {
        Oda oda = oyuncu.getBulunduguOda();
        NPC npc = oda.npcBul(npcAdi);

        if (npc == null) {
            System.out.println("Böyle bir eşya/karakter bulunmuyor.");
            return;
        }

        if (npc instanceof DostNPC) {
            aktifKonusmaDugumu = ((DostNPC) npc).getKokDugum();
            konusmaDugumuYazdir(aktifKonusmaDugumu);
        } else if (npc instanceof DusmanNPC) {
            System.out.println(npc.getAd() + " sana saldırıyor!");
            ((DusmanNPC) npc).saldir(oyuncu);
            if (((DusmanNPC) npc).getCan() <= 0) {
                System.out.println(npc.getAd() + " yenildi ve yok oldu.");
                oda.npcSil(npc);
            }
        } else {
            System.out.println(npc.getAd() + " ile konuşamıyorsun.");
        }
    }

    /**
     * "sec" komutu ile konuşma seçeneklerinin kullanılması.
     */
    private void secenekSec(String sayiStr) {
        if (aktifKonusmaDugumu == null) {
            System.out.println("Şu anda bir diyalog içinde değilsin.");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(sayiStr);
        } catch (NumberFormatException e) {
            System.out.println("Geçerli bir sayı gir (örn: sec 1).");
            return;
        }

        List<KonusmaSecenegi> secenekler = aktifKonusmaDugumu.getSecenekler();
        if (index < 1 || index > secenekler.size()) {
            System.out.println("Geçersiz seçim numarası.");
            return;
        }

        KonusmaSecenegi secenek = secenekler.get(index - 1);
        System.out.println("Sen: " + secenek.getMetin());

        KonusmaDugumu sonraki = secenek.getSonrakiDugum();
        if (sonraki == null) {
            System.out.println("(Konuşma sona erdi.)");
            aktifKonusmaDugumu = null;
        } else {
            aktifKonusmaDugumu = sonraki;
            konusmaDugumuYazdir(aktifKonusmaDugumu);
        }
    }

    /**
     * Konuşma düğümünün metnini ve seçeneklerini ekrana basar.
     */
    private void konusmaDugumuYazdir(KonusmaDugumu dugum) {
        System.out.println(dugum.getMetin());
        if (dugum.getSecenekler().isEmpty()) {
            System.out.println("(Seçenek yok, 'sec' kullanılamaz.)");
            return;
        }

        System.out.println("Seçenekler:");
        int i = 1;
        for (KonusmaSecenegi s : dugum.getSecenekler()) {
            System.out.println("  " + (i++) + ") " + s.getMetin());
        }
        System.out.println("Seçim yapmak için: sec <numara> (örn: sec 1)");
    }

    /**
     * Kullanıcıya yardım metni gösterir.
     */
    private void yardimYazdir() {
        System.out.println("Komutlar:");
        System.out.println("  bak                      : Bulunduğun odayı incele");
        System.out.println("  git <yon>                : kuzey/guney/dogu/bati yönüne git (k/g/d/b de olur)");
        System.out.println("  al <esya_kodu>           : Odanın içindeki bir eşyayı al (örn: al kirmizi_anahtar)");
        System.out.println("  kullan <esya_kodu>       : Envanterdeki eşyayı kullan (örn: kullan kirmizi_anahtar)");
        System.out.println("  konus <ad>               : Dost NPC ile konuş veya düşmanla yüzleş (örn: konus Muhafız)");
        System.out.println("  sec <n>                  : Diyalog seçeneği seç (örn: sec 1)");
        System.out.println("  env / envanter           : Envanterini göster");
        System.out.println("  durum                    : Can ve saldırı gücünü göster");
        System.out.println("  yardim                   : Bu yardım metnini göster");
        System.out.println("  cikis                    : Oyundan çık");
    }

    /* ==================== KİLİTLİ ÇIKIŞ YÖNETİMİ ==================== */

    private String cikisAnahtari(Oda oda, String yon) {
        return oda.getId() + "-" + yon.toLowerCase();
    }

    public void cikisKilitle(Oda oda, String yon) {
        kilitliCikislar.put(cikisAnahtari(oda, yon), true);
    }

    public boolean cikisKilitliMi(Oda oda, String yon) {
        Boolean deger = kilitliCikislar.get(cikisAnahtari(oda, yon));
        return deger != null && deger;
    }

    public void cikisKilidiniAc(Oda oda, String yon) {
        kilitliCikislar.put(cikisAnahtari(oda, yon), false);
    }
}

/* ========================= ODA SINIFLARI ============================== */

/**
 * Temel oda sınıfı.
 * Odalar arası bağlantıları ve içerdiği eşyaları / NPC'leri tutar.
 */
class Oda {
    private String id;
    private String ad;
    private String aciklama;

    private Map<String, Oda> cikislar = new HashMap<>();   // kuzey, guney, dogu, bati
    private List<Esya> esyalar = new ArrayList<>();
    private List<NPC> npcler = new ArrayList<>();

    public Oda(String id, String ad, String aciklama) {
        this.id = id;
        this.ad = ad;
        this.aciklama = aciklama;
    }

    public String getId() {
        return id;
    }

    public String getAd() {
        return ad;
    }

    public void bagla(String yon, Oda hedef) {
        cikislar.put(yon.toLowerCase(), hedef);
    }

    public Oda cikisAl(String yon) {
        return cikislar.get(yon.toLowerCase());
    }

    public void esyaEkle(Esya e) {
        esyalar.add(e);
    }

    public void esyaSil(Esya e) {
        esyalar.remove(e);
    }

    public List<Esya> getEsyalar() {
        return esyalar;
    }

    public void npcEkle(NPC npc) {
        npcler.add(npc);
    }

    public void npcSil(NPC npc) {
        npcler.remove(npc);
    }

    public List<NPC> getNpcler() {
        return npcler;
    }

    public NPC npcBul(String ad) {
        for (NPC n : npcler) {
            if (n.getAd().equalsIgnoreCase(ad)) {
                return n;
            }
        }
        return null;
    }

    /**
     * Odaya her girildiğinde tetiklenen metot.
     * Alt sınıflar (TuzakOdasi, IyilesmeOdasi) bunu override ederek özel davranış ekler.
     */
    public void odaGirildi(Oyuncu oyuncu) {
        // Varsayılan odada özel bir şey olmaz.
    }

    /**
     * Odanın açıklamasını, çıkışları, eşyaları ve NPC’leri ekrana yazar.
     */
    public void detayYaz() {
        System.out.println("\nŞu anda bulunduğun yer: " + ad);
        System.out.println(aciklama);

        // Çıkışlar
        if (cikislar.isEmpty()) {
            System.out.println("Çıkışlar: yok");
        } else {
            System.out.print("Çıkışlar: ");
            List<String> yonler = new ArrayList<>(cikislar.keySet());
            Collections.sort(yonler);
            for (int i = 0; i < yonler.size(); i++) {
                System.out.print(yonler.get(i));
                if (i < yonler.size() - 1) System.out.print(", ");
            }
            System.out.println();
        }

        // Eşyalar
        if (esyalar.isEmpty()) {
            System.out.println("Eşyalar: yok");
        } else {
            System.out.print("Eşyalar: ");
            for (int i = 0; i < esyalar.size(); i++) {
                System.out.print(esyalar.get(i).getKomutAdi());
                if (i < esyalar.size() - 1) System.out.print(", ");
            }
            System.out.println();
        }

        // NPC'ler
        if (npcler.isEmpty()) {
            System.out.println("Karakterler: yok");
        } else {
            System.out.print("Karakterler: ");
            for (int i = 0; i < npcler.size(); i++) {
                System.out.print(npcler.get(i).getAd());
                if (i < npcler.size() - 1) System.out.print(", ");
            }
            System.out.println();
        }
    }
}

/**
 * Özel oda türü: tuzak oda.
 * İçine girildiğinde oyuncunun canını düşürür (sadece ilk girişte).
 */
class TuzakOdasi extends Oda {

    private boolean tuzakAktif = true;
    private int hasar = 15;

    public TuzakOdasi(String id, String ad, String aciklama) {
        super(id, ad, aciklama);
    }

    @Override
    public void odaGirildi(Oyuncu oyuncu) {
        if (tuzakAktif) {
            System.out.println("Tuzak tetiklendi! Etrafına sivri oklar fırlıyor.");
            oyuncu.hasarAl(hasar);
            tuzakAktif = false;
        }
    }
}

/**
 * Özel oda türü: iyileşme odası.
 * İçine girildiğinde oyuncunun canını yükseltir (sadece ilk girişte).
 */
class IyilesmeOdasi extends Oda {

    private boolean kullanildi = false;
    private int iyilesmeMiktari = 30;

    public IyilesmeOdasi(String id, String ad, String aciklama) {
        super(id, ad, aciklama);
    }

    @Override
    public void odaGirildi(Oyuncu oyuncu) {
        if (!kullanildi) {
            System.out.println("Parlayan çeşmeden bir yudum içiyorsun. Kendini daha iyi hissediyorsun.");
            oyuncu.canArtir(iyilesmeMiktari);
            kullanildi = true;
        }
    }
}

/* ========================= ESYA (SOYUT) VE ALT SINIFLAR ============================== */

/**
 * Oyundaki tüm eşyalar için ortak üst sınıf (soyut).
 * Kalıtım ve polimorfizm örneği.
 */
abstract class Esya {
    private String id;        // örn: key_red
    private String komutAdi;  // örn: kirmizi_anahtar
    private String aciklama;

    public Esya(String id, String komutAdi, String aciklama) {
        this.id = id;
        this.komutAdi = komutAdi;
        this.aciklama = aciklama;
    }

    public String getId() {
        return id;
    }

    public String getKomutAdi() {
        return komutAdi;
    }

    public String getAciklama() {
        return aciklama;
    }

    /**
     * Her eşya kullanıldığında farklı bir davranış sergiler.
     * Bu yüzden soyut metot ve alt sınıflar override eder.
     */
    public abstract void kullan(Oyuncu oyuncu, OyunMotoru motor);
}

/**
 * Anahtar türü eşya.
 * Bulunulan odadaki belirli yönü kilitsiz hale getirir.
 */
class AnahtarEsya extends Esya {

    private String kilitYon; // örn: "dogu"

    public AnahtarEsya(String id, String komutAdi, String aciklama, String kilitYon) {
        super(id, komutAdi, aciklama);
        this.kilitYon = kilitYon.toLowerCase();
    }

    @Override
    public void kullan(Oyuncu oyuncu, OyunMotoru motor) {
        Oda oda = oyuncu.getBulunduguOda();
        if (!motor.cikisKilitliMi(oda, kilitYon)) {
            System.out.println("Bu yönde kilitli bir kapı yok veya zaten açık.");
            return;
        }
        Oda hedef = oda.cikisAl(kilitYon);
        if (hedef == null) {
            System.out.println("Bu yönde kapı yok.");
            return;
        }

        motor.cikisKilidiniAc(oda, kilitYon);
        System.out.println("Kapının kilidini açtın ve " + kilitYon +
                " yönüne, " + hedef.getAd() + " odasına geçtin.");
        oyuncu.setBulunduguOda(hedef);
        hedef.detayYaz();
    }
}

/**
 * Can artıran iksir eşyası.
 */
class IksirEsya extends Esya {

    private int iyilesmeMiktari;

    public IksirEsya(String id, String komutAdi, String aciklama, int iyilesmeMiktari) {
        super(id, komutAdi, aciklama);
        this.iyilesmeMiktari = iyilesmeMiktari;
    }

    @Override
    public void kullan(Oyuncu oyuncu, OyunMotoru motor) {
        System.out.println(getKomutAdi() + " içtin. Canın " + iyilesmeMiktari + " arttı.");
        oyuncu.canArtir(iyilesmeMiktari);
    }
}

/**
 * Silah eşyası. Oyuncunun saldırı gücünü artırır.
 */
class SilahEsya extends Esya {

    private int bonusSaldiri;

    public SilahEsya(String id, String komutAdi, String aciklama, int bonusSaldiri) {
        super(id, komutAdi, aciklama);
        this.bonusSaldiri = bonusSaldiri;
    }

    @Override
    public void kullan(Oyuncu oyuncu, OyunMotoru motor) {
        System.out.println(getKomutAdi() + " kuşandın. Saldırı gücün +" + bonusSaldiri + " arttı.");
        oyuncu.saldiriGucuArtir(bonusSaldiri);
    }
}

/* ========================= OYUNCU ============================== */

class Oyuncu {

    private String ad;
    private Oda bulunduguOda;
    private List<Esya> envanter = new ArrayList<>();
    private int can = 100;
    private int saldiriGucu = 5;

    public Oyuncu(String ad, Oda baslangicOda) {
        this.ad = ad;
        this.bulunduguOda = baslangicOda;
    }

    public Oda getBulunduguOda() {
        return bulunduguOda;
    }

    public void setBulunduguOda(Oda oda) {
        this.bulunduguOda = oda;
        oda.odaGirildi(this);
    }

    public int getCan() {
        return can;
    }

    public int getSaldiriGucu() {
        return saldiriGucu;
    }

    public void hasarAl(int miktar) {
        this.can -= miktar;
        if (this.can < 0) this.can = 0;
        System.out.println("Hasar aldın! (" + miktar + ") Mevcut can: " + this.can);
    }

    public void canArtir(int miktar) {
        this.can += miktar;
        if (this.can > 100) this.can = 100;
        System.out.println("Canın yenilendi. Mevcut can: " + this.can);
    }

    public void saldiriGucuArtir(int miktar) {
        this.saldiriGucu += miktar;
        System.out.println("Yeni saldırı gücün: " + this.saldiriGucu);
    }

    /**
     * Yön belirterek oda değiştirme.
     */
    public void hareketEt(String yon, OyunMotoru motor) {
        Oda mevcut = getBulunduguOda();
        Oda hedef = mevcut.cikisAl(yon);

        if (hedef == null) {
            System.out.println("Bu yönde çıkış yok.");
            return;
        }

        if (motor.cikisKilitliMi(mevcut, yon)) {
            System.out.println("Kırmızı kapı kilitli.");
            return;
        }

        setBulunduguOda(hedef);
        hedef.detayYaz();
    }

    /**
     * Bulunulan odadaki bir eşyayı envantere alır.
     */
    public void esyaAl(String esyaKomutAdi) {
        Oda oda = getBulunduguOda();
        Esya hedef = null;
        for (Esya e : oda.getEsyalar()) {
            if (e.getKomutAdi().equalsIgnoreCase(esyaKomutAdi)
                    || e.getId().equalsIgnoreCase(esyaKomutAdi)) {
                hedef = e;
                break;
            }
        }

        if (hedef == null) {
            System.out.println("Böyle bir eşya/karakter bulunmuyor.");
            return;
        }

        envanter.add(hedef);
        oda.esyaSil(hedef);
        System.out.println(hedef.getKomutAdi() + " envanterine eklendi.");
    }

    /**
     * Envanterdeki bir eşyayı kullanır.
     */
    public void esyaKullan(String esyaKomutAdi, OyunMotoru motor) {
        Esya hedef = null;
        for (Esya e : envanter) {
            if (e.getKomutAdi().equalsIgnoreCase(esyaKomutAdi)
                    || e.getId().equalsIgnoreCase(esyaKomutAdi)) {
                hedef = e;
                break;
            }
        }

        if (hedef == null) {
            System.out.println("Bu eşya sende yok.");
            return;
        }

        hedef.kullan(this, motor);

        // Anahtar ve iksir tek kullanımlık olsun
        if (hedef instanceof AnahtarEsya || hedef instanceof IksirEsya) {
            envanter.remove(hedef);
        }
    }

    /**
     * Envanterdeki eşyaları ekrana yazar.
     */
    public void envanteriYaz() {
        if (envanter.isEmpty()) {
            System.out.println("Envanterin boş.");
            return;
        }

        System.out.println("Envanterindeki eşyalar:");
        for (Esya e : envanter) {
            System.out.println(" - " + e.getKomutAdi() + " (" + e.getId() + "): " + e.getAciklama());
        }
    }

    /**
     * Oyuncunun durumunu (can, saldırı gücü, bulunduğu oda) ekrana yazar.
     */
    public void durumYaz() {
        System.out.println("=== Oyuncu Durumu ===");
        System.out.println("Bulunduğun oda : " + bulunduguOda.getAd());
        System.out.println("Can            : " + can);
        System.out.println("Saldırı gücü   : " + saldiriGucu);
    }
}

/* ========================= NPC HİYERARŞİSİ ============================== */

/**
 * Tüm NPC'lerin üst sınıfı.
 */
abstract class NPC {
    private String ad;

    public NPC(String ad) {
        this.ad = ad;
    }

    public String getAd() {
        return ad;
    }
}

/**
 * Dost NPC – oyuncu ile diyalog kurabilen karakter.
 */
class DostNPC extends NPC {

    private KonusmaDugumu kokDugum;

    public DostNPC(String ad, KonusmaDugumu kokDugum) {
        super(ad);
        this.kokDugum = kokDugum;
    }

    public KonusmaDugumu getKokDugum() {
        return kokDugum;
    }
}

/**
 * Düşman NPC – oyuncuya saldırabilen karakter.
 */
class DusmanNPC extends NPC {

    private int can;
    private int saldiriHasari;

    public DusmanNPC(String ad, int can, int saldiriHasari) {
        super(ad);
        this.can = can;
        this.saldiriHasari = saldiriHasari;
    }

    public int getCan() {
        return can;
    }

    public void hasarAl(int miktar) {
        this.can -= miktar;
        if (this.can < 0) this.can = 0;
        System.out.println(getAd() + " " + miktar + " hasar aldı. (Kalan can: " + can + ")");
    }

    /**
     * Düşmanın oyuncuya saldırmasını sağlayan metot.
     */
    public void saldir(Oyuncu oyuncu) {
        System.out.println(getAd() + " sana saldırıyor!");
        oyuncu.hasarAl(saldiriHasari);
    }
}

/* ========================= KONUŞMA YAPISI ============================== */

/**
 * Konuşma ağacındaki bir düğümü temsil eder.
 */
class KonusmaDugumu {
    private String metin;
    private List<KonusmaSecenegi> secenekler = new ArrayList<>();

    public KonusmaDugumu(String metin) {
        this.metin = metin;
    }

    public String getMetin() {
        return metin;
    }

    public List<KonusmaSecenegi> getSecenekler() {
        return secenekler;
    }

    public void secenekEkle(KonusmaSecenegi secenek) {
        secenekler.add(secenek);
    }
}

/**
 * Konuşma düğümünden çıkılabilecek bir seçeneği temsil eder.
 */
class KonusmaSecenegi {
    private String metin;
    private KonusmaDugumu sonrakiDugum;

    public KonusmaSecenegi(String metin, KonusmaDugumu sonrakiDugum) {
        this.metin = metin;
        this.sonrakiDugum = sonrakiDugum;
    }

    public String getMetin() {
        return metin;
    }

    public KonusmaDugumu getSonrakiDugum() {
        return sonrakiDugum;
    }
}
