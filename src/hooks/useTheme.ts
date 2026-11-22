import { useEffect, useState } from "react";
import { DEFAULT_THEME, THEME_STORAGE_KEY } from "../constants";
import type { Theme } from "../types";

function getInitialTheme(): Theme {
  if (typeof window === "undefined") {
    return DEFAULT_THEME;
  }
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === "gdrive-dark" || stored === "dark") {
    return "gdrive-dark";
  }
  return "gdrive-light";
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => getInitialTheme());

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    if (theme === "gdrive-dark") {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "gdrive-dark" ? "gdrive-light" : "gdrive-dark"));
  };

  return [theme, setTheme, toggleTheme] as const;
}
