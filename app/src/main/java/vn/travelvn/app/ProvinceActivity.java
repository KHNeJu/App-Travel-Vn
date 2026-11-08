package vn.travelvn.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        screen(item, province);
        body.addView(button("Đổi hạng mục", PALE, BLUE, this::categories));
        space(body, 12);
        LinearLayout list = column();
        body.addView(list);
        new PlaceFeed(this, list, area, item, "");
    }

    static Bitmap image(String address) {
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

}
