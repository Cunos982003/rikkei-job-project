import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { Toaster } from "sonner";
import { queryClient } from "./lib/queryClient";
import { AppRoutes } from "./routes/AppRoutes";
import { ErrorBoundary } from "./components/common/ErrorBoundary";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ErrorBoundary>
          <AppRoutes />
        </ErrorBoundary>
      </BrowserRouter>
      <Toaster
        position="top-right"
        richColors
        closeButton
        duration={3500}
        toastOptions={{
          className: "text-xs font-sans",
        }}
      />
    </QueryClientProvider>
  );
}

export default App;
