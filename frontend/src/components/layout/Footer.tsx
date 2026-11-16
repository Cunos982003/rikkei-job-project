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
      <footer className="w-full bg-[#f8f9fa] bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] py-12 border-t border-gray-200 mt-auto">
        <div className="container mx-auto px-4 max-w-6xl">

          {/* Tiêu đề chính */}
          <div className="mb-10">
            <h2 className="text-2xl font-bold text-gray-900">Liên hệ</h2>
            <div className="w-full h-px bg-gray-300 mt-3"></div>
          </div>

          {/* Lưới 8 cơ sở (4 cột x 2 hàng) */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-8 gap-y-12">
            {CAMPUSES.map((campus, index) => (
                <div key={index} className="flex flex-col">
                  {/* Tên cơ sở */}
                  <h3 className="text-lg font-bold text-[#c92127] mb-3">
                    {campus.name}
                  </h3>

                  {/* Đường gạch ngang nhỏ dưới tên */}
                  <div className="w-full h-px bg-gray-300 mb-4"></div>

                  {/* Địa chỉ */}
                  <div className="flex items-start gap-2 mb-3 text-gray-700">
                    <MapPin className="w-5 h-5 shrink-0 mt-0.5 text-gray-500" />
                    <p className="text-sm leading-relaxed">{campus.address}</p>
                  </div>

                  {/* Số điện thoại */}
                  <div className="flex items-center gap-2 text-gray-700">
                    <Phone className="w-5 h-5 shrink-0 text-gray-500" />
                    <p className="text-sm font-medium">{campus.phone}</p>
                  </div>
                </div>
            ))}
          </div>

        </div>
      </footer>
  );
}