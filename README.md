# 🚀 Ứng dụng FTP Cơ bản với Chức năng Chat Mở rộng

Ứng dụng này cung cấp các lệnh FTP (File Transfer Protocol) cơ bản cùng với một phần mở rộng cho phép trò chuyện đơn giản (Chat) với server.

## ⚙️ Các Lệnh FTP Được Hỗ Trợ

| Lệnh | Mô Tả | Ví Dụ |
| :--- | :--- | :--- |
| **USER** `<tên>` | Gửi tên người dùng đến server. | `USER duong` |
| **PASS** `<mật_khẩu>` | Gửi mật khẩu xác thực. | `PASS 123` |
| **LIST** | Liệt kê danh sách file hiện có trong thư mục đang làm việc trên server. | `LIST` |
| **STOR** `<tên_file>` | Upload file từ thư mục `client_files/<username>/` lên thư mục hiện tại trên server. | `STOR hello.txt` |
| **RETR** `<tên_file>` | Tải file từ thư mục hiện tại của server về thư mục `client_files/<username>/`. | `RETR hello.txt` |
| **MKD** `<tên_thư_mục>` | Tạo thư mục mới trên server. | `MKD test` |
| **CWD** `<tên_thư_mục>` | Chuyển vào thư mục con trên server. | `CWD test` |
| **PWD** | Hiển thị đường dẫn thư mục hiện tại trên server. | `PWD` |
| **DELE** `<tên_file>` | Xóa file khỏi thư mục hiện tại trên server. | `DELE hello.txt` |
| **QUIT** | Thoát khỏi chương trình FTP. | `QUIT` |

---

## 💬 Các Lệnh Chat (Phần Mở Rộng)

| Lệnh | Mô Tả | Ví Dụ |
| :--- | :--- | :--- |
| **S** `<tin_nhắn>` | Gửi tin nhắn đến server. | `S Xin chào!` |
| **R** | Nhận tin nhắn phản hồi từ server. | `R` |

---

## 💾 Cài Đặt Cơ Sở Dữ Liệu (MySQL)

Dưới đây là các lệnh SQL cần thiết để tạo cơ sở dữ liệu và bảng người dùng cho ứng dụng:

```sql
CREATE DATABASE ftp_chat;
USE ftp_chat;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL
);

INSERT INTO users (username, password)
VALUES ('duong', '123'),
       ('client1', 'pass1'),
       ('admin', 'admin123');
