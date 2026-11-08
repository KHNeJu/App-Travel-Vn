package vn.travelvn.app;

import java.text.Normalizer;
import java.util.Locale;

final class TravelCatalog {
    record Destination(String name, int image, String description, String food, String stay,
                       double lat, double lon, String author, String license, String file) {}
    record Place(String city, String name, String category, String tip, double lat, double lon) {}

    static final Destination[] DESTINATIONS = {
        new Destination("Nha Trang", R.drawable.nha_trang,
                "Dạo biển, khám phá các đảo trong vịnh rồi ghé Bảo tàng Hải dương học.",
                "Thử bún cá và hải sản địa phương.", "Tìm chỗ nghỉ gần bãi biển nếu muốn tiện đi dạo.",
                12.24, 109.197, "Tony24986", "BY-SA 4.0", "Panorama_of_Vịnh_Nha_Trang_coastline.jpg"),
        new Destination("Phú Quốc", R.drawable.phu_quoc, "Bãi biển, cáp treo và những buổi tối ở Dương Đông.",
                "Bún quậy, gỏi cá trích và hải sản. Nên hỏi giá theo phần hoặc theo cân trước khi gọi.",
                "Dương Đông thuận tiện ăn uống. Chọn An Thới nếu phần lớn lịch ở Nam đảo.", 10.22, 103.96,
                "Vivu Vietnam", "BY-SA 4.0", "Hon_Thom_Cable_Car_aerial_view_Phu_Quoc_Island_Vietnam.jpg"),
        new Destination("Huế", R.drawable.hue, "Đại Nội, sông Hương và các lăng triều Nguyễn.",
                "Bún bò, cơm hến, bánh bèo và bánh nậm. Dành một bữa để thử nhiều món bánh nhỏ.",
                "Ở trung tâm hoặc gần sông Hương để dễ đi ăn và sang Đại Nội.", 16.464, 107.590,
                "SerChevalerie", "CC0 1.0", "Hue_Imperial_City,_February_2025.jpg"),
        new Destination("Sa Pa", R.drawable.sa_pa, "Ruộng bậc thang, bản làng và đường đi bộ quanh thung lũng.",
                "Thử cá hồi, đồ nướng hoặc các món rau vùng núi. Hỏi khẩu phần trước nếu đi đông.",
                "Ở trung tâm nếu cần tiện di chuyển. Chọn homestay trong thung lũng nếu muốn ở gần bản.", 22.336, 103.844,
                "Christophe95", "BY-SA 4.0", "Rice_terraces_in_Sa_Pa_01.jpg"),
        new Destination("Đà Lạt", R.drawable.da_lat, "Hồ Xuân Hương, những con dốc và quán cà phê.",
                "Bánh căn, bánh mì xíu mại, lẩu gà lá é. Chợ đêm phù hợp cho một buổi đi ăn vặt.",
                "Gần hồ Xuân Hương hoặc chợ nếu thích đi bộ. Chọn khu xa trung tâm hơn nếu ưu tiên yên tĩnh.", 11.943, 108.445,
                "lionlyonne", "BY-SA 2.0", "Da_Lat,_view_to_Xuan_Huong_lake.jpg"),
        new Destination("Hội An", R.drawable.hoi_an, "Phố cổ, nhà gỗ và những con phố lên đèn.",
                "Cao lầu, mì Quảng, cơm gà Hội An. Có thể ghé chợ Hội An để thử nhiều món cùng lúc.",
                "Chọn chỗ gần phố cổ để đi bộ buổi tối, hoặc gần An Bàng nếu thích ở gần biển.", 15.878, 108.330,
                "Steffen Schmitz (Carschten)", "BY-SA 4.0", "Hội_An,_Ancient_Town,_2020-01_CN-04.jpg"),
        new Destination("Ninh Bình", R.drawable.trang_an, "Núi đá vôi, bến thuyền và những di tích của cố đô.",
                "Thử dê núi, cơm cháy và các món địa phương ở khu Tam Cốc hoặc Hoa Lư.",
                "Tam Cốc thuận tiện đi bộ và ăn tối. Tràng An hợp nếu lịch ưu tiên đi thuyền và tham quan gần đó.", 20.252, 105.921,
                "Jakub Hałun", "BY 4.0", "Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg"),
        new Destination("Đà Nẵng", R.drawable.da_nang, "Một ngày ở biển, một buổi dạo dọc sông Hàn.",
                "Mì Quảng, bún chả cá, bánh tráng cuốn thịt heo và hải sản.",
                "Ở gần Mỹ Khê nếu ưu tiên biển. Khu trung tâm bên sông Hàn tiện ăn uống và tham quan.", 16.066, 108.227,
                "Person-with-No Name", "BY 2.0", "Da_Nang_Dragon_Bridge.jpg")
    };

    static final Place[] PLACES = {
        new Place("Nha Trang", "Bảo tàng Hải dương học", "Tham quan", "Tìm hiểu các loài sinh vật biển.", 12.207, 109.214),
        new Place("Nha Trang", "Hòn Mun", "Thiên nhiên", "Tham khảo các chuyến tham quan đảo khởi hành từ Nha Trang.", 12.169, 109.303),
        new Place("Ninh Bình", "Cố đô Hoa Lư", "Tham quan", "Ghé khu đền và di tích của cố đô.", 20.28285, 105.90175),
        new Place("Ninh Bình", "Tràng An", "Thiên nhiên", "Chọn tuyến đi thuyền phù hợp với thời gian của nhóm.", 20.2503, 105.9186),
        new Place("Ninh Bình", "Hang Múa", "Thiên nhiên", "Leo bậc đá để ngắm cánh đồng và núi đá vôi.", 20.2297, 105.9360),
        new Place("Ninh Bình", "Chùa Bái Đính", "Tham quan", "Dành thêm thời gian di chuyển trong khuôn viên rộng.", 20.2759, 105.8650),
        new Place("Ninh Bình", "Vườn chim Thung Nham", "Thiên nhiên", "Đi bộ ngắm cảnh, kiểm tra thời gian tham quan trước khi đi.", 20.2158, 105.8883),
        new Place("Ninh Bình", "Tam Cốc", "Thiên nhiên", "Dạo quanh làng, đồng lúa và bến thuyền.", 20.2162, 105.9369),
        new Place("Phú Quốc", "Bãi Dài", "Thiên nhiên", "Một điểm nghỉ ven biển ở phía tây bắc đảo.", 10.3345, 103.8490),
        new Place("Phú Quốc", "Bãi Kem", "Thiên nhiên", "Xếp cùng các điểm phía nam đảo để hạn chế quay đầu.", 10.0358, 104.0324),
        new Place("Phú Quốc", "Chợ đêm Phú Quốc", "Ăn uống", "Dạo chợ ở Dương Đông và so giá trước khi gọi món.", 10.2158, 103.9583),
        new Place("Phú Quốc", "Quần đảo An Thới", "Tham quan", "Tham khảo tour biển tại khu cảng An Thới.", 10.0148, 104.0114),
        new Place("Hội An", "Chùa Cầu", "Tham quan", "Đi bộ qua khu phố cổ và các nhà cổ lân cận.", 15.87719, 108.32597),
        new Place("Hội An", "Chợ Hội An", "Ăn uống", "Thử cao lầu hoặc mì Quảng ở khu chợ.", 15.8769, 108.3316),
        new Place("Hội An", "Biển An Bàng", "Thiên nhiên", "Dành một buổi nghỉ gần biển nếu thời tiết thuận lợi.", 15.9146, 108.3407),
        new Place("Đà Nẵng", "Cầu Rồng", "Tham quan", "Dạo hai bờ sông Hàn vào buổi tối.", 16.0610, 108.2275),
        new Place("Đà Nẵng", "Bảo tàng Chăm", "Tham quan", "Có thể xếp cùng Cầu Rồng trong một buổi.", 16.0603, 108.2230),
        new Place("Đà Nẵng", "Bãi biển Mỹ Khê", "Thiên nhiên", "Dành một buổi sáng dạo biển.", 16.0533, 108.2471),
        new Place("Đà Lạt", "Hồ Xuân Hương", "Thiên nhiên", "Tản bộ quanh hồ hoặc kết hợp ghé quảng trường.", 11.9410, 108.4498),
        new Place("Đà Lạt", "Chợ Đà Lạt", "Ăn uống", "Dạo chợ, ăn nhẹ và mua quà địa phương.", 11.9422, 108.4377),
        new Place("Đà Lạt", "Quảng trường Lâm Viên", "Tham quan", "Một điểm dừng gần hồ Xuân Hương.", 11.9382, 108.4453),
        new Place("Huế", "Đại Nội Huế", "Tham quan", "Chừa thời gian để đi bộ giữa các công trình.", 16.4692, 107.5788),
        new Place("Huế", "Chùa Thiên Mụ", "Tham quan", "Có thể kết hợp ngắm sông Hương.", 16.4531, 107.5447),
        new Place("Huế", "Chợ Đông Ba", "Ăn uống", "Ghé chợ để tìm món Huế và quà địa phương.", 16.4726, 107.5884),
        new Place("Sa Pa", "Nhà thờ đá Sa Pa", "Tham quan", "Điểm bắt đầu để dạo khu trung tâm.", 22.3351, 103.8416),
        new Place("Sa Pa", "Bản Cát Cát", "Thiên nhiên", "Mang giày phù hợp vì có đường dốc và bậc đá.", 22.3296, 103.8301)
    };

    static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').trim();
    }

    static Destination destination(String name) {
        for (Destination item : DESTINATIONS) if (fold(item.name()).equals(fold(name))) return item;
        return null;
    }

    static Place place(String city, String name) {
        String title = fold(name);
        if (title.equals("dap xe tam coc") || title.equals("ben thuyen tam coc")) title = "tam coc";
        for (Place item : PLACES) {
            if (fold(item.city()).equals(fold(city)) && fold(item.name()).equals(title)) return item;
        }
        return null;
    }

    private TravelCatalog() {}
}
