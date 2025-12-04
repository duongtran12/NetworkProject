NETWORK FILE TRANSFER & AUTHENTICATION PROJECT

Ứng dụng mô phỏng giao thức FTP đơn giản với chức năng quản lý người dùng, đăng nhập, quản lý tệp tin và trao đổi dữ liệu giữa Client – Server thông qua TCP Socket.

✨ Chức năng chính
🔹 SERVER

Quản lý danh sách người dùng (MySQL)

Xác thực tài khoản (USER + PASS)

Quản lý và thao tác file server-side:

Lệnh	Chức năng
USER	Gửi tên đăng nhập
PASS	Gửi mật khẩu
LIST	Liệt kê file trong thư mục hiện tại
PWD	Hiển thị thư mục hiện hành
CWD	Thay đổi thư mục
MKD	Tạo thư mục
DELE	Xóa file
STOR	Upload file
RETR	Download file
APPE	Ghi thêm vào file
RNFR / RNTO	Đổi tên file
SYST	Thông tin server
STAT	Trạng thái phiên làm việc
QUIT	Thoát

GUI hiển thị danh sách client kết nối, trạng thái Server

🔹 CLIENT

Login bằng tài khoản hợp lệ

GUI thao tác file trực quan

Gửi lệnh FTP đến server

Upload / Download dữ liệu 2 chiều

Hiển thị trạng thái từ Server realtime

🖥️ Môi trường phát triển
Công nghệ	Chi tiết
Ngôn ngữ	Java
IDE	Eclipse
Database	MySQL
Giao thức kết nối	TCP Stream Socket
OOP + Multi-threading	✔ Có sử dụng
🔧 Cài đặt hệ thống
1️⃣ Cấu hình CSDL MySQL

Tạo database:

CREATE DATABASE network_project;
USE network_project;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL
);

INSERT INTO users(username, password) VALUES ('admin','123'),('duong','123');


Chỉnh cấu hình MySQL trong file:

/server/DBConnection.java

2️⃣ Chạy Server

➡ Mở project Server → Run Server.java
Chọn thư mục gốc quản lý file tại ROOT_DIR trong CommandHandler.java

3️⃣ Chạy Client

➡ Mở project Client → Run ClientGUI.java
Đăng nhập → Bắt đầu truyền nhận file

📌 Cấu trúc dự án
Server
📁 server/
 ├── Server.java         → Khởi tạo server socket + đa luồng
 ├── ServerGUI.java      → Giao diện server
 ├── ClientHandler.java  → Xử lý từng Client
 ├── CommandHandler.java → Bộ xử lý lệnh FTP
 ├── FileManager.java    → Quản lý thao tác file
 ├── DBConnection.java   → Kết nối MySQL
 └── UserDAO.java        → CRUD người dùng

Client
📁 client/
 ├── Client.java         → Socket giao tiếp server
 ├── ClientGUI.java      → Giao diện Client

📸 Ảnh giao diện minh họa

Bạn có thể chụp ảnh từ ứng dụng và chèn vào đây
(Client Login, Server GUI, thao tác STOR / RETR ...)

![Client GUI Demo](images/client.png)
![Server GUI Demo](images/server.png)

🔐 Bảo mật & xử lý nâng cao

✔ Hash mật khẩu (có thể nâng cấp bcrypt)
✔ Kiểm soát session theo từng thread
✔ Không cho phép client thao tác ngoài thư mục server quản lý
✔ Validate lệnh, tránh truyền file độc hại

🚀 Hướng phát triển

Chuẩn hóa theo giao thức FTP đầy đủ RFC959

Mã hóa dữ liệu bằng TLS/SSL

Thêm phân quyền người dùng

Resume download/upload

Triển khai trên LAN / Cloud
