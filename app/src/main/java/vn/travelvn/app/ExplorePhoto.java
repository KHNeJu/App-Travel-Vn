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
                if (view.isAttachedToWindow()) view.setImageBitmap(result);
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
            default -> null;
        };
        if (photo != null) return photo;
        TravelCatalog.Destination city = TravelCatalog.destination(key);
        return city == null ? null : new ExplorePhoto(city.image(), city.author(), city.license(), city.file());
    }
}
