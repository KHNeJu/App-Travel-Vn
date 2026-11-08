package vn.travelvn.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

record ExplorePhoto(int image, String author, String license, String file) {
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<Integer, Bitmap> PHOTOS = new LruCache<>(24 * 1024 * 1024) {
        @Override protected int sizeOf(Integer id, Bitmap photo) { return photo.getAllocationByteCount(); }
    };

    static void bind(ImageView view, int image) {
        Bitmap cached = PHOTOS.get(image);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        Resources resources = view.getResources();
        DECODER.execute(() -> {
            Bitmap photo = PHOTOS.get(image);
            if (photo == null) {
                photo = BitmapFactory.decodeResource(resources, image);
                if (photo != null) PHOTOS.put(image, photo);
            }
            Bitmap result = photo;
            if (result != null) MAIN.post(() -> {
                if (view.isAttachedToWindow() && view.getDrawable() == null) view.setImageBitmap(result);
            });
        });
    }

    String url() {
        return "https://commons.wikimedia.org/wiki/File:" + Uri.encode(file);
    }

    static ExplorePhoto lookup(String key) {
        if (key.isEmpty()) return null;
        ExplorePhoto photo = switch (key) {
            case "ha_giang" -> new ExplorePhoto(R.drawable.explore_ha_giang, "Khánh Hmoong", "CC BY 2.0", "Ma Pi Leng Pass winding road Ha Giang Vietnam.jpg");
            case "cao_bang" -> new ExplorePhoto(R.drawable.explore_cao_bang, "Adam Jones from Kelowna, BC, Canada", "CC BY-SA 2.0", "Ban Gioc Waterfall - Trung Kanh District - Cao Bang Province - Vietnam - 10 (48119779296).jpg");
            case "ho_hoan_kiem" -> new ExplorePhoto(R.drawable.explore_ho_hoan_kiem, "rex pe", "CC BY 2.0", "Hoan Kiem Lake.jpg");
            case "ho_tay" -> new ExplorePhoto(R.drawable.explore_ho_tay, "Nguyen Duc Anh", "CC0 1.0", "West Lake, Hanoi.jpg");
            case "van_mieu" -> new ExplorePhoto(R.drawable.explore_van_mieu, "gorik", "CC BY-SA 2.0", "Temple of Literature, Hanoi.jpg");
            case "ba_dinh" -> new ExplorePhoto(R.drawable.explore_ba_dinh, "Vyacheslav Argenberg", "CC BY 4.0", "Hanoi, Vietnam, Ho Chi Minh Mausoleum on Ba Dinh Square.jpg");
            case "ca_phe_giang" -> new ExplorePhoto(R.drawable.explore_ca_phe_giang, "Amenoc", "CC0 1.0", "Egg coffee Café Giang, lane 39, since 1946.jpg");
            case "metropole" -> new ExplorePhoto(R.drawable.explore_metropole, "notacrime", "CC BY-SA 2.0", "Hotel Metropole Hanoi 01.jpg");
            case "intercontinental_nha_trang" -> new ExplorePhoto(R.drawable.explore_intercontinental_nha_trang, "InterContinental Nha Trang", "CC BY-SA 4.0", "InterContinental Nha Trang.jpg");
            case "intercontinental_hanoi" -> new ExplorePhoto(R.drawable.explore_intercontinental_hanoi, "missbossy", "CC BY 2.0", "InterContinental Hanoi Westlake Hotel.jpg");
            case "thang_long" -> new ExplorePhoto(R.drawable.explore_thang_long, "Duyphuong (thảo luận)", "CC BY 3.0", "Đoan Môn.JPG");
            case "nha_hat_lon" -> new ExplorePhoto(R.drawable.explore_nha_hat_lon, "xiquinhosilva", "CC BY 2.0", "Hanoi Opera House, 24 December 2016.jpg");
            case "food" -> new ExplorePhoto(R.drawable.explore_food, "GiveMeMollusks", "CC0 1.0", "Two bowls of phở bò with various toppings.jpg");
            case "coffee" -> new ExplorePhoto(R.drawable.explore_coffee, "ProjectManhattan", "CC BY-SA 3.0", "Vietnamese coffee.jpg");
            case "ben_thanh" -> new ExplorePhoto(R.drawable.explore_ben_thanh, "Stephan Ridgway", "CC BY 2.0", "Inside Ben Thanh market.jpg");
            case "suoi_tien" -> new ExplorePhoto(R.drawable.explore_suoi_tien, "sayo ts", "CC0 1.0", "Suoi Tien Amusement Park (32169459717).jpg");
            case "hotel_room" -> new ExplorePhoto(R.drawable.explore_hotel_room, "Adkdhc8", "Public domain", "Majestic Hotel room.JPG");
            case "tra_su" -> new ExplorePhoto(R.drawable.explore_tra_su, "Tran Anh Khoa (zorrotran)", "CC BY-SA 2.0", "Rừng tràm Trà Sư (37639233302).jpg");
            case "my_khe" -> new ExplorePhoto(R.drawable.explore_my_khe, ". Ray in Manila", "CC BY 2.0", "My Khe Beach, Da Nang, Vietnam.jpg");
            case "vinh_ha_long" -> new ExplorePhoto(R.drawable.explore_vinh_ha_long, "Ondřej Žváček", "CC BY 2.5", "Ha Long Bay.jpg");
            case "ao_ba_om" -> new ExplorePhoto(R.drawable.explore_ao_ba_om, "ASM at Vietnamese Wikipedia", "CC BY-SA 3.0", "Ao Bà Om, Trà Vinh.jpg");
            case "dinh_doc_lap" -> new ExplorePhoto(R.drawable.explore_dinh_doc_lap, "Yesvn123", "CC BY-SA 4.0", "Dinh Độc Lập vào nàm 2024.jpg");
            case "chua_cau" -> new ExplorePhoto(R.drawable.explore_chua_cau, "User dalbera on Flickr", "CC BY 1.0", "Cau Nhat Ban.jpg");
            case "ho_hoa_binh" -> new ExplorePhoto(R.drawable.explore_ho_hoa_binh, "Northern Vietnam", "CC BY-SA 4.0", "Hồ thủy điện Hòa Bình.jpg");
            case "bai_sau" -> new ExplorePhoto(R.drawable.explore_bai_sau, "Prenn", "CC BY-SA 3.0", "Bãi sau, Vũng Tàu, 02 2014.JPG");
            case "dong_xuan" -> new ExplorePhoto(R.drawable.explore_dong_xuan, "NKSTTSSHNVN", "CC BY-SA 4.0", "Chợ Đồng Xuân - NKS.jpg");
            case "ga_da_lat" -> new ExplorePhoto(R.drawable.explore_ga_da_lat, "Diane Selwyn", "CC BY-SA 3.0", "Da Lat train station 37.jpg");
            case "cai_rang" -> new ExplorePhoto(R.drawable.explore_cai_rang, "Bùi Thụy Đào Nguyên", "CC BY-SA 3.0", "Một cảnh ở chợ nổi Cái Răng.jpg");
            case "chua_doi" -> new ExplorePhoto(R.drawable.explore_chua_doi, "Tran Anh Khoa (zorrotran)", "CC BY-SA 2.0", "Chùa Dơi (45715215502).jpg");
            case "cho_da_lat" -> new ExplorePhoto(R.drawable.explore_cho_da_lat, "Roy Kim", "CC BY 2.0", "Dalat market, Vietnam.jpg");
            case "ganh_da_dia" -> new ExplorePhoto(R.drawable.explore_ganh_da_dia, "Đông Hồ", "CC BY-SA 2.0", "Gành Đá Đĩa.jpg");
            case "dong_ba" -> new ExplorePhoto(R.drawable.explore_dong_ba, "Chainwit.", "CC BY 4.0", "Dong Ba Market (Hue) - Chợ Đông Ba Huế (July 2024) - img 03.jpg");
            case "thanh_co_quang_tri" -> new ExplorePhoto(R.drawable.explore_thanh_co_quang_tri, "Bùi Thụy Đào Nguyên", "CC BY-SA 3.0", "Thành cổ Quảng Trị 4.jpg");
            case "tram_chim" -> new ExplorePhoto(R.drawable.explore_tram_chim, "Quoilp at Vietnamese Wikipedia", "CC BY-SA 3.0", "Đồng cỏ và chim nước.jpg");
            case "den_do" -> new ExplorePhoto(R.drawable.explore_den_do, "Cuong PM", "CC BY 2.0", "Đền Đô - 14061132515.jpg");
            case "cat_tien" -> new ExplorePhoto(R.drawable.explore_cat_tien, "Roy Bateman", "CC BY-SA 4.0", "BenCu forest structure 23Dec08.jpg");
            case "mui_ca_mau" -> new ExplorePhoto(R.drawable.explore_mui_ca_mau, "Colalumbu", "CC BY-SA 4.0", "Muicamau.jpg");
            case "phong_nha" -> new ExplorePhoto(R.drawable.explore_phong_nha, "Bùi Thụy Đào Nguyên", "CC BY-SA 3.0", "Vào động Phong Nha.jpg");
            case "u_minh_thuong" -> new ExplorePhoto(R.drawable.explore_u_minh_thuong, "Mongnt", "CC BY-SA 3.0", "RungUMinh1.jpg");
            case "xuan_thuy" -> new ExplorePhoto(R.drawable.explore_xuan_thuy, "Kristinvafranzi", "CC BY-SA 3.0", "Landscape Xuan Thuy National Park.JPG");
            case "doi_cat_mui_ne" -> new ExplorePhoto(R.drawable.explore_doi_cat_mui_ne, "Vyacheslav Argenberg", "CC BY 4.0", "Vietnam, Mui Ne sand dune.jpg");
            case "vinh_vinh_hy" -> new ExplorePhoto(R.drawable.explore_vinh_vinh_hy, "Karel Bilek", "CC BY-SA 4.0", "Vinh hy bay.jpg");
            case "mu_cang_chai" -> new ExplorePhoto(R.drawable.explore_mu_cang_chai, "VŨ HÙNG", "CC BY-SA 3.0", "KHAU PHẠ MÙA NƯỚC ĐỔ - panoramio.jpg");
            default -> null;
        };
        if (photo != null) return photo;
        TravelCatalog.Destination city = TravelCatalog.destination(key);
        return city == null ? null : new ExplorePhoto(city.image(), city.author(), city.license(), city.file());
    }
}
