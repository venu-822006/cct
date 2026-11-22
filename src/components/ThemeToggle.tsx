import { Sun, Moon } from "lucide-react";
import type { Theme } from "../types";

interface ThemeToggleProps {
  theme: Theme;
  onToggle: () => void;
  className?: string;
}

export function ThemeToggle({ theme, onToggle, className = "" }: ThemeToggleProps) {
  const isDark = theme === "gdrive-dark";

  return (
    <button
      type="button"
      onClick={onToggle}
      role="switch"
      aria-checked={isDark}
      title={isDark ? "Switch to light theme" : "Switch to dark theme"}
      aria-label="Toggle theme"
      className={`relative inline-flex h-8 w-14 shrink-0 cursor-pointer items-center rounded-full border border-black/10 dark:border-white/10 transition-colors duration-200 ease-in-out focus:outline-none p-0.5 ${
        isDark ? "bg-[#282a2c]" : "bg-[#edf2fc]"
      } ${className}`}
    >
      <span
        className={`inline-flex h-6.5 w-6.5 transform items-center justify-center rounded-full shadow-sm transition-transform duration-200 ease-in-out ${
          isDark ? "translate-x-6 bg-[#131314] text-blue-400" : "translate-x-0.5 bg-white text-amber-500"
        }`}
      >
        {isDark ? (
          <Moon className="w-3.5 h-3.5" />
        ) : (
          <Sun className="w-3.5 h-3.5" />
        )}
      </span>
    </button>
  );
}
