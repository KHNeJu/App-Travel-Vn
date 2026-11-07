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

- Mở chuyến đi Ninh Bình và chuyển giữa ba ngày.
- Nhập điểm đến, số người, ngân sách và sở thích để xem bản xem trước. Bản thử nghiệm dùng lịch trình mẫu và lưu các khoản chi của chuyến Ninh Bình ngay trên thiết bị.
- Trong **Chi tiêu**, thêm khoản chi 100.001 ₫. Tổng tăng từ 6.550.000 ₫ lên 6.650.001 ₫, phần của bạn tăng từ 1.310.000 ₫ lên 1.330.001 ₫. Mở **Xem gợi ý quyết toán** để xem các khoản cần chuyển.
