# Travel VN

Travel VN là ứng dụng Android viết bằng Java để lập lịch trình, khám phá Việt Nam và chia chi phí du lịch nhóm.

## Chạy trong Android Studio

1. Mở thư mục `travel-vn` bằng **Open** trong Android Studio.
2. Dùng Gradle JDK 17 hoặc 21 và cài Android SDK Platform 35 khi IDE yêu cầu.
3. Chọn điện thoại hoặc máy ảo Android 7.0 trở lên rồi nhấn **Run**.

Trên Windows, nếu đã cấu hình `ANDROID_HOME`, có thể build bằng:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## Thử các luồng chính

- Từ **Trang chủ**, mở chuyến đi Ninh Bình, chuyển ngày, rồi bấm vào hoạt động để chỉnh giờ hoặc tên.
- Bấm **Tạo Trip** để chọn điểm đến, ngày đi và ngày về, ngân sách, số người và các địa điểm muốn ghé.
- Mở tab **Chi tiêu** của chuyến đi để ghi khoản chi, chọn người trả và những người cần chia. **Xem công nợ** gợi ý các khoản chuyển. Dữ liệu của chuyến Ninh Bình từ bản trước vẫn dùng được.
- **Hành lý**, **Tài liệu** và **Nhật ký** lưu theo từng chuyến trên thiết bị. Mục **Thành viên** chia sẻ kế hoạch dạng văn bản.

- Từ **Menu**, chọn **Tạo tài khoản miễn phí** hoặc **Đăng nhập → Dùng email**. Dùng **Quên mật khẩu?** để nhận liên kết qua email. Sau khi đăng nhập, vào **Hồ sơ và tài khoản** để đổi tên, ảnh bìa hoặc mật khẩu. Hai nút Google/Apple hiện chỉ có giao diện. Ảnh bìa và dữ liệu chuyến đi lưu trên từng thiết bị.
- **Khám phá** có dải thẻ cẩm nang, hướng dẫn, điểm đến nổi bật và chỗ ở gợi ý. Nhập tên tỉnh hoặc điểm đến vào ô tìm kiếm để đổi khu vực, hoặc mở **Xem thêm điểm đến** ở cuối mục điểm đến nổi bật. Danh mục ngoại tuyến có 10.528 mục trong 70 khu vực. **63 tỉnh/thành** dùng tên trước sắp xếp hành chính. Mỗi mục có giới thiệu trong trang chi tiết. Các mục chưa có ảnh riêng dùng ảnh theo hạng mục với nhãn **Ảnh minh họa**. Bấm dòng ghi công ở trang chi tiết để xem nguồn và giấy phép của ảnh Wikimedia Commons. Nếu đã bật Google Places, app có thể hiển thị thêm dữ liệu do Google trả về. Với chỗ ở, nút tra giá mở Google Hotels để chọn ngày.
- Các dòng có cờ `osm` trong `app/src/main/assets/places-vn.tsv` là dữ liệu © OpenStreetMap contributors, trích từ bản Vietnam ngày 06/11/2026 của Geofabrik và phân nhóm theo khu vực, hạng mục. Phần dữ liệu này được phân phối theo ODbL 1.0. [Nguồn Geofabrik](https://download.geofabrik.de/asia/vietnam.html) · [Giấy phép và ghi công OpenStreetMap](https://www.openstreetmap.org/copyright)
- Mở **Hỏi AI** và gửi câu hỏi. Gemini nhận các lượt gần nhất, lịch đã lưu của chuyến đang mở, ngân sách, tổng chi phí đã ghi, hành lý và các điểm đã lưu. Trợ lý ưu tiên dữ liệu chuyến đi và dùng Google Search cho câu hỏi cần tra cứu. Khi chưa bật Gemini, app chỉ hiện câu trả lời từ lịch hoặc cẩm nang có sẵn và cung cấp liên kết tìm kiếm cho câu ngoài phạm vi.

Mở **Chuyến đi → Kế hoạch → Lộ trình** để chuyển ngày, xem ghim và danh sách điểm dừng. Nét đứt biểu diễn thứ tự ghé. **Tính đường giữa ghim** lấy đường ô tô ước tính từ OSRM cho các điểm đã có tọa độ. **Mở chỉ đường trên Google Maps** đưa các điểm trong ngày sang Google Maps, kể cả điểm nhập tay. Bản đồ nền OpenStreetMap và tính đường cần mạng.

Tab **Khám phá** có ảnh, địa điểm theo vùng và nút lưu yêu thích. Tab **Kỷ niệm** có danh sách nơi đã đi. Xem lại điểm yêu thích từ **Menu → Địa điểm đã lưu**.

## Bật tài khoản, Google Places và Gemini

1. Tạo dự án Firebase và bật **Authentication → Sign-in method → Email/Password**. Lấy **Web API Key** và **Project ID** từ Project settings. [Firebase Auth](https://firebase.google.com/docs/auth/android/password-auth)
2. Bật **Places API (New)** trong dự án Google Cloud có thanh toán và giới hạn khóa máy chủ cho API đó. Tạo khóa Gemini trong Google AI Studio. Cất cả hai khóa trên máy chủ. [Google Places](https://developers.google.com/maps/documentation/places/web-service/cloud-setup) · [Google AI Studio](https://aistudio.google.com/app/apikey)
3. Trong PowerShell 7, tại thư mục dự án, cấu hình máy chủ.

```powershell
$env:FIREBASE_WEB_API_KEY = Read-Host "Firebase Web API Key"
$env:FIREBASE_PROJECT_ID = Read-Host "Firebase Project ID"
$env:GOOGLE_MAPS_API_KEY = Read-Host "Khóa Google Places của máy chủ" -MaskInput
$env:GEMINI_API_KEY = Read-Host "Khóa Gemini của máy chủ" -MaskInput
$env:GEMINI_GROUNDING = 'search'
node .\server\index.mjs
```

4. Giữ máy chủ chạy và build APK từ cửa sổ PowerShell khác. Dùng địa chỉ gốc của máy chủ, không kèm `/api/chat`. Trên máy ảo debug có thể dùng `http://10.0.2.2:8787`. Khi phát APK cho điện thoại, đặt máy chủ đang nghe ở `127.0.0.1:8787` sau cổng HTTPS và dùng tên miền thực.

```powershell
$env:FIREBASE_WEB_API_KEY = Read-Host "Cùng Firebase Web API Key đã dùng trên máy chủ"
$env:TRAVEL_BACKEND_URL = Read-Host "Địa chỉ gốc của máy chủ"
.\gradlew.bat :app:assembleDebug
```

5. Cài APK mới và đăng nhập một lần. Trợ lý và Khám phá lấy phiên đăng nhập để gọi máy chủ. Nếu muốn khách xem dữ liệu ngay trong app, chạy máy chủ với `TRAVEL_PUBLIC_PLACES=1`; cờ này chỉ mở Khám phá, còn Hỏi AI vẫn cần xác thực. Đặt hạn mức cho Places API trước khi bật truy cập khách.

Google Places trả về mức giá hoặc khoảng giá khi có; giá phòng theo ngày được xem trên Google Hotels. Khi triển khai công khai, công bố điều khoản và chính sách quyền riêng tư cho bản phát hành. [Chính sách Google Places](https://developers.google.com/maps/documentation/places/web-service/policies)

`GEMINI_MODEL` mặc định là `gemini-3.8-flash`. Chọn `GEMINI_GROUNDING=search-maps` để bật thêm Google Maps, hoặc `none` nếu chỉ thử hỏi đáp Gemini. Kiểm tra máy chủ bằng `node --test .\server\index.test.mjs`. [Google Search grounding](https://ai.google.dev/gemini-api/docs/generate-content/google-search)

Bản APK build với cấu hình trống dùng được các màn hình và dữ liệu trên máy. Tài khoản thật, dữ liệu Places và câu trả lời Gemini cần dự án và máy chủ đã cấu hình. Menu của bản debug có mục **Kết nối máy chủ thử nghiệm** để dùng máy chủ phát triển với `TRAVEL_AI_TOKEN` riêng.

Ảnh từ Wikimedia Commons được thu nhỏ và cắt khung khi hiển thị. Giấy phép của từng ảnh vẫn áp dụng cho bản thu nhỏ và phần hiển thị cắt khung.

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

Bản đồ sử dụng Leaflet 1.9.4, dữ liệu © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), và [dịch vụ OSRM](https://map.project-osrm.org/about.html) để tính đường theo yêu cầu. Các ghim trong cẩm nang là vị trí tham khảo của điểm hoặc cổng khu tham quan.
