import { MapPin, Phone } from 'lucide-react';

// Dữ liệu các cơ sở
const CAMPUSES = [
  {
    name: "Cơ sở 1",
    address: "HPC Landmark - 105 Tô Hiệu, Hà Đông, Hà Nội",
    phone: "0862 069 233"
  },
  {
    name: "Cơ sở 2",
    address: "HPC Landmark - 105 Tô Hiệu, Hà Đông, Hà Nội",
    phone: "0862 069 233"
  },
  {
    name: "Cơ sở 3",
    address: "Tầng 12, Tòa nhà Đảm Bảo An Toàn Hàng Hải phía Nam, Số 42 đường Tự Cường, phường 4, Tân Bình, TP HCM",
    phone: "0862 069 233"
  },
  {
    name: "Cơ sở 4",
    address: "Tầng 4, Tòa Ricco, số 363 Nguyễn Hữu Thọ, phường Khuê Trung, quận Cẩm Lệ, Đà Nẵng",
    phone: "0904 694 869"
  },
  {
    name: "Cơ sở 5",
    address: "Tầng 3, toà TSA Building, Số 77 Lê Trung Nghĩa, Phường 12, Tân Bình, Thành phố Hồ Chí Minh",
    phone: "0962 703 893"
  },
  {
    name: "Cơ sở Fukuoka",
    address: "Tokan Fukuoka 2nd Building 417 Hiecho 1-chome-18, Hakata-ku, Fukuoka, Japan",
    phone: "0904 694 869"
  },
  {
    name: "Cơ sở PTIT Hà Nội",
    address: "Học viện Công nghệ Bưu chính viễn thông, Km10, đường Nguyễn Trãi, Hà Đông, Hà Nội",
    phone: "0862 069 233"
  },
  {
    name: "Cơ sở PTIT Hồ Chí Minh",
    address: "Học viện Công nghệ Bưu chính viễn thông, 97 Man Thiện, Phường Hiệp Phú, Thủ Đức, TP HCM",
    phone: "0962 703 893"
  }
];

export function Footer() {
  return (
      // 1. Thêm padding (py-8 px-4) cho thẻ footer ngoài cùng (nền trắng) để tạo lề
      <footer className="w-full bg-white py-8 px-4 sm:px-6 lg:px-8 mt-auto">

        {/* 2. ĐÂY LÀ KHUNG ĐỎ (Đã được thu nhỏ và bo góc) */}
        <div className="relative mx-auto max-w-5xl bg-gradient-to-r from-red-700 via-red-600 to-red-700 rounded-2xl shadow-xl overflow-hidden group">

          {/* Họa tiết động bên trong khung đỏ */}
          <div className="absolute inset-0 pointer-events-none opacity-20">
            <div className="absolute w-48 h-48 bg-white rounded-full blur-3xl -top-10 -left-10 animate-pulse duration-[5000ms]"></div>
            <div className="absolute w-56 h-56 bg-yellow-300 rounded-full blur-3xl -bottom-20 -right-10 animate-pulse duration-[7000ms]"></div>
          </div>

          <div className="relative z-10 p-6 sm:p-8 lg:p-10">

            {/* Tiêu đề chính */}
            <div className="mb-8 text-center">
              <h2 className="text-xl font-bold text-white tracking-widest uppercase mb-2 drop-shadow-md">
                Liên hệ
              </h2>
              <div className="w-16 h-0.5 bg-white mx-auto rounded-full shadow-[0_0_8px_rgba(255,255,255,0.8)]"></div>
            </div>

            {/* Lưới cơ sở */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-8">
              {CAMPUSES.map((campus, index) => (
                  <div
                      key={index}
                      className="flex flex-col group/item p-3 rounded-lg transition-all duration-300 hover:bg-white/10 hover:-translate-y-1 hover:shadow-xl border border-transparent hover:border-white/20"
                  >
                    {/* Tên cơ sở */}
                    <h3 className="text-sm font-bold text-white mb-2 flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-yellow-400 group-hover/item:animate-ping"></span>
                      {campus.name}
                    </h3>

                    {/* Đường gạch ngang */}
                    <div className="w-1/3 h-[1.5px] bg-red-300 mb-3 transition-all duration-500 group-hover/item:w-full group-hover/item:bg-white"></div>

                    {/* Địa chỉ */}
                    <div className="flex items-start gap-2 mb-2 text-red-50 transition-colors duration-300 group-hover/item:text-white">
                      <MapPin className="w-4 h-4 shrink-0 mt-0.5 text-red-300 transition-transform duration-300 group-hover/item:scale-110 group-hover/item:text-yellow-400" />
                      <p className="text-xs leading-snug font-light">{campus.address}</p>
                    </div>

                    {/* Số điện thoại */}
                    <div className="flex items-center gap-2 text-red-50 mt-auto transition-colors duration-300 group-hover/item:text-white">
                      <Phone className="w-4 h-4 shrink-0 text-red-300 transition-transform duration-300 group-hover/item:rotate-12 group-hover/item:text-yellow-400" />
                      <p className="text-xs font-medium tracking-wide">{campus.phone}</p>
                    </div>
                  </div>
              ))}
            </div>

          </div>
        </div>

      </footer>
  );
}