# 🎮 RPS Multiplayer Game

**RPS Multiplayer Game** (Rock-Paper-Scissors) là một trò chơi **Kéo – Búa – Bao nhiều người chơi** được xây dựng bằng **Java (JavaFX 17)** và **SQLite**, hỗ trợ **thi đấu trực tuyến qua Socket TCP**.  
Dự án được phát triển trong môn *Lập Trình Mạng*, nhóm 2.

---

## 🚀 Tính năng chính

- 🧑‍🤝‍🧑 **Đăng ký / Đăng nhập tài khoản**
- 🕹️ **Tạo phòng & tham gia phòng đấu (Best of N)**
- ⚔️ **Thi đấu real-time** qua socket
- 💬 **Chat trong phòng & ngoài sảnh**
- 🏆 **Bảng xếp hạng người chơi (Leaderboard)**
- 📈 **Tính điểm ELO tự động** sau mỗi trận
- 💾 **Lưu lịch sử & thống kê người chơi trong SQLite**

---

## 🧩 Công nghệ sử dụng

| Thành phần | Mô tả |
|-------------|-------|
| **Java 17+** | Ngôn ngữ lập trình chính |
| **JavaFX 17** | Xây dựng giao diện người dùng |
| **SQLite** | Cơ sở dữ liệu lưu tài khoản và kết quả trận đấu |
| **Socket TCP** | Giao tiếp mạng giữa Server và Client |
| **JDBC** | Kết nối cơ sở dữ liệu SQLite |

---

## 📂 Cấu trúc thư mục
```
RockPaperScissorsGame/
├── src/
│ ├── ClientApplication/ # Ứng dụng client (JavaFX UI, socket client)
│ ├── Server/ # Mã nguồn server (Socket, GameRoom, Database, Leaderboard)
│ └── libs/ # Thư viện JavaFX 17 + SQLite JDBC
├── out/ # Thư mục build (tự sinh)
├── .idea/
└── README.md 
```

---

## ⚙️ Cài đặt & chạy dự án

### 1. Cài môi trường

- Cài **JDK 17 hoặc cao hơn**
- Cài **JavaFX 17 SDK**
- Cài **SQLite JDBC Driver** (thư viện đã có trong thư mục `libs/`)

### 2. Cấu hình JavaFX trong IDE

**IntelliJ IDEA**:

1. Mở `File → Project Structure → Libraries`
2. Thêm đường dẫn đến thư mục `libs/javafx-sdk-17/lib`
3. Vào `Run → Edit Configurations`  
   Trong phần *VM options*, thêm: --module-path "libs/javafx-sdk-17/lib" --add-modules javafx.controls,javafx.fxml

---

## ▶️ Cách chạy chương trình

### **1️⃣ Chạy Server**
- Mở file `Server/Server.java`
- Run chương trình
- Server sẽ khởi chạy trên cổng `5000`


### **2️⃣ Chạy Client**
- Mở file `ClientApplication/Main.java` (hoặc file khởi động JavaFX)
- Run ứng dụng client
- Nhập IP (localhost nếu chạy cùng máy) và port `5000`
- Đăng ký hoặc đăng nhập để bắt đầu chơi

---

## 🧮 Cách tính điểm ELO

Sau mỗi trận đấu:
- Người thắng nhận điểm ELO dựa trên công thức chuẩn: E_new = E_old + K * (1 - expectedScore)
- Người thua mất điểm tương ứng
- ELO mặc định ban đầu: **1000**

---

## 🏅 Bảng xếp hạng (Leaderboard)

Lệnh `LEADERBOARD` hiển thị top 10 người chơi có ELO cao nhất:


---

## 💾 Cơ sở dữ liệu SQLite

Tập tin cơ sở dữ liệu sẽ tự động tạo khi chạy lần đầu:

Các bảng chính:
- `users(username, password, elo, wins, losses)`
- `matches(id, player1, player2, winner, loser, bo, score1, score2, time)`

---

## 👥 Nhóm phát triển

**Nhóm 2 – Lập Trình Mạng**

| Thành viên | Vai trò |
|-------------|----------|
| Vũ Chí Hiếu | Lập trình Server và bảng xếp hạng, tính ELO và điều tiết dự án |
| Phạm Nhật Huy| Lập trình Client và giao diện |
| Nguyễn Thúc Gia Khôi | Thiết kế cơ sở dữ liệu |

---

*Dự án phục vụ mục đích học tập và nghiên cứu. Bạn có thể tự do sử dụng và chỉnh sửa cho mục đích cá nhân.*

