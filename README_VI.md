# MẸ GỌI CON – bản thử nghiệm miễn phí Firebase Spark

## Kiến trúc
- Firebase Authentication Anonymous.
- Firebase Realtime Database Singapore.
- Không dùng Firebase Cloud Messaging, Cloud Functions hay gói Blaze.
- Máy con chạy foreground service loại `remoteMessaging`, giữ listener Realtime Database.
- Khi yêu cầu mới xuất hiện, máy con phát âm thanh lặp + rung; thông báo có nút ĐÃ NHẬN.
- Xác nhận cập nhật `active=false`, `acknowledged=true`; máy mẹ theo dõi và hiển thị đã nhận.

## Cấu hình đã tích hợp
Đã thêm `app/google-services.json` cho project `megoicon-d5104`, package `vn.megoi.con`.
Database URL được khai báo rõ trong `FirebaseConfig.kt`:
`https://megoicon-d5104-default-rtdb.asia-southeast1.firebasedatabase.app`

## Firebase Console
1. Authentication > Sign-in method > Anonymous: Enabled.
2. Realtime Database đã tạo ở Singapore.
3. Rules cần khớp với `firebase/database.rules.json`; publish rules.
4. Không cần bật Cloud Messaging hay triển khai Functions.

## Build APK
1. Mở project bằng Android Studio mới.
2. Chờ Gradle Sync.
3. Build > Build APK(s).
4. APK debug ở `app/build/outputs/apk/debug/app-debug.apk`.
5. Cài cùng APK trên cả hai máy.

## Ghép đôi
1. Máy mẹ chọn Điện thoại mẹ > Tạo mã ghép đôi > Kết nối.
2. Máy con chọn Điện thoại con > nhập mã > Kết nối.
3. Máy con cần cho phép thông báo và giữ thông báo thường trực “MẸ GỌI CON đang hoạt động”.

## Cấu hình Xiaomi Turbo 4 Pro (HyperOS)
Tên menu có thể thay đổi theo phiên bản HyperOS:
- Cài đặt > Ứng dụng > Quản lý ứng dụng > Mẹ Gọi Con > Tiết kiệm pin > Không hạn chế.
- Bật Tự khởi động (Autostart) nếu có.
- Trong màn hình đa nhiệm, khóa ứng dụng nếu máy có chức năng khóa.
- Cài đặt > Thông báo & thanh trạng thái > Thông báo ứng dụng > Mẹ Gọi Con: cho phép thông báo, âm thanh, màn hình khóa.
- Tắt Không làm phiền khi muốn nhận cảnh báo có âm thanh.
- Không vuốt đóng app, không chọn Force stop.

## Giới hạn quan trọng
- Đây là bản thử nghiệm cá nhân, chưa kiểm thử trên thiết bị thật và chưa phải hệ thống an toàn/cấp cứu.
- Kết nối trực tiếp yêu cầu app/dịch vụ trên máy con còn sống; Android/HyperOS có thể dừng dịch vụ, mất mạng, hết pin, force-stop hoặc giới hạn nền.
- Foreground service có thông báo thường trực. Android mới có thể áp dụng hạn chế thời lượng dịch vụ; phải thử nghiệm thực tế.
- Bản này không gửi cảnh báo từ xa khi tiến trình máy con đã bị hệ thống dừng hoàn toàn. Để có push đáng tin cậy hơn cần FCM gửi từ máy chủ đáng tin cậy.
- Rules hiện tại chỉ phù hợp thử nghiệm kín; mã phòng 8 ký tự chưa có quy trình mời an toàn/chống chiếm mã hoàn chỉnh.
- Cần kiểm tra `google-services.json`, database URL, quyền notification, microphone/audio policy, battery policy và rules trước khi dùng.

## Build APK chỉ bằng điện thoại (GitHub Actions)
1. Tạo tài khoản GitHub và repository mới, ví dụ `me-goi-con-android`.
2. Tải ZIP mã nguồn lên repository và giải nén nội dung trong repository (không để nguyên ZIP).
3. Đảm bảo thư mục `.github/workflows/build-apk.yml` được tải lên. Trên điện thoại, bật hiển thị file ẩn nếu cần khi tải lên; nếu không, tạo file workflow theo nội dung trong ZIP bằng giao diện GitHub.
4. Vào tab Actions > Build Me Goi Con APK > Run workflow.
5. Chờ workflow hoàn tất. Mở lần chạy thành công, mục Artifacts, tải `Me-Goi-Con-debug-APK.zip`.
6. Giải nén trên điện thoại; cài `app-debug.apk` trên cả hai máy.
Lưu ý: Đây là quy trình build trên máy chủ GitHub, không phải APK đã có sẵn. Cần kiểm thử ứng dụng trước khi dùng thực tế.
