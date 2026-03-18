# 📌 XÂY DỰNG HỆ THỐNG QUẢN LÝ CỬA HÀNG ĐIỆN THOẠI PHONESTORE TRÊN NỀN TẢNG DESKTOP

## 📌 1. Giới thiệu
PhoneStoreManagement là hệ thống desktop hỗ trợ cửa hàng điện thoại quản lý tập trung các nghiệp vụ cốt lõi:

- 👤 Nhân viên và tài khoản
- 📦 Sản phẩm, phiên bản sản phẩm, thuộc tính
- 🏷️ Kho hàng, IMEI, nhập kho, xuất kho
- 🤝 Khách hàng, nhà cung cấp
- 📊 Thống kê và báo cáo theo thời gian

Hệ thống hướng đến mục tiêu đồng bộ dữ liệu nghiệp vụ, giảm sai lệch tồn kho và tăng hiệu quả vận hành trong môi trường bán lẻ thực tế.

## 🎯 2. Mục tiêu đề tài
- Xây dựng hệ thống quản lý cửa hàng điện thoại đầy đủ nghiệp vụ trên nền tảng desktop.
- Chuẩn hóa quy trình nhập kho, xuất kho và theo dõi tồn kho theo sản phẩm/phiên bản.
- Quản lý IMEI cho các thiết bị cần theo dõi serial.
- Quản trị người dùng, phân quyền và bảo vệ các thao tác nhạy cảm.
- Cung cấp thống kê theo thời gian để hỗ trợ ra quyết định vận hành.

## 🧠 3. Điểm nổi bật & Tính mới
Khác với mô hình quản lý rời rạc bằng file thủ công, hệ thống tập trung vào quản lý giao dịch nhất quán giữa nhập - xuất - tồn - IMEI.

### 🔍 Điểm nổi bật trong hệ thống
| Thành phần | Mục đích |
|---|---|
| ✅ Quản lý IMEI | Theo dõi thiết bị theo serial, giảm nhầm lẫn trong nhập/xuất |
| ✅ Service nghiệp vụ | Dồn xử lý tồn kho/IMEI về tầng service để giảm lệ thuộc UI |
| ✅ Bảo vệ tài khoản ADMIN | Giới hạn số lượng ADMIN và xác thực trước thao tác xóa |
| ✅ OTP email | Chỉ cho xóa tài khoản ADMIN khi OTP hợp lệ |
| ✅ Thống kê theo lịch | Tổng hợp dữ liệu nhập/xuất theo khoảng ngày cho quản lý |

## 🏗️ 4. Kiến trúc hệ thống
Hệ thống được xây dựng theo mô hình nhiều lớp (multi-layer) phù hợp với ứng dụng desktop Java:

Client (Java Swing)
  ↓
Controller Layer
  ↓
Service Layer
  ↓
DAO/JDBC Layer
  ↓
MySQL Database

### 🔹 Thành phần chính
- Frontend Desktop (Swing): Giao diện thao tác nghiệp vụ, biểu mẫu, bảng thống kê.
- Controller: Điều phối yêu cầu từ UI và chuyển đến service.
- Service: Xử lý luật nghiệp vụ, validate dữ liệu, cập nhật side effects.
- DAO/JDBC: Truy vấn SQL, ánh xạ đối tượng và làm việc trực tiếp với DB.
- Database: Lưu trữ dữ liệu giao dịch và danh mục.

## 🗄️ 5. Thiết kế cơ sở dữ liệu
Cơ sở dữ liệu sử dụng MySQL theo mô hình quan hệ.

### 👤 Quản lý người dùng
- `nhanvien`
- `taikhoan`
- `nhomquyen`

### 📦 Quản lý hàng hóa và kho
- `sanpham`
- `phienbansanpham`
- các bảng thuộc tính liên quan
- `khuvuckho`
- `imei_registry`

### 🧾 Quản lý nhập/xuất
- `phieunhap`
- `ctphieunhap`
- `phieuxuat`
- `ctphieuxuat`

### 🤝 Quản lý đối tác
- `khachhang`
- `nhacungcap`

## ⚙️ 6. Chức năng chính
### 🔐 Quản lý người dùng
- Đăng nhập / Đăng xuất
- Phân quyền theo vai trò
- Quản lý tài khoản và trạng thái hoạt động

### 🏢 Quản lý tổ chức
- Quản lý nhân viên
- Quản lý nhóm quyền
- Xem chi tiết thông tin theo quyền truy cập

### 📁 Quản lý sản phẩm & kho
- Quản lý sản phẩm, phiên bản và thuộc tính
- Quản lý khu vực kho
- Theo dõi tồn kho theo sản phẩm/phien bản

### 🧾 Quản lý nhập & xuất
- Tạo phiếu nhập và chi tiết phiếu nhập
- Tạo phiếu xuất và chi tiết phiếu xuất
- Đồng bộ tăng/giảm tồn kho khi phát sinh giao dịch

### 🧠 Chức năng bảo mật nghiệp vụ
- OTP xác thực khi xóa tài khoản ADMIN
- Giới hạn số lượng tài khoản ADMIN theo quy định hệ thống

### 📈 Dashboard & báo cáo
- Dashboard tổng quan
- Thống kê doanh thu/chi phí
- Thống kê nhập theo nhà cung cấp
- Thống kê xuất theo khách hàng
- Lọc báo cáo theo khoảng thời gian

## 🧪 7. Công nghệ sử dụng
| Thành phần | Công nghệ |
|---|---|
| Nền tảng | Java 21 |
| Giao diện | Java Swing + FlatLaf |
| Truy cập dữ liệu | JDBC |
| CSDL | MySQL 8 |
| Chọn ngày | LGoodDatePicker |
| Bảo mật mật khẩu | jBCrypt |
| Hỗ trợ mã | ZXing |

## 🧠 8. Luồng nghiệp vụ trọng tâm

### 📈 8.1. Luồng nhập kho
Mục đích:
Đưa hàng vào hệ thống và cập nhật tồn kho chính xác.

Ứng dụng trong hệ thống:
- Tạo phiếu nhập và chi tiết nhập
- Chuẩn hóa dòng trùng trước khi lưu
- Gắn IMEI theo dòng hàng
- Tăng tồn ở bảng sản phẩm và phiên bản

### 🌲 8.2. Luồng xuất kho
Mục đích:
Xuất bán và đồng bộ tồn kho/IMEI sau giao dịch.

Ứng dụng trong hệ thống:
- Tạo phiếu xuất và chi tiết xuất
- Trừ tồn kho theo số lượng thực xuất
- Đánh dấu IMEI đã bán

### 🔐 8.3. Luồng bảo mật quản trị
Mục đích:
Giảm rủi ro khi thao tác trên tài khoản quyền cao.

Ứng dụng trong hệ thống:
- Xác thực OTP trước khi xóa ADMIN
- Chỉ mở nút xóa khi OTP hợp lệ

## 📊 9. Kết quả đạt được
- Hoàn thiện hệ thống desktop quản lý cửa hàng điện thoại theo kiến trúc nhiều lớp.
- Triển khai đầy đủ các phân hệ chính: hàng hóa, kho, nhập/xuất, đối tác, nhân sự, tài khoản.
- Đồng bộ tốt hơn dữ liệu tồn kho và trạng thái IMEI ở tầng nghiệp vụ.
- Cung cấp thống kê theo thời gian phục vụ theo dõi hoạt động kinh doanh.

## 🚀 10. Hướng phát triển
- Bổ sung unit test và integration test cho các luồng quan trọng.
- Tối ưu truy vấn thống kê trên dữ liệu lớn.
- Bổ sung audit log cho thao tác nhạy cảm.
- Nâng cấp cơ chế OTP theo hướng lưu trữ có thời hạn (persistent).
- Mở rộng API/Web khi cần vận hành đa nền tảng.

## 📌 11. Phạm vi đề tài
- Áp dụng cho cửa hàng điện thoại quy mô nhỏ và vừa.
- Triển khai trên môi trường MySQL cục bộ phục vụ học tập và thực nghiệm.
- Tập trung vào quản lý hàng hóa - kho - bán hàng - nhân sự - tài khoản.

## 📚 12. Mục đích Repository
Repository được xây dựng phục vụ:

- 📖 Học tập
- 🎓 Đồ án/khóa luận tốt nghiệp
- 🔬 Nghiên cứu cải tiến nghiệp vụ quản lý bán lẻ

Giảng viên và người học có thể tham khảo:
- Kiến trúc phân lớp trong ứng dụng Java desktop
- Thiết kế dữ liệu quan hệ cho hệ thống bán lẻ
- Cách triển khai quy trình bảo mật OTP trong tác vụ quản trị

## ✨ Tổng kết
Đề tài hướng đến ứng dụng công nghệ phần mềm để giải quyết bài toán quản lý cửa hàng điện thoại trong thực tiễn, ưu tiên tính nhất quán dữ liệu và hiệu quả vận hành. Đây là nền tảng phù hợp để mở rộng lên phiên bản lớn hơn trong tương lai.

---

## ▶️ Hướng dẫn chạy nhanh

### Yêu cầu
- JDK 21
- MySQL 8+
- Cấu hình kết nối trong `db.properties`

### Build
```bash
javac -encoding UTF-8 -cp "lib/*" -d bin @all_sources.txt
```

### Run
```bash
java -cp "bin;lib/*" com.phonestore.Main
```

### Lưu ý PowerShell
Nếu gặp lỗi parse tham số classpath, có thể dùng `cmd --%` khi build/chạy.

## ✅ Kiểm tra sau khi chạy
Sau khi chạy ứng dụng thành công, kiểm tra nhanh các luồng chính:

1. Đăng nhập hệ thống bằng tài khoản hợp lệ.
2. Mở màn hình sản phẩm, kiểm tra danh sách hiển thị.
3. Tạo một phiếu nhập thử, xác nhận tồn kho tăng.
4. Tạo một phiếu xuất thử, xác nhận tồn kho giảm và IMEI (nếu có) được cập nhật trạng thái.
5. Mở thống kê nhà cung cấp/khách hàng, chọn khoảng ngày để kiểm tra dữ liệu tổng hợp.

Nếu các bước trên hoạt động đúng, hệ thống đã sẵn sàng cho vận hành nội bộ.

## ⚠️ Ghi chú vận hành
- Luôn kiểm tra kết nối cơ sở dữ liệu trước khi thao tác nghiệp vụ.
- Nên sao lưu dữ liệu định kỳ để tránh mất mát khi có sự cố.
- Với thao tác quản trị nhạy cảm (như xóa tài khoản ADMIN), cần xác thực OTP đúng quy trình.

## ✨ Tổng kết triển khai
PhoneStoreManagement đáp ứng mục tiêu xây dựng hệ thống quản lý cửa hàng điện thoại theo hướng tập trung và nhất quán dữ liệu. Hệ thống bao phủ các phân hệ quan trọng như hàng hóa, kho, nhập/xuất, IMEI, đối tác, nhân sự, tài khoản và thống kê. Việc tổ chức theo kiến trúc nhiều lớp giúp mã nguồn rõ ràng, dễ bảo trì và mở rộng. Các cải tiến gần đây về đồng bộ tồn kho, chuẩn hóa dữ liệu phiếu nhập/xuất, bảo vệ thao tác quản trị bằng OTP và thống kê theo thời gian giúp hệ thống ổn định hơn trong vận hành thực tế.
