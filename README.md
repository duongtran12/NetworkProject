# 📡 NETWORK FILE TRANSFER & AUTHENTICATION PROJECT

Ứng dụng mô phỏng giao thức FTP đơn giản với chức năng quản lý người dùng, đăng nhập, quản lý tệp tin và trao đổi dữ liệu giữa Client – Server thông qua TCP Socket.

---

## ✨ Chức năng chính

### 🔹 Server
- Xác thực người dùng từ MySQL
- Quản lý các phiên kết nối đa luồng
- Hỗ trợ các lệnh:
  | Lệnh | Chức năng |
  |------|-----------|
  | USER / PASS | Đăng nhập |
  | LIST | Liệt kê file |
  | PWD | Thư mục hiện hành |
  | CWD | Đổi thư mục |
  | MKD | Tạo thư mục |
  | DELE | Xóa file |
  | STOR | Upload file |
  | RETR | Download file |
  | APPE | Append file |
  | RNFR / RNTO | Đổi tên file |
  | SYST / STAT | Xem thông tin hệ thống |
  | QUIT | Thoát |
- Giao diện theo dõi trạng thái server và client

### 🔹 Client
- Đăng nhập / xác thực với server
- Gửi lệnh và nhận phản hồi từ server
- Upload / Download file
- Giao diện trực quan dễ sử dụng

---

## 🖥️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|----------|-----------|
| Ngôn ngữ | Java |
| Database | MySQL |
| Giao thức | TCP Socket |
| Giao diện | Java Swing |
| Kiến trúc | Client - Server, Đa luồng |

---

## ⚙️ Hướng dẫn cài đặt

### 📌 1. Cấu hình MySQL

```sql
CREATE DATABASE network_project;
USE network_project;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL
);

INSERT INTO users(username, password)


VALUES ('admin','123'),('duong','123');

Cấu trúc thư mục
📁 server/
│── Server.java         # Khởi tạo server socket + đa luồng
│── ServerGUI.java      # Giao diện server
│── ClientHandler.java  # Xử lý từng client
│── CommandHandler.java # Bộ xử lý lệnh FTP
│── FileManager.java    # Quản lý thao tác file
│── DBConnection.java   # Kết nối MySQL
└── UserDAO.java        # CRUD tài khoản

📁 client/
│── Client.java         # Socket giao tiếp server
└── ClientGUI.java      # Giao diện người dùng
