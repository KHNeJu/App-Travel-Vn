package vn.travelvn.app;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocalTravelGuide {
    private static final Pattern DAYS = Pattern.compile("\\b([1-7]) ngay\\b");

    static TravelCatalog.Destination mentioned(String message) {
        String words = " " + TravelCatalog.fold(message).replaceAll("[^a-z0-9 ]", " ") + " ";
        for (TravelCatalog.Destination item : TravelCatalog.DESTINATIONS) {
            if (words.contains(" " + TravelCatalog.fold(item.name()) + " ")) return item;
        }
        return null;
    }

    static boolean supports(String question) {
        String words = TravelCatalog.fold(question);
        return has(words, "chia tien", "chia chi", "ung tien", "cong no", "hanh ly", "dong goi",
                "du lich", "di choi", "di dau", "tham quan", "choi gi", "lo trinh", "lich trinh",
                "khach san", "homestay", "cho o", "dat phong", "o dau", "ca phe",
                "an gi", "quan an", "dac san", "thoi tiet", "gio mo cua");
    }

    static JSONObject answer(String question, String context, TravelCatalog.Destination recent) throws JSONException {
        String words = TravelCatalog.fold(question);
        TravelCatalog.Destination city = mentioned(question);
        if (city == null) city = recent;
        if (city == null) city = mentioned(context);
        String text;
        JSONArray links = new JSONArray();
        if (has(words, "chia tien", "chia chi", "ung tien", "cong no")) {
            text = "Mở chuyến đi → Chi tiêu → Thêm khoản chi. Chọn người đã trả và đánh dấu đúng những người cần chia khoản đó. "
                    + "Sau khi ghi các khoản, bấm Xem công nợ để biết ai chuyển cho ai.";
        } else if (has(words, "hanh ly", "mang gi", "chuan bi", "dong goi")) {
            text = "Bắt đầu với giấy tờ, vé, sạc, thuốc cá nhân và giày đi bộ. "
                    + "Thêm áo mưa hoặc áo khoác nếu dự báo cần. Mở Hành lý trong chuyến đi để đánh dấu từng món.";
        } else if (city == null) {
            text = "Bạn định đi tỉnh hoặc thành phố nào? Mình có cẩm nang cho Phú Quốc, Huế, Sa Pa, Đà Lạt, Hội An, Ninh Bình và Đà Nẵng. "
                    + "Bạn có thể hỏi “Ở đâu khi đi Phú Quốc?” hoặc “Lên lịch Ninh Bình 2 ngày”.";
            links.put(link("Tìm câu hỏi trên Google Maps", question.substring(0, Math.min(question.length(), 140))));
        } else {
            String cityName = city.name();
            List<TravelCatalog.Place> places = new ArrayList<>();
            for (TravelCatalog.Place place : TravelCatalog.PLACES) if (place.city().equals(cityName)) places.add(place);
            if (has(words, "thoi tiet", "du bao", "gia hom nay", "gio mo cua", "con phong", "hien tai")) {
                text = "Cẩm nang trên máy không có dữ liệu trực tiếp về thời tiết, giá, giờ mở cửa hoặc phòng còn trống. "
                        + "Bạn có thể mở kết quả tìm kiếm để kiểm tra trước khi đi.";
                links.put(link("Kiểm tra thông tin tại " + cityName, question + " " + cityName));
            } else if (has(words, "khach san", "homestay", "o dau", "cho o", "dat phong", "luu tru")) {
                text = city.stay();
                links.put(link("Tìm chỗ ở tại " + cityName, "khách sạn homestay " + cityName));
            } else if (has(words, "ca phe", "cafe", "quan nuoc")) {
                text = "Ở " + cityName + ", hãy chọn quán gần điểm bạn sẽ ghé để tiện di chuyển. "
                        + "Mở Google Maps bên dưới để xem quán cà phê, ảnh chụp và giờ mở cửa cập nhật.";
                links.put(link("Quán cà phê ở " + cityName, "cà phê view đẹp " + cityName));
            } else if (has(words, "an gi", "quan an", "mon an", "dac san", "an uong")) {
                text = city.food();
                links.put(link("Tìm quán ăn tại " + cityName, "quán ăn địa phương " + cityName));
            } else if (has(words, "di chuyen", "lo trinh", "chi duong")) {
                text = "Trong chuyến " + cityName + ", mở Kế hoạch → Lộ trình và chọn ngày. "
                        + "Bạn sẽ thấy thứ tự điểm dừng và có thể mở tuyến đường bằng Google Maps.";
                links.put(link("Xem bản đồ " + cityName, cityName));
            } else if (has(words, "lich trinh", "ke hoach", " ngay", "ngay 2", "ngay 3")) {
                Matcher duration = DAYS.matcher(words);
                int days = duration.find() ? Integer.parseInt(duration.group(1)) : 2;
                StringBuilder plan = new StringBuilder("Gợi ý lịch " + days + " ngày ở " + cityName + ":");
                for (int i = 0; i < days; i++) {
                    plan.append("\n\nNgày ").append(i + 1).append(" · ");
                    if (i < places.size()) {
                        TravelCatalog.Place stop = places.get(i);
                        plan.append(stop.name()).append(". ").append(stop.tip());
                        links.put(link(stop.name(), stop.name() + " " + cityName));
                    } else plan.append("Dành thời gian cho các điểm còn lại, ăn món địa phương hoặc nghỉ ngơi.");
                }
                text = plan.toString();
            } else {
                StringBuilder plan = new StringBuilder(city.description()).append("\n");
                for (int i = 0; i < Math.min(3, places.size()); i++) {
                    TravelCatalog.Place stop = places.get(i);
                    plan.append("\n• ").append(stop.name()).append(" · ").append(stop.tip());
                    links.put(link(stop.name(), stop.name() + " " + cityName));
                }
                text = plan.toString();
            }
        }
        return new JSONObject().put("text", text).put("sources", links).put("local", true);
    }

    private static boolean has(String words, String... matches) {
        for (String match : matches) if (words.contains(match)) return true;
        return false;
    }

    private static JSONObject link(String title, String query) throws JSONException {
        return new JSONObject().put("title", title).put("kind", "maps")
                .put("url", "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
    }
}
