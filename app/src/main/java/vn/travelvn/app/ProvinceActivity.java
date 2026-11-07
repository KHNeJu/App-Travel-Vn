package vn.travelvn.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

public final class ProvinceActivity extends TravelScreen {
    static final String[] CATEGORIES = {"Ăn uống", "Thắng cảnh", "Địa danh", "Du lịch", "Mua sắm", "Cà phê", "Lịch sử", "Chỗ ở"};
    private static final int[] CATEGORY_COLORS = {0xFFFFEEE3, 0xFFE5F5ED, 0xFFE6EFFF, 0xFFF0EAFF,
            0xFFFFECF1, 0xFFF5ECE4, 0xFFFFF3D5, 0xFFE3F4F7};
    static final String[] PROVINCES = {
            "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh", "Bến Tre", "Bình Định",
            "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau", "Cần Thơ", "Cao Bằng", "Đà Nẵng", "Đắk Lắk",
            "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Nội", "Hà Tĩnh",
            "Hải Dương", "Hải Phòng", "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum",
            "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định", "Nghệ An", "Ninh Bình", "Ninh Thuận",
            "Phú Thọ", "Phú Yên", "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng",
            "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang",
            "TP. Hồ Chí Minh", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
    };
    private String province = "";
    private String category = "";
    private int request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) provinces();
        else {
            province = state.getString("province", "");
            category = state.getString("category", "");
            if (province.isEmpty()) provinces();
            else if (category.isEmpty()) categories();
            else results(category);
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("province", province);
        state.putString("category", category);
    }

    @Override public void onBackPressed() {
        if (!category.isEmpty()) categories();
        else if (!province.isEmpty()) provinces();
        else super.onBackPressed();
    }

    private void provinces() {
        request++;
        province = category = "";
        screen("Khám phá 63 tỉnh/thành", "Danh mục điểm đến theo tên trước sắp xếp hành chính.");
        EditText search = field(body, "Tìm tỉnh hoặc thành phố", InputType.TYPE_CLASS_TEXT);
        LinearLayout list = column();
        body.addView(list);
        fill(list, "");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { fill(list, text.toString()); }
            @Override public void afterTextChanged(Editable text) {}
        });
    }

    private void fill(LinearLayout list, String query) {
        list.removeAllViews();
        String filter = TravelCatalog.fold(query);
        int shown = 0;
        for (String item : PROVINCES) {
            if (!TravelCatalog.fold(item).contains(filter)) continue;
            shown++;
            list.addView(button(item + "  ›", WHITE, INK, () -> { province = item; categories(); }));
            space(list, 6);
        }
        if (shown == 0) label(list, "Chưa tìm thấy tỉnh/thành phù hợp.", 14, MUTED, false);
    }

    private void categories() {
        request++;
        category = "";
        screen(province, "Chọn hạng mục để xem địa điểm, ảnh và thông tin từ Google Maps.");
        body.addView(button("Đổi tỉnh/thành", PALE, BLUE, this::provinces));
        space(body, 12);
        for (int i = 0; i < CATEGORIES.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int j = i; j < i + 2; j++) {
                String item = CATEGORIES[j];
                TextView tile = button(item + "  ›", CATEGORY_COLORS[j], INK, () -> results(item));
                tile.setMinHeight(dp(71));
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0, -2, 1);
                place.setMargins(dp(3), 0, dp(3), dp(8));
                row.addView(tile, place);
            }
            body.addView(row);
        }
    }

    private void results(String item) {
        category = item;
        String area = province.equals("Thừa Thiên Huế") ? "Huế" : province;
        int id = ++request;
        screen(item, province);
        LinearLayout controls = card(body);
        label(controls, "Google Maps", 13, INK, true);
        TextView status = label(controls, "Đang tìm địa điểm có ảnh…", 14, MUTED, false);
        controls.addView(button("Tìm trên Google Maps", PALE, BLUE, () -> open("https://www.google.com/maps/search/?api=1&query="
                + Uri.encode(item + " tại " + area + ", Việt Nam"))));
        controls.addView(button("Đổi hạng mục", WHITE, BLUE, this::categories));
        LinearLayout list = column();
        body.addView(list);
        new Thread(() -> {
            try {
                JSONObject response = TravelCloud.backend(this, "/api/places", new JSONObject().put("province", area).put("category", item));
                JSONArray places = response.getJSONArray("places");
                runOnUiThread(() -> {
                    if (!current(id)) return;
                    status.setText(places.length() == 0 ? "Chưa tìm thấy địa điểm có ảnh cho hạng mục này." : places.length() + " địa điểm có ảnh");
                });
                for (int i = 0; i < places.length(); i++) {
                    JSONObject place = places.optJSONObject(i);
                    if (place == null) continue;
                    Bitmap bitmap = image(place.optString("photoUrl"));
                    runOnUiThread(() -> {
                        if (current(id)) showPlace(list, place, bitmap);
                        else if (bitmap != null) bitmap.recycle();
                    });
                }
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!current(id)) return;
                    status.setText(error instanceof TravelCloud.ApiError ? error.getMessage() : "Chưa tải được địa điểm. Hãy thử lại.");
                    controls.addView(button("Thử lại", WHITE, BLUE, () -> results(item)));
                });
            }
        }, "travel-places").start();
    }

    private boolean current(int id) {
        return !isFinishing() && !isDestroyed() && request == id;
    }

    private void showPlace(LinearLayout list, JSONObject place, Bitmap bitmap) {
        LinearLayout card = card(list);
        if (bitmap != null) {
            ImageView photo = new ImageView(this);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setBackground(round(PALE, 21));
            photo.setClipToOutline(true);
            photo.setImageBitmap(bitmap);
            photo.setContentDescription("Ảnh " + place.optString("title"));
            card.addView(photo, new LinearLayout.LayoutParams(-1, dp(155)));
            credits(card, place.optJSONArray("authors"), "displayName", "uri");
        } else label(card, "Ảnh hiện chưa tải được", 12, MUTED, false);
        label(card, place.optString("title"), 19, INK, true);
        label(card, place.optString("address"), 14, MUTED, false);
        credits(card, place.optJSONArray("attributions"), "provider", "providerUri");
        card.addView(button("Mở trên Google Maps", PALE, BLUE, () -> open(place.optString("url"))));
    }

    private void credits(LinearLayout card, JSONArray items, String name, String uri) {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject credit = items.optJSONObject(i);
            if (credit == null || credit.optString(name).isEmpty()) continue;
            String url = credit.optString(uri);
            if (url.startsWith("https://")) card.addView(button(credit.optString(name), WHITE, BLUE, () -> open(url)));
            else label(card, credit.optString(name), 12, MUTED, false);
        }
    }

    private Bitmap image(String address) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(address);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || !uri.getHost().endsWith(".googleusercontent.com")) return null;
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(5500);
            connection.setReadTimeout(6500);
            if (connection.getResponseCode() != 200) return null;
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int count; (count = input.read(buffer)) >= 0; ) {
                    if (bytes.size() + count > 2_000_000) return null;
                    bytes.write(buffer, 0, count);
                }
                byte[] data = bytes.toByteArray();
                return BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        } catch (Exception error) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void open(String address) {
        if (!address.startsWith("https://")) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address))); }
        catch (ActivityNotFoundException error) {
            android.widget.Toast.makeText(this, "Thiết bị chưa có ứng dụng để mở liên kết.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
