# Travel VN

Ứng dụng Android viết bằng Java để thử luồng lập lịch trình và chia chi phí du lịch nhóm.

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

**Hỏi AI** trả lời ngay từ cẩm nang lưu trên máy khi chưa bật Gemini. Mỗi câu trả lời ghi rõ nguồn. Bạn có thể hỏi về địa điểm, món ăn, chỗ ở, hành lý và lịch theo ngày ở bảy điểm đến. Khi thông tin cần dữ liệu mới, trợ lý có thể mở tìm kiếm trên Google Maps. **Hỏi trợ lý về lịch trình** mở câu hỏi điền sẵn. Dùng **Thêm hoạt động** để lưu các gợi ý bạn chọn.

Mở **Chuyến đi → Kế hoạch → Lộ trình** để chuyển ngày, xem ghim và danh sách điểm dừng. Nét đứt biểu diễn thứ tự ghé. **Tính đường giữa ghim** lấy đường ô tô ước tính từ OSRM cho các điểm đã có tọa độ. **Mở chỉ đường trên Google Maps** đưa các điểm trong ngày sang Google Maps, kể cả điểm nhập tay. Bản đồ nền OpenStreetMap và tính đường cần mạng.

Tab **Khám phá** có ảnh, địa điểm theo vùng và nút lưu yêu thích. Tab **Kỷ niệm** có danh sách nơi đã đi. Xem lại điểm yêu thích từ **Menu → Địa điểm đã lưu**.

## Bật Hỏi AI bằng Gemini

1. Tạo khóa ở [Google AI Studio](https://aistudio.google.com/app/apikey). Giữ khóa Gemini trên máy chạy máy chủ.
2. Trong PowerShell 7, mở thư mục dự án và chạy các lệnh sau. Khi được hỏi, dán khóa Gemini vào ô nhập ẩn. Chuỗi in từ lệnh `node -p` là mã truy cập Travel VN dùng để nhập vào app.

```powershell
$env:GEMINI_API_KEY = Read-Host "Khóa Gemini" -MaskInput
$env:TRAVEL_AI_TOKEN = node -p "require('node:crypto').randomBytes(24).toString('base64url')"
$env:TRAVEL_AI_TOKEN
$env:GEMINI_GROUNDING = 'search'
node .\server\index.mjs
```

3. Giữ cửa sổ máy chủ đang chạy. Trên máy ảo Android Studio dùng địa chỉ `http://10.0.2.2:8787/api/chat`. Trên điện thoại USB có gỡ lỗi, chạy `adb reverse tcp:8787 tcp:8787` rồi dùng `http://127.0.0.1:8787/api/chat`. HTTP loopback chỉ có trong bản debug.
4. Mở **Menu → Cấu hình Gemini (tùy chọn)**. Nhập địa chỉ trên và mã truy cập Travel VN. Địa chỉ được lưu trên thiết bị, mã truy cập chỉ tồn tại trong phiên chạy app. Người dùng có thể gửi câu hỏi ngay từ **Hỏi AI**, kể cả khi chưa cấu hình máy chủ.

Máy chủ nghe trên loopback. Để truy cập từ ngoài máy hoặc qua Internet, đặt dịch vụ sau một cổng HTTPS có kiểm soát truy cập. Khi gửi câu hỏi, máy chủ chuyển câu hỏi, vài lượt gần nhất và ngữ cảnh cơ bản của chuyến đi cho Gemini. Chi tiêu, tài liệu và nhật ký không được tự động đính kèm.

`GEMINI_MODEL` mặc định là `gemini-3.8-flash`. Chọn `GEMINI_GROUNDING=search-maps` để bật thêm Google Maps, hoặc `none` nếu chỉ thử hỏi đáp Gemini. Quyền dùng công cụ Google phụ thuộc model và cấu hình Google Cloud. [Xem giá và hạn mức của Gemini](https://ai.google.dev/gemini-api/docs/pricing). Kiểm tra máy chủ bằng `node --test .\server\index.test.mjs`.

Ảnh từ Wikimedia Commons được thu nhỏ và cắt khung khi hiển thị. Giấy phép của từng ảnh vẫn áp dụng cho bản thu nhỏ và phần hiển thị cắt khung.

| Ảnh | Tác giả | Giấy phép |
| --- | --- | --- |
| [Tràng An](https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg) | Jakub Hałun | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| [Hòn Thơm, Phú Quốc](https://commons.wikimedia.org/wiki/File:Hon_Thom_Cable_Car_aerial_view_Phu_Quoc_Island_Vietnam.jpg) | Vivu Vietnam | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Phố cổ Hội An](https://commons.wikimedia.org/wiki/File:H%E1%BB%99i_An,_Ancient_Town,_2020-01_CN-04.jpg) | Steffen Schmitz (Carschten) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Hồ Xuân Hương](https://commons.wikimedia.org/wiki/File:Da_Lat,_view_to_Xuan_Huong_lake.jpg) | lionlyonne | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/) |
| [Cầu Rồng](https://commons.wikimedia.org/wiki/File:Da_Nang_Dragon_Bridge.jpg) | Person-with-No Name | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) |
| [Đại Nội Huế](https://commons.wikimedia.org/wiki/File:Hue_Imperial_City,_February_2025.jpg) | SerChevalerie | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| [Ruộng bậc thang Sa Pa](https://commons.wikimedia.org/wiki/File:Rice_terraces_in_Sa_Pa_01.jpg) | Christophe95 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |

Bản đồ sử dụng Leaflet 1.9.4, dữ liệu © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), và [dịch vụ OSRM](https://map.project-osrm.org/about.html) để tính đường theo yêu cầu. Các ghim trong cẩm nang là vị trí tham khảo của điểm hoặc cổng khu tham quan.
