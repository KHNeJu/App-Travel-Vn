# Travel VN

**Ứng dụng Android viết bằng Java để khám phá Việt Nam, lên lịch trình và chia chi phí chuyến đi.**

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/phu_quoc.jpg" alt="Bờ biển Phú Quốc nhìn từ cáp treo Hòn Thơm" width="760">
</p>

**Phiên bản 1.9** · Android 7.0+ · 10.528 địa điểm trong 70 khu vực

## Tính năng

| Mục | Bạn có thể làm gì |
| --- | --- |
| 🧭 **Khám phá** | Tìm chỗ ở, quán ăn, cà phê, bãi biển, địa điểm tham quan và các hạng mục khác. Nhập tên tỉnh hoặc điểm đến để đổi khu vực. Mở thẻ địa điểm để đọc giới thiệu, lưu, viết đánh giá riêng tư hoặc thêm vào lịch trình. |
| 🗓️ **Chuyến đi** | Chọn ngày đi, số người, ngân sách và các điểm muốn ghé. Quản lý hành lý, tài liệu và nhật ký theo từng chuyến. Chia sẻ kế hoạch dưới dạng văn bản. |
| 🗺️ **Lộ trình** | Xem các điểm dừng theo ngày, tính đường ước tính giữa các ghim hoặc mở chỉ đường trên Google Maps. |
| 💸 **Chi tiêu** | Ghi khoản chi, chọn người trả và người chia chung. Mục **Xem công nợ** gợi ý các khoản chuyển. |
| 📍 **Kỷ niệm** | Đánh dấu những nơi đã đi. Mở **Menu → Địa điểm đã lưu** để xem lại các địa điểm yêu thích. |
| 💬 **Hỏi AI** | Tra cứu lịch và cẩm nang có sẵn. Khi bật Gemini, trợ lý có thể dùng ngữ cảnh chuyến đi và tìm nguồn bổ sung qua Google Search. |

> [!TIP]
> Bạn có thể chạy thử **Khám phá**, lịch trình và chi tiêu với dữ liệu trên thiết bị. Tài khoản email, Google Places và Gemini được cấu hình riêng.

## Chạy ứng dụng

**Yêu cầu** gồm Android Studio, Android SDK Platform 35, Gradle JDK 17 hoặc 21 và điện thoại hoặc máy ảo chạy Android 7.0 trở lên.

1. Mở thư mục gốc của dự án bằng **Open** trong Android Studio.
2. Chọn **Gradle JDK 17** hoặc **21** và đợi đồng bộ Gradle.
3. Chọn thiết bị rồi nhấn **Run**.

Để tạo APK debug trên Windows, chạy lệnh sau tại thư mục gốc.

```powershell
.\gradlew.bat :app:assembleDebug
```

APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

## Thử một chuyến đi

1. Vào **Trang chủ**, mở chuyến Ninh Bình, chuyển ngày và bấm một hoạt động để chỉnh giờ hoặc tên.
2. Mở **Chuyến đi → Kế hoạch → Lộ trình**. Nét đứt nối các điểm theo thứ tự ghé. **Tính đường giữa ghim** dùng OSRM cho các điểm có tọa độ. **Mở chỉ đường trên Google Maps** gửi các điểm trong ngày sang Google Maps, kể cả điểm nhập tay. Bản đồ nền và tính đường cần kết nối mạng.
3. Vào **Khám phá**, nhập **Đồng Tháp** và bấm **Tìm**. Chọn một hạng mục rồi mở thẻ địa điểm. Ảnh theo hạng mục được gắn nhãn **Ảnh minh họa** khi chưa có ảnh riêng.
4. Vào **Chi tiêu** để ghi một khoản chi và thử **Xem công nợ**.

## Cấu hình dịch vụ trực tuyến

| Dịch vụ | Cấu hình | Tính năng |
| --- | --- | --- |
| Firebase Authentication | Bật **Email/Password** và lấy Web API Key cùng Project ID trong Project settings. [Firebase Auth](https://firebase.google.com/docs/auth/android/password-auth) | Đăng ký, đăng nhập email và đặt lại mật khẩu. |
| Google Places API (New) | Bật API trong dự án Google Cloud có thanh toán và giới hạn khóa máy chủ cho API này. [Thiết lập Google Places](https://developers.google.com/maps/documentation/places/web-service/cloud-setup) | Ảnh, địa chỉ, đánh giá và mức giá địa điểm khi có dữ liệu. |
| Gemini | Tạo khóa Gemini trong Google AI Studio. [Google AI Studio](https://aistudio.google.com/app/apikey) | Hỏi đáp theo ngữ cảnh chuyến đi và tra cứu thêm bằng Google Search. |

### 1. Chạy máy chủ

Máy chủ cần **Node.js**. Trong **PowerShell 7**, nhập thông tin Firebase và ít nhất một trong hai khóa Google Places hoặc Gemini. Hai khóa dịch vụ này chỉ dùng trên máy chủ.

```powershell
$env:FIREBASE_WEB_API_KEY = Read-Host "Firebase Web API Key"
$env:FIREBASE_PROJECT_ID = Read-Host "Firebase Project ID"
$env:GOOGLE_MAPS_API_KEY = Read-Host "Khóa Google Places nếu dùng" -MaskInput
$env:GEMINI_API_KEY = Read-Host "Khóa Gemini nếu dùng" -MaskInput
$env:GEMINI_GROUNDING = 'search'
node .\server\index.mjs
```

Máy chủ nghe tại `127.0.0.1:8787` theo mặc định. Trên máy ảo Android, dùng `http://10.0.2.2:8787` với bản debug. Để phát APK cho điện thoại, đặt máy chủ sau cổng HTTPS và dùng tên miền thực.

### 2. Tạo APK có cấu hình

Giữ máy chủ chạy và mở cửa sổ PowerShell thứ hai. Nhập URL gốc của máy chủ, bỏ phần đường dẫn `/api/chat`.

```powershell
$env:FIREBASE_WEB_API_KEY = Read-Host "Cùng Firebase Web API Key đã dùng trên máy chủ"
$env:TRAVEL_BACKEND_URL = Read-Host "URL gốc của máy chủ"
.\gradlew.bat :app:assembleDebug
```

Cài APK rồi vào **Menu → Đăng nhập → Dùng email**. Ứng dụng dùng phiên đăng nhập để gọi máy chủ. Ảnh bìa và dữ liệu chuyến đi được lưu trên từng thiết bị. Bản hiện tại hỗ trợ đăng nhập bằng email. Nút Google và Apple là phần giao diện.

### Tùy chọn máy chủ

| Biến | Mặc định | Cách dùng |
| --- | --- | --- |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Chọn mô hình Gemini. |
| `GEMINI_GROUNDING` | `search` | Dùng `search-maps` để thêm Google Maps hoặc `none` để chỉ hỏi đáp Gemini. |
| `TRAVEL_PUBLIC_PLACES` | Tắt | Đặt `1` để khách dùng Google Places. Hỏi AI vẫn cần xác thực. Đặt hạn mức Places trước khi mở cho khách. |
| `TRAVEL_AI_TOKEN` | Trống | Mã truy cập riêng cho mục **Kết nối máy chủ thử nghiệm** trong bản debug. |

Kiểm tra máy chủ bằng `node --test .\server\index.test.mjs`. Khi công bố ứng dụng, cung cấp điều khoản và chính sách quyền riêng tư cho bản phát hành. [Google Search grounding](https://ai.google.dev/gemini-api/docs/generate-content/google-search) · [Chính sách Google Places](https://developers.google.com/maps/documentation/places/web-service/policies)

## Dữ liệu và ghi công

- **Danh mục địa điểm.** Các dòng có cờ `osm` trong [places-vn.tsv](app/src/main/assets/places-vn.tsv) lấy từ dữ liệu © OpenStreetMap contributors, trích xuất bản Việt Nam ngày 06/11/2026 của Geofabrik và phân phối theo ODbL 1.0. Danh mục dùng tên 63 tỉnh/thành trước khi sắp xếp hành chính. [Geofabrik](https://download.geofabrik.de/asia/vietnam.html) · [Giấy phép OpenStreetMap](https://www.openstreetmap.org/copyright)
- **Bản đồ.** Lộ trình dùng Leaflet 1.9.4, bản đồ nền OpenStreetMap và OSRM để tính đường theo yêu cầu. Các ghim trong cẩm nang là vị trí tham khảo của địa điểm hoặc cổng tham quan. [Giấy phép Leaflet](app/src/main/assets/route/LEAFLET-LICENSE.txt) · [OSRM](https://map.project-osrm.org/about.html)
- **Ảnh.** Ảnh Wikimedia Commons được thu nhỏ và cắt khung khi hiển thị. Xem dòng ghi công trên trang chi tiết hoặc danh sách [ảnh Khám phá](app/src/main/java/vn/travelvn/app/ExplorePhoto.java). Ảnh bìa README là Hòn Thơm, Phú Quốc của Vivu Vietnam. [Ảnh bìa và giấy phép](https://commons.wikimedia.org/wiki/File:Hon_Thom_Cable_Car_aerial_view_Phu_Quoc_Island_Vietnam.jpg)
- **Giá phòng.** Google Places có thể trả về mức giá hoặc khoảng giá của địa điểm. Trong mục **Chỗ ở**, nút tra giá mở Google Hotels để xem giá theo ngày.

<details>
<summary>Ghi công ảnh điểm đến</summary>

| Ảnh | Tác giả | Giấy phép |
| --- | --- | --- |
| [Vịnh Nha Trang](https://commons.wikimedia.org/wiki/File:Panorama_of_V%E1%BB%8Bnh_Nha_Trang_coastline.jpg) | Tony24986 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Bãi Kem](https://commons.wikimedia.org/wiki/File:Kem_Beach_aerial_view_Phu_Quoc_Island_Vietnam.jpg) | Vivu Vietnam | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Tràng An](https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg) | Jakub Hałun | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| [Hòn Thơm, Phú Quốc](https://commons.wikimedia.org/wiki/File:Hon_Thom_Cable_Car_aerial_view_Phu_Quoc_Island_Vietnam.jpg) | Vivu Vietnam | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Phố cổ Hội An](https://commons.wikimedia.org/wiki/File:H%E1%BB%99i_An,_Ancient_Town,_2020-01_CN-04.jpg) | Steffen Schmitz (Carschten) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Hồ Xuân Hương](https://commons.wikimedia.org/wiki/File:Da_Lat,_view_to_Xuan_Huong_lake.jpg) | lionlyonne | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/) |
| [Cầu Rồng](https://commons.wikimedia.org/wiki/File:Da_Nang_Dragon_Bridge.jpg) | Person-with-No Name | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) |
| [Đại Nội Huế](https://commons.wikimedia.org/wiki/File:Hue_Imperial_City,_February_2025.jpg) | SerChevalerie | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| [Ruộng bậc thang Sa Pa](https://commons.wikimedia.org/wiki/File:Rice_terraces_in_Sa_Pa_01.jpg) | Christophe95 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |

</details>
