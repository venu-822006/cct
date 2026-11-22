import { useEffect, useState } from "react";
import { DEFAULT_THEME, THEME_STORAGE_KEY, THEMES } from "../constants";
import type { Theme } from "../types";

function getInitialTheme(): Theme {
  if (typeof window === "undefined") {
    return DEFAULT_THEME;
  }
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (stored && THEMES.includes(stored as Theme)) {
    return stored as Theme;
  }
  return DEFAULT_THEME;
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => getInitialTheme());

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    const isDark =
      theme === "gdrive-dark" ||
      theme === "indigo" ||
      theme === "emerald" ||
      theme === "ocean" ||
      theme === "sunset";
    if (isDark) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  return [theme, setTheme] as const;
}
