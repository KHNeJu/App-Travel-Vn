package vn.travelvn.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

final class PlaceFeed {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final String OSM_CREDIT = "© OpenStreetMap contributors";
    private static final String OSM_COPYRIGHT = "https://www.openstreetmap.org/copyright";
    private static final int INK = TravelScreen.INK, BLUE = TravelScreen.BLUE, WHITE = TravelScreen.WHITE,
            PALE = TravelScreen.PALE, MUTED = TravelScreen.MUTED;
    private final TravelScreen host;
    private final LinearLayout panel;
    private final String area;
    private final String category;
    private final String query;
    private final boolean hotel;
    private final String previewTitle;
    private final Runnable openAll;
    private final List<Place> places = new ArrayList<>();
    private volatile boolean detached;
    private boolean offline;
    private boolean topRated;
    private boolean showAll;
    private boolean expanded;
    private int pageSize = 18;

    private static final class Place {
        final JSONObject data;
        final List<ImageView> images = new ArrayList<>();
        Bitmap photo;
        Place(JSONObject data) { this.data = data; }
        String title() { return data.optString("title"); }
        ExplorePhoto illustration() { return ExplorePhoto.lookup(data.optString("illustration")); }
        String area() { return data.optString("area"); }
        boolean hasLocation() { return data.has("lat") && data.has("lon"); }
        boolean osm() { return data.optBoolean("osm"); }
        double rating() { return data.optDouble("rating", 0); }
        int reviews() { return data.optInt("reviewCount"); }
    }

    PlaceFeed(TravelScreen host, LinearLayout panel, String area, String category, String query) {
        this(host, panel, area, category, query, "", null);
    }

    PlaceFeed(TravelScreen host, LinearLayout panel, String area, String category, String query,
              String previewTitle, Runnable openAll) {
        this.host = host;
        this.panel = panel;
        this.area = area;
        this.category = category;
        this.query = query;
        this.previewTitle = previewTitle;
        this.openAll = openAll;
        hotel = "Chỗ ở".equals(category);
        panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { detached = false; }
            @Override public void onViewDetachedFromWindow(View view) { detached = true; }
        });
        load();
    }

    private void load() {
        panel.removeAllViews();
        places.clear();
        offline = false;
        if (!TravelCloud.available(host)) {
            loadOffline();
            return;
        }
        List<Place> batch = new ArrayList<>();
        TextView status = host.label(panel, "Đang tìm địa điểm…", 14, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        new Thread(() -> {
            try {
                JSONArray results = TravelCloud.backend(host, "/api/places", new JSONObject()
                        .put("province", area).put("category", category).put("query", query)).getJSONArray("places");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject place = results.optJSONObject(i);
                    if (place != null) batch.add(new Place(place.put("area", area)));
                }
                host.runOnUiThread(() -> {
                    if (!current()) return;
                    if (batch.isEmpty()) {
                        unavailable("Chưa tìm thấy địa điểm. Bạn thử đổi từ khóa hoặc khu vực nhé.", false);
                        return;
                    }
                    places.addAll(batch);
                    render();
                });
                for (Place place : batch) {
                    if (detached) break;
                    Bitmap photo = ProvinceActivity.image(place.data.optString("photoUrl"));
                    if (photo == null) continue;
                    host.runOnUiThread(() -> {
                        if (!current()) { photo.recycle(); return; }
                        place.photo = photo;
                        for (ImageView view : place.images) view.setImageBitmap(photo);
                    });
                }
            } catch (Exception error) {
                host.runOnUiThread(() -> {
                    if (current()) loadOffline();
                });
            }
        }, "travel-places").start();
    }

    private void loadOffline() {
        offline = true;
        places.clear();
        panel.removeAllViews();
        host.label(panel, "Đang tìm địa điểm…", 14, MUTED, false)
                .setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        new Thread(() -> {
            try {
                List<Place> results = readOffline(area, category, query);
                host.runOnUiThread(() -> {
                    if (!current()) return;
                    places.addAll(results);
                    if (places.isEmpty()) unavailable("Chưa có địa điểm phù hợp. Hãy thử danh mục Tất cả hoặc một tên khác.", false);
                    else render();
                });
            } catch (IOException | JSONException | NumberFormatException error) {
                host.runOnUiThread(() -> {
                    if (current()) unavailable("Chưa đọc được danh mục địa điểm. Bạn có thể thử lại.", true);
                });
            }
        }, "travel-local-places").start();
    }

    private List<Place> readOffline(String area, String category, String query) throws IOException, JSONException {
        String key = areaKey(area);
        String search = TravelCatalog.fold(query.trim());
        List<Place> localPlaces = new ArrayList<>();
        List<Place> otherAreas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                host.getAssets().open("places-vn.tsv"), StandardCharsets.UTF_8))) {
            String line;
            String currentArea = "";
            boolean local = false;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                if (fields.length < 3) continue;
                if (!fields[0].equals(currentArea)) {
                    currentArea = fields[0];
                    local = areaKey(currentArea).equals(key);
                }
                if (!local && (search.length() < 3 || otherAreas.size() >= 80)) continue;
                if (!matchesCategory(category, fields[1]) || !TravelCatalog.fold(fields[2]).contains(search)) continue;
                JSONObject data = new JSONObject().put("title", fields[2]).put("category", fields[1]).put("area", fields[0])
                        .put("url", "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(fields[2] + ", " + fields[0] + ", Việt Nam"));
                if (fields.length > 3) data.put("description", fields[3]);
                if (fields.length > 4) data.put("illustration", fields[4]);
                if (fields.length > 5) data.put("osm", "osm".equals(fields[5]));
                if (fields.length > 7 && !fields[6].isEmpty() && !fields[7].isEmpty()) {
                    data.put("lat", Double.parseDouble(fields[6])).put("lon", Double.parseDouble(fields[7]));
                }
                (local ? localPlaces : otherAreas).add(new Place(data));
            }
        }
        return localPlaces.isEmpty() ? otherAreas : localPlaces;
    }

    private static String areaKey(String name) {
        String key = TravelCatalog.fold(name);
        return switch (key) {
            case "thua thien hue" -> "hue";
            case "ho chi minh", "thanh pho ho chi minh" -> "tp. ho chi minh";
            default -> key;
        };
    }

    private static boolean matchesCategory(String category, String tags) {
        return switch (category) {
            case "Tất cả" -> true;
            case "Thắng cảnh" -> tags.contains("Thiên nhiên") || tags.contains("Biển");
            case "Địa danh", "Du lịch" -> tags.contains("Tham quan") || tags.contains("Lịch sử")
                    || tags.contains("Thiên nhiên") || tags.contains("Biển");
            default -> tags.contains(category);
        };
    }

    private boolean current() {
        return panel.isAttachedToWindow() && !host.isFinishing() && !host.isDestroyed();
    }

    private void unavailable(String message, boolean retry) {
        panel.removeAllViews();
        LinearLayout notice = host.card(panel);
        host.label(notice, message, 14, MUTED, false);
        notice.addView(host.button("Tìm trên Google Maps", PALE, BLUE, this::openSearch));
        if (retry) notice.addView(host.button("Thử lại", WHITE, BLUE, this::load));
    }

    private void openSearch() {
        String term = query.isEmpty() ? ("Tất cả".equals(category) ? "địa điểm du lịch" : category) : query;
        open("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(term + " tại " + area + ", Việt Nam"));
    }

    private void render() {
        panel.removeAllViews();
        for (Place place : places) place.images.clear();
        LinearLayout headline = new LinearLayout(host);
        headline.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading = host.column();
        boolean preview = !previewTitle.isEmpty() && !expanded;
        host.label(heading, preview ? previewTitle : hotel ? "Khách sạn tại " + area
                        : query.isEmpty() ? "Khám phá quanh " + area : "Kết quả tìm kiếm",
                22, INK, true);
        String caption = places.stream().noneMatch(place -> areaKey(place.area()).equals(areaKey(area)))
                ? "Kết quả ở khu vực khác" : area;
        host.label(heading, caption, 14, MUTED, false);
        headline.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        headline.addView(host.button(preview && hotel ? "Tất cả" : "Bản đồ", TravelScreen.PAGE, BLUE,
                preview && hotel ? this::allPlaces : this::openSearch));
        panel.addView(headline);
        host.space(panel, 12);
        if (preview || (offline && query.isEmpty() && !expanded)) {
            List<Place> featured = new ArrayList<>(places);
            if (offline) featured.sort(Comparator.comparing(place -> place.illustration() == null));
            carousel(panel, featured.subList(0, Math.min(8, featured.size())), hotel ? 218 : 152, hotel ? 147 : 185);
            if (preview) {
                if (!hotel) panel.addView(host.button("Xem tất cả địa điểm", TravelScreen.PAGE, BLUE, this::allPlaces));
                sourceCredit(panel);
                return;
            }
            host.space(panel, 20);
            host.label(panel, "Tất cả địa điểm", 20, INK, true);
        }
        List<Place> rated = new ArrayList<>(places);
        rated.sort(Comparator.comparingDouble(Place::rating).thenComparingInt(Place::reviews).reversed());
        if (offline) {
            LinearLayout list = host.column();
            panel.addView(list);
            TextView more = host.button("Xem thêm địa điểm", TravelScreen.PAGE, BLUE, () -> {});
            more.setOnClickListener(view -> {
                int start = pageSize;
                pageSize += 18;
                for (int i = start; i < Math.min(pageSize, places.size()); i++) offlineRow(list, places.get(i));
                view.setVisibility(pageSize < places.size() ? View.VISIBLE : View.GONE);
            });
            for (int i = 0; i < Math.min(pageSize, places.size()); i++) offlineRow(list, places.get(i));
            panel.addView(more);
            more.setVisibility(pageSize < places.size() ? View.VISIBLE : View.GONE);
            sourceCredit(panel);
            return;
        }
        if (hotel) {
            LinearLayout sort = new LinearLayout(host);
            addSort(sort, "Phù hợp nhất", false);
            addSort(sort, "Đánh giá cao", true);
            panel.addView(sort);
            host.space(panel, 16);
            for (Place place : topRated ? rated : places) {
                panel.addView(tile(place, true));
                host.space(panel, 21);
            }
            return;
        }
        carousel(panel, places, 156, 185);
        rated.removeIf(place -> place.rating() <= 0);
        if (!rated.isEmpty()) {
            host.space(panel, 23);
            LinearLayout header = new LinearLayout(host);
            header.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout title = host.column();
            host.label(title, "Được đánh giá cao", 21, INK, true);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            if (rated.size() > 4) header.addView(host.button(showAll ? "Thu gọn" : "Tất cả", WHITE, BLUE,
                    () -> { showAll = !showAll; render(); }));
            panel.addView(header);
            for (int i = 0; i < Math.min(rated.size(), showAll ? rated.size() : 4); i++) rank(rated.get(i), i + 1);
        }
    }

    private void allPlaces() {
        if (openAll != null) openAll.run();
        else {
            expanded = true;
            render();
        }
    }

    private void sourceCredit(LinearLayout parent) {
        if (offline && places.stream().anyMatch(Place::osm)) osmCredit(parent);
        if (!offline) host.label(parent, "Google Maps", 12, MUTED, true);
    }

    private void carousel(LinearLayout parent, List<Place> entries, int width, int height) {
        HorizontalScrollView strip = new HorizontalScrollView(host);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout tiles = new LinearLayout(host);
        for (Place place : entries) {
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(host.dp(width), -2);
            size.setMarginEnd(host.dp(12));
            tiles.addView(tile(place, height, false), size);
        }
        strip.addView(tiles);
        parent.addView(strip);
    }

    private void osmCredit(LinearLayout parent) {
        TextView credit = host.label(parent, OSM_CREDIT, 11, MUTED, false);
        credit.setPaintFlags(credit.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        credit.setOnClickListener(view -> open(OSM_COPYRIGHT));
        credit.setFocusable(true);
    }

    private void offlineRow(LinearLayout parent, Place place) {
        LinearLayout row = new LinearLayout(host);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10));
        row.setBackground(host.round(WHITE, 18));
        row.addView(photo(place, 60), new LinearLayout.LayoutParams(host.dp(60), host.dp(60)));
        LinearLayout copy = host.column();
        copy.setPadding(host.dp(12), 0, host.dp(8), 0);
        host.label(copy, place.title(), 16, INK, true);
        host.label(copy, place.area() + " · " + metadata(place), 13, MUTED, false);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        host.label(row, "›", 20, BLUE, true);
        row.setFocusable(true);
        row.setOnClickListener(v -> details(place));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        host.space(parent, 8);
    }

    private void addSort(LinearLayout row, String name, boolean rated) {
        TextView button = host.button(name, topRated == rated ? INK : WHITE, topRated == rated ? WHITE : INK,
                () -> { topRated = rated; render(); });
        button.setSelected(topRated == rated);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-2, -2);
        size.setMarginEnd(host.dp(8));
        row.addView(button, size);
    }

    private LinearLayout tile(Place place, boolean wide) {
        return tile(place, wide ? 173 : 185, wide);
    }

    private LinearLayout tile(Place place, int height, boolean wide) {
        LinearLayout tile = host.column();
        tile.addView(photo(place, height), new LinearLayout.LayoutParams(-1, host.dp(height)));
        credits(tile, place.data.optJSONArray("authors"), "displayName", "uri");
        host.space(tile, 6);
        TextView title = host.label(tile, place.title(), wide ? 20 : 16, INK, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        host.label(tile, offline ? place.area() : metadata(place), 13, MUTED, false);
        credits(tile, place.data.optJSONArray("attributions"), "provider", "providerUri");
        tile.setFocusable(true);
        tile.setOnClickListener(v -> details(place));
        if (wide) tile.addView(host.button("Xem giá theo ngày", WHITE, BLUE, () -> hotelPrices(place)));
        return tile;
    }

    private void rank(Place place, int position) {
        LinearLayout row = new LinearLayout(host);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, host.dp(9), 0, host.dp(9));
        TextView number = host.label(row, Integer.toString(position), 22, BLUE, true);
        number.setWidth(host.dp(30));
        LinearLayout thumbnail = host.column();
        thumbnail.addView(photo(place, 61), new LinearLayout.LayoutParams(host.dp(61), host.dp(61)));
        credits(thumbnail, place.data.optJSONArray("authors"), "displayName", "uri");
        row.addView(thumbnail, new LinearLayout.LayoutParams(host.dp(67), -2));
        LinearLayout text = host.column();
        text.setPadding(host.dp(10), 0, 0, 0);
        host.label(text, place.title(), 16, INK, true);
        host.label(text, metadata(place), 13, MUTED, false);
        credits(text, place.data.optJSONArray("attributions"), "provider", "providerUri");
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        row.setFocusable(true);
        row.setOnClickListener(v -> details(place));
        panel.addView(row);
        View divider = new View(host);
        divider.setBackgroundColor(0xFFE0E9F1);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, host.dp(1)));
    }

    private View photo(Place place, int height) {
        FrameLayout frame = new FrameLayout(host);
        frame.setBackground(host.round(PALE, height < 70 ? 14 : 21));
        frame.setClipToOutline(true);
        TravelIcon placeholder = new TravelIcon(host, 2, MUTED);
        frame.addView(placeholder, new FrameLayout.LayoutParams(host.dp(25), host.dp(25), Gravity.CENTER));
        ImageView photo = new ImageView(host);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (place.photo != null) photo.setImageBitmap(place.photo);
        ExplorePhoto illustration = place.illustration();
        if (illustration != null) ExplorePhoto.bind(photo, illustration.image());
        if (!offline && !place.osm()) place.images.add(photo);
        frame.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    private String metadata(Place place) {
        if (offline || !place.data.optString("category").isEmpty()) return place.data.optString("category").replace("|", " · ");
        List<String> parts = new ArrayList<>();
        if (place.rating() > 0) parts.add(String.format(VI, "★ %.1f/5", place.rating()));
        if (place.reviews() > 0) parts.add(NumberFormat.getIntegerInstance(VI).format(place.reviews()) + " đánh giá");
        String price = price(place.data);
        if (!price.isEmpty() && !hotel) parts.add(price);
        return parts.isEmpty() ? area : String.join(" · ", parts);
    }

    private static String price(JSONObject place) {
        JSONObject range = place.optJSONObject("priceRange");
        if (range != null) {
            String low = money(range.optJSONObject("startPrice"));
            String high = money(range.optJSONObject("endPrice"));
            if (!low.isEmpty() && !high.isEmpty()) return low.equals(high) ? low : low + " – " + high;
            if (!low.isEmpty()) return "Từ " + low;
            if (!high.isEmpty()) return "Đến " + high;
        }
        return switch (place.optString("priceLevel")) {
            case "PRICE_LEVEL_FREE" -> "Miễn phí";
            case "PRICE_LEVEL_INEXPENSIVE" -> "Giá thấp";
            case "PRICE_LEVEL_MODERATE" -> "Giá vừa";
            case "PRICE_LEVEL_EXPENSIVE" -> "Giá cao";
            case "PRICE_LEVEL_VERY_EXPENSIVE" -> "Giá rất cao";
            default -> "";
        };
    }

    private static String money(JSONObject amount) {
        if (amount == null) return "";
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance(VI);
            Currency currency = Currency.getInstance(amount.getString("currencyCode"));
            format.setCurrency(currency);
            int fraction = Math.max(0, currency.getDefaultFractionDigits());
            format.setMinimumFractionDigits(fraction);
            format.setMaximumFractionDigits(fraction);
            return format.format(new BigDecimal(amount.optString("units", "0"))
                    .add(BigDecimal.valueOf(amount.optLong("nanos"), 9)));
        } catch (Exception ignored) { return ""; }
    }

    private void details(Place place) {
        Dialog dialog = new Dialog(host, android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout root = host.column();
        root.setBackgroundColor(WHITE);
        ScrollView scroll = new ScrollView(host);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout story = host.column();
        scroll.addView(story);

        FrameLayout cover = new FrameLayout(host);
        boolean hasPhoto = place.photo != null || place.illustration() != null || !place.data.optString("photoUrl").isEmpty();
        if (hasPhoto) {
            View image = photo(place, 272);
            image.setClipToOutline(false);
            cover.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else cover.setBackgroundColor(PALE);
        story.addView(cover, new LinearLayout.LayoutParams(-1, host.dp(hasPhoto ? 272 : 70)));
        TextView back = host.button("←", WHITE, INK, dialog::dismiss);
        back.setContentDescription("Quay lại");
        placeControl(cover, back, Gravity.START);
        SharedPreferences store = host.getSharedPreferences("travel-vn", Context.MODE_PRIVATE);
        String bookmarkKey = "bookmark:" + place.area() + ":" + place.title();
        TextView bookmark = host.button("☆", WHITE, INK, () -> {});
        bookmark.setOnClickListener(view -> {
            store.edit().putBoolean(bookmarkKey, !store.getBoolean(bookmarkKey, false)).apply();
            updateBookmark(bookmark, store.getBoolean(bookmarkKey, false));
        });
        updateBookmark(bookmark, store.getBoolean(bookmarkKey, false));
        placeControl(cover, bookmark, Gravity.END);

        LinearLayout detail = host.column();
        detail.setPadding(host.dp(18), host.dp(18), host.dp(18), host.dp(24));
        story.addView(detail);
        String categoryLabel = place.data.optString("category", category);
        if ("Tất cả".equals(categoryLabel)) categoryLabel = "Địa điểm";
        TextView eyebrow = host.label(detail, categoryLabel.split("\\|")[0].toUpperCase(VI)
                + " · " + place.area().toUpperCase(VI), 12, BLUE, true);
        eyebrow.setLetterSpacing(.08f);
        host.space(detail, 5);
        host.label(detail, place.title(), 29, INK, true);
        String address = place.data.optString("address");
        if (!address.isEmpty()) host.label(detail, address, 14, MUTED, false);
        if (place.rating() > 0) host.label(detail, metadata(place), 14, INK, true);
        String description = place.data.optString("description");
        host.space(detail, 23);
        host.label(detail, "Giới thiệu", 20, INK, true);
        host.space(detail, 7);
        TextView intro = host.label(detail, description.isEmpty() ? "Địa điểm thuộc danh mục "
                + categoryLabel.split("\\|")[0].toLowerCase(VI) + " tại " + place.area() + "." : description, 15, INK, false);
        intro.setLineSpacing(host.dp(3), 1);
        ExplorePhoto illustration = place.illustration();
        if (illustration != null) detail.addView(host.button("Ảnh · " + illustration.author() + " · " + illustration.license(),
                WHITE, BLUE, () -> open(illustration.url())));
        credits(detail, place.data.optJSONArray("authors"), "displayName", "uri");
        if (place.osm()) osmCredit(detail);
        else if (!offline) host.label(detail, "Google Maps", 12, MUTED, true);
        String phone = place.data.optString("phone");
        if (!phone.isEmpty()) host.label(detail, "Điện thoại: " + phone, 14, INK, false);
        JSONArray hours = place.data.optJSONArray("hours");
        if (hours != null && hours.length() > 0) {
            host.space(detail, 10);
            host.label(detail, "Giờ mở cửa thường lệ", 14, INK, true);
            for (int i = 0; i < hours.length(); i++) host.label(detail, hours.optString(i), 13, MUTED, false);
        }
        credits(detail, place.data.optJSONArray("attributions"), "provider", "providerUri");
        String website = place.data.optString("website");
        if (!website.isEmpty()) detail.addView(host.button("Trang web", WHITE, BLUE, () -> open(website)));
        if (hotel || categoryLabel.contains("Chỗ ở")) detail.addView(host.button(offline ? "Tra giá phòng trên Google" : "Xem giá theo ngày",
                PALE, BLUE, () -> hotelPrices(place)));
        detail.addView(host.button("Mở bản đồ", WHITE, BLUE, () -> open(place.data.optString("url"))));
        host.space(detail, 22);
        host.label(detail, "Đánh giá", 20, INK, true);
        host.space(detail, 5);
        TextView review = host.label(detail, "", 14, MUTED, false);
        String reviewKey = "reviews:" + TravelCloud.session(host).optString("uid");
        String reviewPlace = place.title() + " · " + place.area();
        updateReview(review, store, reviewKey, reviewPlace);
        detail.addView(host.button("Viết đánh giá", PALE, INK, () -> writeReview(review, store, reviewKey, reviewPlace)));
        LinearLayout footer = host.column();
        footer.setBackgroundColor(WHITE);
        footer.setElevation(host.dp(4));
        footer.setPadding(host.dp(18), host.dp(9), host.dp(18), host.dp(10));
        footer.addView(host.button("Thêm vào chuyến", INK, WHITE, () -> {
            dialog.dismiss();
            if (host instanceof MainActivity main) main.addPlaceToTrip(place.title(), place.area());
            else host.startActivity(new Intent(host, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("addPlace", place.title()).putExtra("placeArea", place.area()));
        }));
        root.addView(footer);
        dialog.setContentView(root);
        dialog.show();
        if (Build.VERSION.SDK_INT >= 30) {
            dialog.getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }
        new Thread(() -> {
            try {
                List<Place> nearby = readOffline(place.area(), "Tất cả", "");
                nearby.removeIf(other -> TravelCatalog.fold(other.title()).equals(TravelCatalog.fold(place.title())));
                if (place.hasLocation()) {
                    nearby.removeIf(other -> !other.hasLocation() || distance(place, other) > 6000);
                    nearby.sort(Comparator.comparing((Place other) -> other.illustration() == null)
                            .thenComparingDouble(other -> distance(place, other)));
                }
                host.runOnUiThread(() -> {
                    if (dialog.isShowing() && !nearby.isEmpty()) addNearby(detail, place, dialog, nearby);
                });
            } catch (IOException | JSONException | NumberFormatException ignored) {
                // The main place remains available if the local suggestions cannot be loaded.
            }
        }, "travel-nearby-places").start();
    }

    private void placeControl(FrameLayout cover, TextView button, int gravity) {
        button.setTextSize(22);
        FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(host.dp(48), host.dp(48), Gravity.TOP | gravity);
        size.setMargins(host.dp(12), host.dp(11), host.dp(12), 0);
        cover.addView(button, size);
    }

    private void updateBookmark(TextView button, boolean saved) {
        button.setText(saved ? "★" : "☆");
        button.setContentDescription(saved ? "Bỏ lưu địa điểm" : "Lưu địa điểm");
        button.setSelected(saved);
    }

    private JSONArray reviews(SharedPreferences store, String key) {
        try { return new JSONArray(store.getString(key, "[]")); }
        catch (JSONException error) { return new JSONArray(); }
    }

    private void updateReview(TextView review, SharedPreferences store, String key, String name) {
        JSONArray values = reviews(store, key);
        String prefix = name + "\n";
        for (int i = values.length() - 1; i >= 0; i--) {
            String value = values.optString(i);
            if (!value.startsWith(prefix)) continue;
            review.setText("Đánh giá của bạn · Lưu trên thiết bị\n" + value.substring(prefix.length()));
            return;
        }
        review.setText("Bạn chưa viết đánh giá cho địa điểm này.");
    }

    private void writeReview(TextView review, SharedPreferences store, String key, String name) {
        LinearLayout editor = host.column();
        editor.setPadding(host.dp(18), 0, host.dp(18), 0);
        EditText input = host.field(editor, "Cảm nhận của bạn", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        AlertDialog dialog = new AlertDialog.Builder(host).setTitle("Đánh giá riêng tư").setView(editor)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) { input.setError("Nhập cảm nhận của bạn"); return; }
            if (!store.edit().putString(key, reviews(store, key).put(name + "\n" + text).toString()).commit()) {
                input.setError("Chưa lưu được đánh giá");
                return;
            }
            updateReview(review, store, key, name);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void addNearby(LinearLayout detail, Place current, Dialog dialog, List<Place> nearby) {
        host.space(detail, 23);
        host.label(detail, current.hasLocation() ? "Gần đây" : "Cùng khu vực", 20, INK, true);
        host.space(detail, 9);
        HorizontalScrollView strip = new HorizontalScrollView(host);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(host);
        for (Place place : nearby.subList(0, Math.min(6, nearby.size()))) {
            LinearLayout tile = tile(place, 106, false);
            if (current.hasLocation()) host.label(tile, String.format(VI, "Cách %.1f km", distance(current, place) / 1000),
                    12, MUTED, false);
            tile.setOnClickListener(view -> { dialog.dismiss(); details(place); });
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(host.dp(140), -2);
            size.setMarginEnd(host.dp(10));
            cards.addView(tile, size);
        }
        strip.addView(cards);
        detail.addView(strip);
    }

    private static double distance(Place a, Place b) {
        float[] distance = new float[1];
        Location.distanceBetween(a.data.optDouble("lat"), a.data.optDouble("lon"),
                b.data.optDouble("lat"), b.data.optDouble("lon"), distance);
        return distance[0];
    }

    private void credits(LinearLayout parent, JSONArray items, String name, String uri) {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject credit = items.optJSONObject(i);
            if (credit == null || credit.optString(name).trim().isEmpty()) continue;
            String url = credit.optString(uri);
            TextView text = host.label(parent, credit.optString(name), 11, MUTED, false);
            if (url.startsWith("https://")) {
                text.setPaintFlags(text.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                text.setFocusable(true);
                text.setOnClickListener(v -> open(url));
            }
        }
    }

    private void hotelPrices(Place place) {
        open("https://www.google.com/travel/hotels?q=" + Uri.encode(place.title() + " " + place.area() + ", Việt Nam"));
    }

    private void open(String address) {
        if (!address.startsWith("https://")) return;
        try { host.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address))); }
        catch (ActivityNotFoundException error) {
            Toast.makeText(host, "Thiết bị chưa có ứng dụng để mở liên kết.", Toast.LENGTH_SHORT).show();
        }
    }
}
