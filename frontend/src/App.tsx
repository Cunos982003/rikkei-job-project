import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { SkillProfilePage } from "@/features/profile/SkillProfilePage";
import EvaluationsPage from "@/features/profile/EvaluationsPage";
// 1. Import trang InternshipsPage vừa tạo
import InternshipsPage from "@/features/profile/InternshipsPage";

const queryClient = new QueryClient({
    defaultOptions: {
        queries: { staleTime: 1000 * 60 * 5, retry: 1, refetchOnWindowFocus: false },
    },
});

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Navigate to="/profile/skills" replace />} />
                    <Route path="/profile/skills" element={<SkillProfilePage />} />
                    <Route path="/profile/:userId/skills" element={<SkillProfilePage />} />

                    <Route path="/profile/evaluations" element={<EvaluationsPage />} />

                    {/* 2. Thêm Route cho trang Thực tập IOC */}
                    <Route path="/profile/internships" element={<InternshipsPage />} />

                    <Route path="*" element={<Navigate to="/profile/skills" replace />} />
                </Routes>
            </BrowserRouter>
        </QueryClientProvider>
    );
}

export default App;