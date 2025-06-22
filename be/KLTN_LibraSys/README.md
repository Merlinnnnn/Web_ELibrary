# LibraSys - Hệ Thống Quản Lý Thư Viện

## Mô tả dự án
LibraSys là một hệ thống quản lý thư viện hiện đại được phát triển bằng Spring Boot 3.3.5. Hệ thống cung cấp các tính năng quản lý sách, người dùng, mượn trả sách và nhiều tính năng khác.

## Link triển khai
- API Production: http://api-modernlibrary.click/
- API Documentation: http://api-modernlibrary.click/swagger-ui.html

## Công nghệ sử dụng
- Java 17
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA
- MySQL
- JWT Authentication
- WebSocket
- Cloudinary (Quản lý hình ảnh)
- Apache POI (Xử lý Excel)
- Google ZXing (Tạo mã QR)
- iText7 (Xử lý PDF)
- DialogFlow (Chatbot)
- DeepLearning4j (Machine Learning)

## Yêu cầu hệ thống
- JDK 17 trở lên
- Maven 3.6.x trở lên
- MySQL 8.0 trở lên
- IDE hỗ trợ Java (khuyến nghị IntelliJ IDEA)

## Cài đặt và chạy dự án

### 1. Clone dự án
```bash
git clone [URL_REPOSITORY]
cd KLTN_LibraSys
```

### 2. Cấu hình cơ sở dữ liệu
- Tạo database MySQL mới
- Cập nhật thông tin kết nối database trong file `src/main/resources/application.properties`

### 3. Cấu hình Cloudinary
- Đăng ký tài khoản tại [Cloudinary](https://cloudinary.com)
- Cập nhật thông tin Cloudinary trong file cấu hình

### 4. Build và chạy dự án
```bash
# Build project
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run
```

### 5. Truy cập ứng dụng
- API Documentation: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:8080

## Cấu trúc thư mục
```
├── src/                    # Mã nguồn chính
├── models/                 # Các model dữ liệu
├── config/                 # Cấu hình ứng dụng
├── chatbot/               # Module chatbot
├── uploads/               # Thư mục lưu trữ file
└── target/                # Thư mục build
```

## Tính năng chính
- Quản lý sách và tài liệu
- Quản lý người dùng và phân quyền
- Mượn trả sách
- Tạo mã QR cho sách
- Xuất báo cáo PDF/Excel
- Chatbot hỗ trợ
- Gửi email thông báo
- API RESTful đầy đủ
- Bảo mật với JWT
- Rate limiting

## Đóng góp


## Giấy phép
Dự án này được phát triển cho mục đích học tập và nghiên cứu. 