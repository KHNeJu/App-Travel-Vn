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
- Bấm nút ✦ để nhập điểm đến, số ngày, số người và ngân sách. Xem trước rồi bấm **Lưu lịch trình** để lưu chuyến đi riêng.
- Mở tab **Tiền** của chuyến đi để ghi khoản chi, chọn người trả và những người cần chia. **Xem công nợ** gợi ý các khoản chuyển. Dữ liệu của chuyến Ninh Bình từ bản trước vẫn dùng được.
- **Hành lý**, **Tài liệu** và **Nhật ký** lưu theo từng chuyến trên thiết bị. Mục **Thành viên** chia sẻ kế hoạch dạng văn bản.

Lịch trình tạo từ mẫu có sẵn. Danh sách khám phá và lịch theo ngày tháng dùng dữ liệu Ninh Bình mẫu.

Ảnh bìa Tràng An của Jakub Hałun, giấy phép [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Ảnh được thu nhỏ và cắt khung khi hiển thị. [Ảnh gốc trên Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg).
