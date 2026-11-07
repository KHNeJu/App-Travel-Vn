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

**Hỏi AI** mở màn trợ lý với ngữ cảnh chuyến đi nếu có. **Để AI gợi ý lịch trình** mở trợ lý với câu hỏi điền sẵn. Bạn có thể đưa các gợi ý muốn dùng vào lịch trình bằng **Thêm hoạt động**. Danh sách gợi ý điểm đến trong form là dữ liệu mẫu, còn mục **Tìm trên Google Maps** mở ứng dụng bản đồ hoặc trình duyệt.

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
4. Mở **Hỏi AI → Kết nối AI**. Nhập địa chỉ trên và mã truy cập Travel VN, sau đó gửi một câu hỏi. Địa chỉ được lưu trên thiết bị, mã truy cập chỉ tồn tại trong phiên chạy app.

Máy chủ nghe trên loopback. Để truy cập từ ngoài máy hoặc qua Internet, đặt dịch vụ sau một cổng HTTPS có kiểm soát truy cập. Khi gửi câu hỏi, máy chủ chuyển câu hỏi, vài lượt gần nhất và ngữ cảnh cơ bản của chuyến đi cho Gemini. Chi tiêu, tài liệu và nhật ký không được tự động đính kèm.

`GEMINI_MODEL` mặc định là `gemini-3.8-flash`. Chọn `GEMINI_GROUNDING=search-maps` để bật thêm Google Maps, hoặc `none` nếu chỉ thử hỏi đáp Gemini. Quyền dùng công cụ Google phụ thuộc model và cấu hình Google Cloud. [Xem giá và hạn mức của Gemini](https://ai.google.dev/gemini-api/docs/pricing). Kiểm tra máy chủ bằng `node --test .\server\index.test.mjs`.

Ảnh bìa Tràng An của Jakub Hałun, giấy phép [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Ảnh được thu nhỏ và cắt khung khi hiển thị. [Ảnh gốc trên Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg).
