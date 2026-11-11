import { create } from "zustand";

interface UiState {
  isSidebarOpen: boolean;
  isMobileSidebarOpen: boolean;
  isRoleSwitcherOpen: boolean;
  globalSearchQuery: string;

  // Actions
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  toggleMobileSidebar: () => void;
  openMobileSidebar: () => void;
  closeMobileSidebar: () => void;
  setRoleSwitcherOpen: (open: boolean) => void;
  setGlobalSearchQuery: (query: string) => void;
}

export const useUiStore = create<UiState>((set) => ({
  isSidebarOpen: true,
  isMobileSidebarOpen: false,
  isRoleSwitcherOpen: false,
  globalSearchQuery: "",

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
  setSidebarOpen: (open) => set({ isSidebarOpen: open }),

  toggleMobileSidebar: () => set((state) => ({ isMobileSidebarOpen: !state.isMobileSidebarOpen })),
  openMobileSidebar: () => set({ isMobileSidebarOpen: true }),
  closeMobileSidebar: () => set({ isMobileSidebarOpen: false }),

  setRoleSwitcherOpen: (open) => set({ isRoleSwitcherOpen: open }),
  setGlobalSearchQuery: (query) => set({ globalSearchQuery: query }),
}));

