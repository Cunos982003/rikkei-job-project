/**
 * Giao diện Cổng Đào Tạo Sinh Viên Rikkei Education (Mock LMS Portal)
 * Phục vụ tại: http://localhost:9000/ hoặc http://localhost:9000/lms/portal
 */

function renderLmsPortalHtml(lmsData, usersData = []) {
  const students = lmsData.students || {};
  const studentsJson = JSON.stringify(students).replace(/</g, '\\u003c');
  
  const usersList = (usersData && usersData.length > 0)
    ? usersData.map(u => ({
        id: u.id,
        email: u.email,
        fullName: u.fullName,
        role: u.role || 'CANDIDATE',
        lmsUserId: u.lmsUserId || null,
        companyName: u.companyName || null,
        password: u.password || 'Test@123'
      }))
    : Object.values(students).map(s => ({
        id: s.lmsUserId,
        email: s.email,
        fullName: s.fullName,
        role: s.role || 'CANDIDATE',
        lmsUserId: s.lmsUserId,
        companyName: null,
        password: 'Test@123'
      }));

  const usersJson = JSON.stringify(usersList).replace(/</g, '\\u003c');

  return `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Rikkei Education LMS - Cổng Đào Tạo Học Viên</title>
  <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🎓</text></svg>">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;600&display=swap" rel="stylesheet">
  <script>
    tailwind.config = {
      theme: {
        extend: {
          fontFamily: {
            sans: ['"Plus Jakarta Sans"', 'sans-serif'],
            mono: ['"JetBrains Mono"', 'monospace'],
          },
          colors: {
            brand: {
              50: '#fff1f2',
              100: '#ffe4e6',
              500: '#f43f5e',
              600: '#e11d48',
              700: '#be123c',
              800: '#9f1239',
              900: '#881337',
            }
          }
        }
      }
    }
  </script>
  <style>
    body { font-family: 'Plus Jakarta Sans', sans-serif; }
    .code-font { font-family: 'JetBrains Mono', monospace; }
    @keyframes pulse-subtle {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.85; transform: scale(0.98); }
    }
    .pulse-badge { animation: pulse-subtle 2s infinite ease-in-out; }
  </style>
</head>
<body class="bg-slate-50 text-slate-800 min-h-screen flex flex-col antialiased selection:bg-rose-100 selection:text-rose-900">

  <!-- Top Navigation Header -->
  <header class="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-rose-400 text-white flex items-center justify-center font-black text-xl shadow-md shadow-brand-500/20">
          RE
        </div>
        <div>
          <div class="flex items-center gap-2">
            <span class="font-extrabold text-base tracking-tight text-slate-900">Rikkei Education LMS</span>
            <span class="px-2 py-0.5 text-[11px] font-semibold bg-rose-50 text-brand-700 rounded-full border border-rose-200">
              Mock Portal :9000
            </span>
          </div>
          <p class="text-[11px] text-slate-500 hidden sm:block">Hệ thống Đào tạo & Quản lý Học viên Chuẩn Quốc tế</p>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <div class="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
          <span>LMS Mock Server Online</span>
        </div>

        <a href="http://localhost:5173" target="_blank" rel="noopener noreferrer" 
           class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold transition-all shadow-xs">
          <span>Mở Rikkei Jobs (:5173)</span>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
          </svg>
        </a>
      </div>
    </div>
  </header>

  <!-- Main Container -->
  <main class="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
    
    <!-- Hero / Banner Section -->
    <div class="bg-gradient-to-r from-slate-900 via-slate-800 to-rose-950 rounded-2xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
      <div class="absolute -right-10 -bottom-10 w-72 h-72 bg-brand-500/10 rounded-full blur-3xl pointer-events-none"></div>
      <div class="relative z-10 max-w-3xl space-y-3">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold text-rose-200 border border-white/10">
          <span>🚀 Giả lập Hệ thống Đào tạo & Single Sign-On (SSO)</span>
        </div>
        <h1 class="text-2xl sm:text-3xl font-black tracking-tight text-white">
          Cổng Sinh Viên Rikkei Education & Tích Hợp Việc Làm
        </h1>
        <p class="text-sm text-slate-300 leading-relaxed">
          Trang mô phỏng giao diện LMS thực tế. Bạn có thể chọn nhanh từ <strong class="text-white">40 học viên</strong>, hoặc sử dụng tính năng mới 
          <strong class="text-rose-300">Đăng nhập bằng Email & Mật khẩu</strong> để thử nghiệm tất cả các tài khoản seed (Học viên, HR, Cán bộ tuyển dụng, Admin) và kích hoạt luồng 
          <strong>Đăng nhập SSO 1-chạm</strong> sang nền tảng <strong>Rikkei Jobs</strong>.
        </p>
      </div>
    </div>

    <!-- Student Selector & Email Login Dual Card -->
    <div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-6">
      
      <!-- Tab Buttons Header -->
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
        <div class="flex items-center gap-2 p-1 bg-slate-100 rounded-xl">
          <button id="tabBtnQuick" type="button" onclick="switchTab('quick')"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 bg-white text-slate-900 shadow-xs cursor-pointer">
            <span>⚡ 1-Chạm Học Viên Mẫu</span>
            <span class="px-2 py-0.5 rounded-full text-[10px] bg-rose-100 text-brand-700 font-extrabold" id="tabQuickCountBadge">40 Học viên</span>
          </button>
          <button id="tabBtnEmail" type="button" onclick="switchTab('email')"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 text-slate-600 hover:text-slate-900 hover:bg-white/60 cursor-pointer">
            <span>🔑 Đăng Nhập Email & Mật Khẩu</span>
            <span class="px-2 py-0.5 rounded-full text-[10px] bg-emerald-100 text-emerald-800 font-extrabold" id="tabEmailCountBadge">51 Tài khoản Seed</span>
          </button>
        </div>
        <div class="text-xs text-slate-500 flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
          <span>Đồng bộ auth_db (Mật khẩu mặc định: <strong class="code-font text-slate-700">Test@123</strong>)</span>
        </div>
      </div>

      <!-- Tab 1: Quick Select View -->
      <div id="tabQuickContent" class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-xs font-bold text-slate-500 uppercase tracking-wider">
            Học viên tiêu biểu (1-Chạm Chọn Nhanh)
          </h2>
          <span class="text-xs text-slate-400">Nhấn thẻ để nạp thông tin</span>
        </div>

        <!-- 3 Highlighted Quick Cards -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4" id="studentTabs">
          <!-- Rendered via JS -->
        </div>

        <!-- 40 Students Full Dropdown -->
        <div class="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
          <label for="studentSelectDropdown" class="text-xs font-bold text-slate-700 shrink-0 flex items-center gap-2">
            <svg class="w-4 h-4 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
            </svg>
            <span>Hoặc chọn từ toàn bộ danh sách 40 học viên:</span>
          </label>
          <select id="studentSelectDropdown" onchange="selectStudent(this.value)"
            class="flex-1 bg-white border border-slate-300 rounded-lg px-3 py-2 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 cursor-pointer">
            <!-- Rendered via JS -->
          </select>
        </div>
      </div>

      <!-- Tab 2: Email & Password Login View -->
      <div id="tabEmailContent" class="hidden space-y-4">
        <form id="emailLoginForm" onsubmit="handleEmailLogin(event)" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 items-end">
            
            <!-- Email Input with Datalist Suggestions -->
            <div class="sm:col-span-2 space-y-1.5">
              <label for="loginEmail" class="block text-xs font-bold text-slate-700">
                Email tài khoản <span class="text-rose-500">*</span>
              </label>
              <div class="relative">
                <input id="loginEmail" type="email" list="seedAccountsList" required
                  placeholder="Gõ hoặc chọn email (candidate1..., hr1...)"
                  class="w-full bg-slate-50 border border-slate-300 rounded-lg px-3 py-2 text-xs font-semibold text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500">
                <datalist id="seedAccountsList">
                  <!-- Rendered via JS -->
                </datalist>
              </div>
            </div>

            <!-- Password Input with Eye Toggle -->
            <div class="sm:col-span-2 space-y-1.5">
              <div class="flex items-center justify-between">
                <label for="loginPassword" class="block text-xs font-bold text-slate-700">
                  Mật khẩu <span class="text-rose-500">*</span>
                </label>
                <span class="text-[11px] text-slate-400">Gợi ý: <code class="code-font text-brand-600">Test@123</code></span>
              </div>
              <div class="relative">
                <input id="loginPassword" type="password" value="Test@123" required
                  placeholder="Mật khẩu"
                  class="w-full bg-slate-50 border border-slate-300 rounded-lg px-3 py-2 pr-10 text-xs font-semibold text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500">
                <button type="button" onclick="togglePasswordVisibility()" title="Ẩn/Hiện mật khẩu"
                  class="absolute right-2 top-2 text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer p-0.5">
                  <span id="pwdEyeIcon">👁️</span>
                </button>
              </div>
            </div>

            <!-- Submit Button -->
            <div class="sm:col-span-1">
              <button type="submit" id="btnLoginSubmit"
                class="w-full py-2 px-4 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-1.5 cursor-pointer h-[34px]">
                <span>🔐 Đăng nhập LMS</span>
              </button>
            </div>

          </div>

          <!-- Dynamic Feedback Banner -->
          <div id="loginFeedback" class="hidden text-xs rounded-xl p-3 border">
            <!-- Dynamic feedback -->
          </div>

          <!-- Quick Seed Chips -->
          <div class="flex flex-wrap items-center gap-2 pt-2 text-xs text-slate-500">
            <span class="font-semibold text-slate-600">Thử nhanh các vai trò:</span>
            <button type="button" onclick="fillLoginForm('candidate4@rikkeijobs.test', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 code-font text-[11px] font-medium transition-colors cursor-pointer">
              std-004 (Phạm Thu Dung)
            </button>
            <button type="button" onclick="fillLoginForm('candidate15@rikkeijobs.test', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 code-font text-[11px] font-medium transition-colors cursor-pointer">
              std-015 (Tạ Hoàng Phúc)
            </button>
            <button type="button" onclick="fillLoginForm('candidate40@rikkeijobs.test', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 code-font text-[11px] font-medium transition-colors cursor-pointer">
              std-040 (Tạ Văn Long)
            </button>
            <button type="button" onclick="fillLoginForm('hr1@rikkeijobs.test', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-amber-50 hover:bg-amber-100 text-amber-800 text-[11px] font-semibold border border-amber-200 transition-colors cursor-pointer">
              hr1 (HR Tuyển dụng)
            </button>
            <button type="button" onclick="fillLoginForm('staff1@rikkeijobs.test', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-blue-50 hover:bg-blue-100 text-blue-800 text-[11px] font-semibold border border-blue-200 transition-colors cursor-pointer">
              staff1 (Cán bộ LMS)
            </button>
            <button type="button" onclick="fillLoginForm('ducanhn2810@gmail.com', 'Test@123')"
              class="px-2.5 py-1 rounded-md bg-rose-50 hover:bg-rose-100 text-rose-800 text-[11px] font-semibold border border-rose-200 transition-colors cursor-pointer">
              ducanhn2810 (HR Rikkeisoft)
            </button>
          </div>

        </form>
      </div>

    </div>

    <!-- Active Student Detail & SSO Actions Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
      
      <!-- Left Column: Student Profile & Academic Record (2 spans) -->
      <div class="lg:col-span-2 space-y-6">
        
        <!-- Profile Summary Card -->
        <div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-6">
          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
            <div class="flex items-center gap-4">
              <div id="studentAvatar" class="w-16 h-16 rounded-2xl bg-gradient-to-tr from-brand-600 to-rose-400 text-white font-black text-2xl flex items-center justify-center shadow-md">
                NA
              </div>
              <div>
                <div class="flex items-center gap-2 flex-wrap">
                  <h3 id="studentName" class="text-xl font-bold text-slate-900">Nguyễn Văn An</h3>
                  <span id="studentRoleBadge" class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                    Học viên (STUDENT)
                  </span>
                </div>
                <p id="studentEmail" class="text-sm text-slate-500 mt-0.5">candidate1@rikkeijobs.test</p>
              </div>
            </div>

            <div class="flex flex-col sm:items-end gap-1">
              <span id="studentIdLabel" class="text-xs text-slate-400 font-medium">Mã học viên (LMS User ID)</span>
              <span id="studentLmsIdBadge" class="code-font text-xs font-bold px-3 py-1 rounded-lg bg-slate-100 text-slate-800 border border-slate-200">
                std-001
              </span>
            </div>
          </div>

          <!-- Metadata grid -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4" id="metadataGrid">
            <div class="bg-slate-50 rounded-xl p-3.5 border border-slate-100">
              <p id="metaLabel1" class="text-xs text-slate-500 font-medium">Lớp học</p>
              <p id="studentClass" class="text-sm font-bold text-slate-800 mt-1">RA-JAVA-2026</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-3.5 border border-slate-100">
              <p id="metaLabel2" class="text-xs text-slate-500 font-medium">Chương trình đào tạo</p>
              <p id="studentProgram" class="text-sm font-bold text-slate-800 mt-1 truncate">Java Fullstack Chuyên sâu</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-3.5 border border-slate-100">
              <p id="metaLabel3" class="text-xs text-slate-500 font-medium">Tổng R-Point tích lũy</p>
              <p id="studentTotalRpoint" class="text-sm font-bold text-brand-600 mt-1">480 Điểm</p>
            </div>
          </div>
        </div>

        <!-- Academic Results Table Card -->
        <div id="academicResultsCard" class="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="text-base font-bold text-slate-900">Kết Quả Học Tập & Điểm Chuyên Cần</h4>
              <p class="text-xs text-slate-500">Dữ liệu thực tế được đồng bộ tự động sang hồ sơ Rikkei Jobs qua SSO</p>
            </div>
            <span id="resultsCountBadge" class="text-xs font-semibold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-md">
              4 Môn hoàn thành
            </span>
          </div>

          <div class="overflow-x-auto rounded-xl border border-slate-200">
            <table class="w-full text-left border-collapse text-xs">
              <thead>
                <tr class="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                  <th class="py-3 px-3.5">Mã môn</th>
                  <th class="py-3 px-3.5">Tên môn học</th>
                  <th class="py-3 px-3.5 text-center">Điểm số</th>
                  <th class="py-3 px-3.5 text-center">Chuyên cần</th>
                  <th class="py-3 px-3.5 text-center">Bài tập</th>
                  <th class="py-3 px-3.5 text-center">R-Point</th>
                </tr>
              </thead>
              <tbody id="studentResultsBody" class="divide-y divide-slate-100 font-medium">
                <!-- Rendered via JS -->
              </tbody>
            </table>
          </div>
        </div>

        <!-- Staff / Non-Student Informational Card (hidden by default) -->
        <div id="staffInfoCard" class="hidden bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center font-bold text-lg">
              ℹ️
            </div>
            <div>
              <h4 class="text-base font-bold text-slate-900">Tài Khoản Quản Lý / Doanh Nghiệp</h4>
              <p class="text-xs text-slate-500">Tài khoản này có vai trò phân quyền đặc biệt trên nền tảng Rikkei Jobs</p>
            </div>
          </div>
          <p class="text-xs text-slate-600 leading-relaxed">
            Bạn đã đăng nhập với tư cách nhân viên hoặc đối tác tuyển dụng. Đối với tài khoản này, bạn có thể chuyển trực tiếp sang Rikkei Jobs để truy cập vào bảng điều khiển tuyển dụng, quản lý tin tuyển dụng hoặc duyệt ứng viên.
          </p>
        </div>

      </div>

      <!-- Right Column: SSO Bridge Action Panel (1 span) -->
      <div class="space-y-6">
        
        <div class="bg-gradient-to-b from-white to-rose-50/50 rounded-2xl border-2 border-brand-200 p-6 shadow-md space-y-6 relative">
          <div class="absolute -top-3 left-6 px-3 py-0.5 rounded-full bg-brand-600 text-white text-[11px] font-bold shadow-xs uppercase tracking-wider">
            SSO Bridge
          </div>

          <div class="space-y-2 pt-1">
            <h3 id="ssoCardTitle" class="text-lg font-extrabold text-slate-900 tracking-tight">
              Chuyển Sang Rikkei Jobs
            </h3>
            <p id="ssoCardDescription" class="text-xs text-slate-600 leading-relaxed">
              Nhấn nút bên dưới để tạo <strong>Mã vé SSO (One-time Ticket)</strong> và tự động đăng nhập vào 
              <strong>Rikkei Jobs</strong> mà không cần nhập mật khẩu.
            </p>
          </div>

          <!-- Feature Bullets -->
          <ul class="text-xs text-slate-600 space-y-2.5">
            <li class="flex items-start gap-2">
              <svg class="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              <span>Tự động chuyển tiếp vé sang <code class="code-font bg-slate-100 px-1 py-0.5 rounded text-slate-800">:5173/login?ticket=...</code></span>
            </li>
            <li class="flex items-start gap-2">
              <svg class="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              <span>Thời hạn hiệu lực vé: <strong class="text-slate-800">60 giây</strong></span>
            </li>
            <li class="flex items-start gap-2">
              <svg class="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              <span>Tự động giải phóng sau khi dùng (GETDEL - 1 lần duy nhất)</span>
            </li>
          </ul>

          <!-- Primary CTA Button -->
          <div class="space-y-3 pt-2">
            <button id="btnAutoSso" type="button" onclick="handleGenerateTicketAndRedirect()"
              class="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-brand-600 to-rose-600 hover:from-brand-700 hover:to-rose-700 text-white font-bold text-sm shadow-md shadow-brand-500/25 transition-all transform active:scale-95 flex items-center justify-center gap-2 cursor-pointer">
              <span id="btnAutoSsoSpinner" class="hidden">
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </span>
              <span id="btnAutoSsoText">🚀 Đăng nhập Rikkei Jobs (Tự động 1 chạm)</span>
            </button>

            <button id="btnManualTicket" type="button" onclick="handleGenerateTicketManual()"
              class="w-full py-2.5 px-4 rounded-xl bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs border border-slate-300 transition-all flex items-center justify-center gap-1.5 cursor-pointer">
              <span>📋 Tạo & Sao chép mã vé thủ công</span>
            </button>
          </div>

          <!-- Ticket Display Box (Appears after generating) -->
          <div id="ticketResultBox" class="hidden pt-4 border-t border-rose-200/60 space-y-3">
            <div class="flex items-center justify-between text-xs font-semibold">
              <span class="text-slate-700 flex items-center gap-1.5">
                <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                <span>Mã vé SSO vừa tạo:</span>
              </span>
              <span id="ticketCountdown" class="text-rose-600 code-font">60s</span>
            </div>

            <div class="relative">
              <input id="ticketInput" type="text" readonly
                class="w-full bg-slate-100 code-font text-xs font-bold px-3 py-2.5 rounded-lg border border-slate-200 text-slate-800 pr-20 select-all outline-none"
                value="" />
              <button type="button" onclick="copyTicketToClipboard()"
                class="absolute right-1.5 top-1.5 px-2.5 py-1 rounded-md bg-white border border-slate-200 text-[11px] font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer transition-all shadow-2xs">
                <span id="copyBtnText">Sao chép</span>
              </button>
            </div>

            <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
              <div id="ticketProgressBar" class="bg-rose-500 h-full transition-all duration-1000 ease-linear" style="width: 100%"></div>
            </div>

            <p class="text-[11px] text-slate-500">
              👉 Bạn có thể dán mã vé này vào tab <strong>"Học viên (Vé SSO)"</strong> trên trang Đăng nhập Rikkei Jobs.
            </p>
          </div>

        </div>

        <!-- Integration Help Card -->
        <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-3 text-xs text-slate-600">
          <div class="flex items-center gap-2 font-bold text-slate-800">
            <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <span>Thông tin kiểm thử Microservices</span>
          </div>
          <p class="leading-relaxed">
            - Endpoint cấp vé: <code class="code-font bg-slate-100 px-1 py-0.5 rounded text-slate-800">POST /lms/sso/tickets</code><br>
            - Endpoint backend xác thực: <code class="code-font bg-slate-100 px-1 py-0.5 rounded text-slate-800">POST /lms/sso/verify</code><br>
            - Frontend Rikkei Jobs: <a href="http://localhost:5173/login" target="_blank" class="text-rose-600 hover:underline font-semibold">http://localhost:5173/login</a>
          </p>
        </div>

      </div>

    </div>

  </main>

  <!-- Footer -->
  <footer class="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
    <div class="max-w-7xl mx-auto px-4">
      <p>© 2026 Rikkei Education LMS. Hệ thống mô phỏng tích hợp Single Sign-On (SSO) phục vụ Dự án Rikkei Jobs.</p>
    </div>
  </footer>

  <!-- Script logic -->
  <script>
    const STUDENTS = ${studentsJson};
    const USERS = ${usersJson};
    let currentStudentId = 'std-001';
    let currentLoggedInUser = null;
    let countdownTimer = null;

    function init() {
      renderStudentTabs();
      renderStudentDropdown();
      renderSeedAccountsDatalist();
      selectStudent(currentStudentId);
    }

    // Switch between Tab 1 (Quick Select) and Tab 2 (Email Login)
    function switchTab(tab) {
      const btnQuick = document.getElementById('tabBtnQuick');
      const btnEmail = document.getElementById('tabBtnEmail');
      const contentQuick = document.getElementById('tabQuickContent');
      const contentEmail = document.getElementById('tabEmailContent');

      if (tab === 'quick') {
        btnQuick.className = 'px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 bg-white text-slate-900 shadow-xs cursor-pointer';
        btnEmail.className = 'px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 text-slate-600 hover:text-slate-900 hover:bg-white/60 cursor-pointer';
        contentQuick.classList.remove('hidden');
        contentEmail.classList.add('hidden');
      } else {
        btnEmail.className = 'px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 bg-white text-slate-900 shadow-xs cursor-pointer';
        btnQuick.className = 'px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 text-slate-600 hover:text-slate-900 hover:bg-white/60 cursor-pointer';
        contentEmail.classList.remove('hidden');
        contentQuick.classList.add('hidden');
      }
    }

    // Render 3 Highlight Cards in Tab 1
    function renderStudentTabs() {
      const container = document.getElementById('studentTabs');
      if (!container) return;

      const highlightIds = ['std-001', 'std-002', 'std-003'];
      container.innerHTML = highlightIds.map(id => {
        const s = STUDENTS[id];
        if (!s) return '';
        const isSelected = id === currentStudentId;
        const initials = s.fullName.split(' ').slice(-2).map(n => n[0]).join('');

        return \`
          <div onclick="selectStudent('\${id}')" 
               class="cursor-pointer rounded-xl p-4 border-2 transition-all flex items-center gap-3.5 \${
                 isSelected 
                   ? 'border-brand-600 bg-rose-50/60 shadow-sm' 
                   : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50'
               }">
            <div class="w-11 h-11 rounded-xl \${
              isSelected ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-700'
            } font-bold text-sm flex items-center justify-center shrink-0">
              \${initials}
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center justify-between gap-1">
                <h4 class="font-bold text-sm text-slate-900 truncate">\${s.fullName}</h4>
                <span class="code-font text-[10px] px-1.5 py-0.5 rounded \${
                  isSelected ? 'bg-brand-100 text-brand-800 font-bold' : 'bg-slate-100 text-slate-500'
                }">\${s.lmsUserId}</span>
              </div>
              <p class="text-xs text-slate-500 truncate mt-0.5">\${s.courseName}</p>
              <p class="text-[11px] text-slate-400 mt-0.5">\${s.classCode}</p>
            </div>
          </div>
        \`;
      }).join('');
    }

    // Render 40-student dropdown options
    function renderStudentDropdown() {
      const select = document.getElementById('studentSelectDropdown');
      if (!select) return;

      const keys = Object.keys(STUDENTS);
      select.innerHTML = keys.map(id => {
        const s = STUDENTS[id];
        return \`<option value="\${id}">[\${s.lmsUserId}] \${s.fullName} - \${s.program} (\${s.email})</option>\`;
      }).join('');

      select.value = currentStudentId;
    }

    // Render datalist of all 51 seed accounts for easy autocomplete
    function renderSeedAccountsDatalist() {
      const datalist = document.getElementById('seedAccountsList');
      if (!datalist) return;

      datalist.innerHTML = USERS.map(u => {
        const tag = u.lmsUserId ? \`Học viên \${u.lmsUserId}\` : \`Vai trò \${u.role}\`;
        return \`<option value="\${u.email}">\${u.fullName} (\${tag})</option>\`;
      }).join('');
    }

    // Fill form from suggestion chips
    function fillLoginForm(email, password) {
      document.getElementById('loginEmail').value = email;
      document.getElementById('loginPassword').value = password;
      switchTab('email');
      const feedback = document.getElementById('loginFeedback');
      feedback.className = 'hidden';
    }

    // Toggle password visibility
    function togglePasswordVisibility() {
      const pwdInput = document.getElementById('loginPassword');
      const icon = document.getElementById('pwdEyeIcon');
      if (pwdInput.type === 'password') {
        pwdInput.type = 'text';
        icon.textContent = '🙈';
      } else {
        pwdInput.type = 'password';
        icon.textContent = '👁️';
      }
    }

    // Handle Form Email Login
    function handleEmailLogin(event) {
      if (event) event.preventDefault();

      const emailInput = document.getElementById('loginEmail').value.trim();
      const pwdInput = document.getElementById('loginPassword').value.trim();
      const feedback = document.getElementById('loginFeedback');

      if (!emailInput) {
        showFeedback('Vui lòng nhập địa chỉ email!', 'error');
        return;
      }

      // Find user
      const user = USERS.find(u => u.email.toLowerCase() === emailInput.toLowerCase());
      if (!user) {
        showFeedback(\`Không tìm thấy tài khoản "\${emailInput}". Vui lòng kiểm tra lại danh sách tài khoản seed!\`, 'error');
        return;
      }

      // Check password
      if (user.password && user.password !== pwdInput) {
        showFeedback('Mật khẩu không chính xác! Hãy thử mật khẩu mặc định "Test@123".', 'error');
        return;
      }

      // Success login
      currentLoggedInUser = user;

      if (user.lmsUserId && STUDENTS[user.lmsUserId.toLowerCase()]) {
        // Logged in as student
        selectStudent(user.lmsUserId.toLowerCase());
        showFeedback(\`🎉 Đăng nhập thành công học viên: <strong>\${user.fullName}</strong> (\${user.lmsUserId}). Đã nạp bảng điểm và kích hoạt SSO!\`, 'success');
      } else {
        // Logged in as HR, Staff, Mentor, Admin
        selectNonStudent(user);
        showFeedback(\`🎉 Đăng nhập thành công với vai trò: <strong>\${user.role}</strong> (\${user.fullName}). Đã sẵn sàng chuyển tiếp sang Rikkei Jobs!\`, 'success');
      }
    }

    function showFeedback(message, type) {
      const fb = document.getElementById('loginFeedback');
      fb.classList.remove('hidden');
      if (type === 'success') {
        fb.className = 'text-xs rounded-xl p-3 border bg-emerald-50 text-emerald-800 border-emerald-200';
      } else {
        fb.className = 'text-xs rounded-xl p-3 border bg-rose-50 text-rose-800 border-rose-200';
      }
      fb.innerHTML = message;
    }

    // Select Student and Update Views
    function selectStudent(id) {
      const studentKey = id.toLowerCase();
      if (!STUDENTS[studentKey]) return;
      currentStudentId = studentKey;
      const s = STUDENTS[studentKey];

      // Update Tab 1 UI
      renderStudentTabs();
      const select = document.getElementById('studentSelectDropdown');
      if (select) select.value = studentKey;

      // Ensure Profile view is active
      document.getElementById('academicResultsCard').classList.remove('hidden');
      document.getElementById('staffInfoCard').classList.add('hidden');

      // Update Profile Details
      const initials = s.fullName.split(' ').slice(-2).map(n => n[0]).join('');
      document.getElementById('studentAvatar').textContent = initials;
      document.getElementById('studentAvatar').className = 'w-16 h-16 rounded-2xl bg-gradient-to-tr from-brand-600 to-rose-400 text-white font-black text-2xl flex items-center justify-center shadow-md';
      document.getElementById('studentName').textContent = s.fullName;
      document.getElementById('studentEmail').textContent = s.email;
      
      const roleBadge = document.getElementById('studentRoleBadge');
      roleBadge.textContent = 'Học viên (STUDENT)';
      roleBadge.className = 'px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200';

      document.getElementById('studentIdLabel').textContent = 'Mã học viên (LMS User ID)';
      document.getElementById('studentLmsIdBadge').textContent = s.lmsUserId;

      // Metadata labels
      document.getElementById('metaLabel1').textContent = 'Lớp học';
      document.getElementById('studentClass').textContent = s.classCode;
      document.getElementById('metaLabel2').textContent = 'Chương trình đào tạo';
      document.getElementById('studentProgram').textContent = s.program;
      document.getElementById('metaLabel3').textContent = 'Tổng R-Point tích lũy';

      // Calculate Total RPoint
      const totalRpoint = (s.results || []).reduce((acc, r) => acc + (r.rpoint || 0), 0);
      document.getElementById('studentTotalRpoint').textContent = totalRpoint + ' Điểm';

      // Render Results Table
      const resultsBody = document.getElementById('studentResultsBody');
      const resultsBadge = document.getElementById('resultsCountBadge');
      if (s.results && s.results.length > 0) {
        resultsBadge.textContent = s.results.length + ' Môn hoàn thành';
        resultsBody.innerHTML = s.results.map(r => \`
          <tr class="hover:bg-slate-50/80 transition-colors">
            <td class="py-3 px-3.5 code-font font-bold text-slate-700">\${r.subjectCode}</td>
            <td class="py-3 px-3.5 text-slate-900 font-semibold">\${r.subjectName}</td>
            <td class="py-3 px-3.5 text-center">
              <span class="inline-block px-2 py-0.5 rounded font-bold \${
                r.score >= 8.5 ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-blue-50 text-blue-700 border border-blue-200'
              }">\${r.score.toFixed(1)}</span>
            </td>
            <td class="py-3 px-3.5 text-center text-slate-600">\${r.attendanceRate}%</td>
            <td class="py-3 px-3.5 text-center text-slate-600">\${r.homeworkRate}%</td>
            <td class="py-3 px-3.5 text-center font-bold text-brand-600">+\${r.rpoint}</td>
          </tr>
        \`).join('');
      } else {
        resultsBadge.textContent = '0 Môn';
        resultsBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-slate-400">Chưa có kết quả môn học</td></tr>';
      }

      // Reset SSO panel for student
      document.getElementById('ssoCardTitle').textContent = 'Chuyển Sang Rikkei Jobs';
      document.getElementById('ssoCardDescription').innerHTML = 'Nhấn nút bên dưới để tạo <strong>Mã vé SSO (One-time Ticket)</strong> và tự động đăng nhập vào <strong>Rikkei Jobs</strong>.';
      document.getElementById('btnAutoSsoText').textContent = '🚀 Đăng nhập Rikkei Jobs (Tự động 1 chạm)';
      document.getElementById('btnManualTicket').classList.remove('hidden');

      // Hide previous ticket if switching student
      document.getElementById('ticketResultBox').classList.add('hidden');
      if (countdownTimer) clearInterval(countdownTimer);
    }

    // Select Non-Student (HR, Staff, Admin)
    function selectNonStudent(user) {
      currentStudentId = null;
      document.getElementById('academicResultsCard').classList.add('hidden');
      document.getElementById('staffInfoCard').classList.remove('hidden');

      const initials = user.fullName.split(' ').slice(-2).map(n => n[0]).join('');
      document.getElementById('studentAvatar').textContent = initials;
      document.getElementById('studentAvatar').className = 'w-16 h-16 rounded-2xl bg-gradient-to-tr from-amber-500 to-indigo-600 text-white font-black text-2xl flex items-center justify-center shadow-md';
      document.getElementById('studentName').textContent = user.fullName;
      document.getElementById('studentEmail').textContent = user.email;

      const roleBadge = document.getElementById('studentRoleBadge');
      roleBadge.textContent = user.role + ' (Doanh nghiệp/Cán bộ)';
      roleBadge.className = 'px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200';

      document.getElementById('studentIdLabel').textContent = 'Vai trò hệ thống';
      document.getElementById('studentLmsIdBadge').textContent = user.role;

      // Metadata labels
      document.getElementById('metaLabel1').textContent = 'Tổ chức / Doanh nghiệp';
      document.getElementById('studentClass').textContent = user.companyName || 'Rikkei Education';
      document.getElementById('metaLabel2').textContent = 'Phân quyền';
      document.getElementById('studentProgram').textContent = user.role === 'ADMIN' ? 'Toàn quyền Hệ thống' : 'Quản lý Tuyển dụng';
      document.getElementById('metaLabel3').textContent = 'Trạng thái';
      document.getElementById('studentTotalRpoint').textContent = 'Hoạt động';

      // Update SSO panel for direct redirect
      document.getElementById('ssoCardTitle').textContent = 'Đăng Nhập Rikkei Jobs (:5173)';
      document.getElementById('ssoCardDescription').innerHTML = 'Chuyển thẳng sang trang <strong>Rikkei Jobs</strong> với tài khoản <strong>' + user.email + '</strong>.';
      document.getElementById('btnAutoSsoText').textContent = '🚀 Mở Rikkei Jobs & Đăng nhập ngay';
      document.getElementById('btnManualTicket').classList.add('hidden');
      document.getElementById('ticketResultBox').classList.add('hidden');
      if (countdownTimer) clearInterval(countdownTimer);
    }

    async function fetchTicket(lmsUserId) {
      const response = await fetch('/lms/sso/tickets', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Service-Key': 'fbc368ffa97a999a71577c552ecc2546815f245fd4ebee014732afcba1b52870'
        },
        body: JSON.stringify({ lmsUserId })
      });
      if (!response.ok) {
        throw new Error('Lỗi khi tạo vé từ LMS: HTTP ' + response.status);
      }
      return await response.json();
    }

    async function handleGenerateTicketAndRedirect() {
      // If non-student, redirect directly to login page with prefilled email
      if (!currentStudentId && currentLoggedInUser) {
        window.location.href = 'http://localhost:5173/login?email=' + encodeURIComponent(currentLoggedInUser.email);
        return;
      }

      const btn = document.getElementById('btnAutoSso');
      const spinner = document.getElementById('btnAutoSsoSpinner');
      const text = document.getElementById('btnAutoSsoText');

      try {
        btn.disabled = true;
        spinner.classList.remove('hidden');
        text.textContent = 'Đang cấp vé SSO & chuyển hướng...';

        const data = await fetchTicket(currentStudentId);
        const ticket = data.ticket;

        // Redirect URL to Rikkei Jobs LoginPage with ticket and auto=true
        const targetUrl = 'http://localhost:5173/login?ticket=' + encodeURIComponent(ticket) + '&auto=true';

        // Brief delay for smooth UX transition
        setTimeout(() => {
          window.location.href = targetUrl;
        }, 400);

      } catch (err) {
        alert('Không thể tạo vé SSO: ' + err.message);
        btn.disabled = false;
        spinner.classList.add('hidden');
        text.textContent = '🚀 Đăng nhập Rikkei Jobs (Tự động 1 chạm)';
      }
    }

    async function handleGenerateTicketManual() {
      if (!currentStudentId) return;

      const btn = document.getElementById('btnManualTicket');
      const originalText = btn.innerHTML;

      try {
        btn.disabled = true;
        btn.textContent = 'Đang tạo mã vé...';

        const data = await fetchTicket(currentStudentId);
        const ticket = data.ticket;

        const ticketBox = document.getElementById('ticketResultBox');
        const ticketInput = document.getElementById('ticketInput');
        ticketInput.value = ticket;
        ticketBox.classList.remove('hidden');

        // Copy to clipboard immediately
        copyTicketToClipboard();

        // Start countdown
        startCountdown(60);

      } catch (err) {
        alert('Lỗi tạo vé: ' + err.message);
      } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
      }
    }

    function startCountdown(durationSeconds) {
      if (countdownTimer) clearInterval(countdownTimer);

      let remaining = durationSeconds;
      const display = document.getElementById('ticketCountdown');
      const bar = document.getElementById('ticketProgressBar');

      display.textContent = remaining + 's';
      bar.style.width = '100%';

      countdownTimer = setInterval(() => {
        remaining--;
        display.textContent = remaining + 's';
        bar.style.width = ((remaining / durationSeconds) * 100) + '%';

        if (remaining <= 0) {
          clearInterval(countdownTimer);
          display.textContent = 'Hết hạn';
          display.className = 'text-slate-400 code-font';
          bar.style.width = '0%';
        }
      }, 1000);
    }

    function copyTicketToClipboard() {
      const input = document.getElementById('ticketInput');
      input.select();
      navigator.clipboard.writeText(input.value).then(() => {
        const btn = document.getElementById('copyBtnText');
        btn.textContent = 'Đã chép!';
        setTimeout(() => {
          btn.textContent = 'Sao chép';
        }, 2000);
      });
    }

    // Initialize on load
    window.addEventListener('DOMContentLoaded', init);
  </script>
</body>
</html>`;
}

module.exports = { renderLmsPortalHtml };
