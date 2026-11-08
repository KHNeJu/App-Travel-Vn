package vn.travelvn.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

final class PlaceFeed {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final int INK = TravelScreen.INK, BLUE = TravelScreen.BLUE, WHITE = TravelScreen.WHITE,
            PALE = TravelScreen.PALE, MUTED = TravelScreen.MUTED;
    private final TravelScreen host;
    private final LinearLayout panel;
    private final String area;
    private final String category;
    private final String query;
    private final boolean hotel;
    private final List<Place> places = new ArrayList<>();
    private volatile boolean detached;
    private boolean topRated;
    private boolean showAll;

    private static final class Place {
        final JSONObject data;
        final List<ImageView> images = new ArrayList<>();
        Bitmap photo;
        Place(JSONObject data) { this.data = data; }
        String title() { return data.optString("title"); }
        double rating() { return data.optDouble("rating", 0); }
        int reviews() { return data.optInt("reviewCount"); }
    }

    PlaceFeed(TravelScreen host, LinearLayout panel, String area, String category, String query) {
        this.host = host;
        this.panel = panel;
        this.area = area;
        this.category = category;
        this.query = query;
        hotel = "Chỗ ở".equals(category);
        panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { detached = false; }
            @Override public void onViewDetachedFromWindow(View view) { detached = true; }
        });
        load();
    }

    private void load() {
        panel.removeAllViews();
        List<Place> batch = new ArrayList<>();
        TextView status = host.label(panel, "Đang tìm địa điểm…", 14, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        new Thread(() -> {
            try {
                JSONArray results = TravelCloud.backend(host, "/api/places", new JSONObject()
                        .put("province", area).put("category", category).put("query", query)).getJSONArray("places");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject place = results.optJSONObject(i);
                    if (place != null) batch.add(new Place(place));
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
                    if (current()) unavailable(error instanceof TravelCloud.ApiError ? error.getMessage()
                            : "Chưa tải được địa điểm.", true);
                });
            }
        }, "travel-places").start();
    }

    private boolean current() {
        return panel.isAttachedToWindow() && !host.isFinishing() && !host.isDestroyed();
    }

    private void unavailable(String message, boolean retry) {
        panel.removeAllViews();
        LinearLayout notice = host.card(panel);
        host.label(notice, message, 14, MUTED, false);
        notice.addView(host.button("Tìm trên Google Maps", PALE, BLUE, this::openSearch));
        if (retry && TravelCloud.available(host)) notice.addView(host.button("Thử lại", WHITE, BLUE, this::load));
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
        host.label(heading, hotel ? "Khách sạn tại " + area : query.isEmpty() ? "Khám phá quanh " + area : "Kết quả tìm kiếm",
                22, INK, true);
        host.label(heading, "Google Maps", 12, MUTED, true);
        headline.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        headline.addView(host.button("Bản đồ", WHITE, BLUE, this::openSearch));
        panel.addView(headline);
        host.space(panel, 12);
        List<Place> rated = new ArrayList<>(places);
        rated.sort(Comparator.comparingDouble(Place::rating).thenComparingInt(Place::reviews).reversed());
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
        HorizontalScrollView strip = new HorizontalScrollView(host);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout tiles = new LinearLayout(host);
        for (Place place : places) {
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(host.dp(156), -2);
            size.setMarginEnd(host.dp(12));
            tiles.addView(tile(place, false), size);
        }
        strip.addView(tiles);
        panel.addView(strip);
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

    private void addSort(LinearLayout row, String name, boolean rated) {
        TextView button = host.button(name, topRated == rated ? INK : WHITE, topRated == rated ? WHITE : INK,
                () -> { topRated = rated; render(); });
        button.setSelected(topRated == rated);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-2, -2);
        size.setMarginEnd(host.dp(8));
        row.addView(button, size);
    }

    private LinearLayout tile(Place place, boolean wide) {
        LinearLayout tile = host.column();
        tile.addView(photo(place, wide ? 173 : 185), new LinearLayout.LayoutParams(-1, host.dp(wide ? 173 : 185)));
        credits(tile, place.data.optJSONArray("authors"), "displayName", "uri");
        host.space(tile, 6);
        host.label(tile, place.title(), wide ? 20 : 16, INK, true);
        host.label(tile, metadata(place), 13, MUTED, false);
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
        place.images.add(photo);
        frame.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    private String metadata(Place place) {
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
        LinearLayout detail = host.column();
        detail.setPadding(host.dp(20), 0, host.dp(20), host.dp(12));
        if (place.photo != null) {
            detail.addView(photo(place, 160), new LinearLayout.LayoutParams(-1, host.dp(160)));
            credits(detail, place.data.optJSONArray("authors"), "displayName", "uri");
        }
        host.label(detail, "Google Maps", 12, MUTED, true);
        host.label(detail, metadata(place), 14, INK, true);
        host.label(detail, place.data.optString("address", area), 14, MUTED, false);
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
        if (hotel) detail.addView(host.button("Xem giá theo ngày", PALE, BLUE, () -> hotelPrices(place)));
        ScrollView scroll = new ScrollView(host);
        scroll.addView(detail);
        new AlertDialog.Builder(host).setTitle(place.title()).setView(scroll)
                .setNegativeButton("Đóng", null).setPositiveButton("Google Maps", (dialog, which) ->
                        open(place.data.optString("url"))).show();
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
        open("https://www.google.com/travel/hotels?q=" + Uri.encode(place.title() + " " + area + ", Việt Nam"));
    }

    private void open(String address) {
        if (!address.startsWith("https://")) return;
        try { host.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address))); }
        catch (ActivityNotFoundException error) {
            Toast.makeText(host, "Thiết bị chưa có ứng dụng để mở liên kết.", Toast.LENGTH_SHORT).show();
        }
    }
}
