import React, { useState } from "react";
import { Cloud, Lock, User, Mail, UserCheck, ArrowRight, ShieldCheck, HardDrive, Palette } from "lucide-react";
import { loginUser, registerUser, setAuthToken, type UserProfile } from "../services/springApi";
import { useTheme } from "../hooks/useTheme";
import { THEMES, THEME_CONFIGS } from "../constants";
import type { Theme } from "../types";

interface AuthModalProps {
  onSuccess: (user: UserProfile) => void;
}

export function AuthModal({ onSuccess }: AuthModalProps) {
  const [theme, setTheme] = useTheme();
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        if (!username || !email || !password) {
          throw new Error("Please fill in all required fields.");
        }
        if (password.length < 6) {
          throw new Error("Password must be at least 6 characters.");
        }
        const res = await registerUser(username, email, password, fullName);
        if (res.token) {
          setAuthToken(res.token);
          onSuccess(res);
        }
      } else {
        if (!username || !password) {
          throw new Error("Please enter both username and password.");
        }
        const res = await loginUser(username, password);
        if (res.token) {
          setAuthToken(res.token);
          onSuccess(res);
        }
      }
    } catch (err: any) {
      setError(err.message || "Authentication failed. Please check your credentials.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen theme-bg-app flex flex-col items-center justify-center p-4 relative overflow-hidden font-sans transition-colors duration-200">
      {/* Background Glow */}
      <div className="absolute top-1/4 -left-20 w-96 h-96 rounded-full blur-3xl pointer-events-none" style={{ backgroundColor: "var(--app-glow-1)" }} />
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 rounded-full blur-3xl pointer-events-none" style={{ backgroundColor: "var(--app-glow-2)" }} />

      {/* Floating Theme Selector */}
      <div className="absolute top-6 right-6 z-20">
        <div className="relative">
          <button
            onClick={() => setThemeMenuOpen(!themeMenuOpen)}
            className="flex items-center gap-2 px-3 py-2 rounded-xl theme-bg-surface border theme-border theme-text-sub hover:theme-text-main shadow-md transition-all cursor-pointer text-xs font-medium"
            title="Change Color Theme"
          >
            <Palette className="w-3.5 h-3.5 text-[var(--app-primary-light)]" />
            <span>{THEME_CONFIGS[theme as keyof typeof THEME_CONFIGS]?.name || "Theme"}</span>
          </button>

          {themeMenuOpen && (
            <div className="absolute right-0 mt-2 w-48 theme-bg-surface border theme-border rounded-xl shadow-2xl p-1.5 z-50 space-y-1 animate-fadeUp">
              {THEMES.map((t) => {
                const conf = THEME_CONFIGS[t as keyof typeof THEME_CONFIGS];
                if (!conf) return null;
                const isSelected = theme === t;
                return (
                  <button
                    key={t}
                    onClick={() => {
                      setTheme(t);
                      setThemeMenuOpen(false);
                    }}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                      isSelected
                        ? "theme-pill-active font-semibold"
                        : "theme-text-sub hover:theme-text-main theme-bg-hover"
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span className={`w-2.5 h-2.5 rounded-full ${conf.dotClass}`} />
                      <span>{conf.name}</span>
                    </div>
                    {isSelected && <span className="text-[10px]">✓</span>}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Main Card */}
      <div className="w-full max-w-md theme-bg-surface border theme-border rounded-2xl shadow-2xl p-8 backdrop-blur-xl relative z-10 transition-colors">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center mb-8">
          <div className="w-16 h-16 rounded-2xl theme-btn-primary flex items-center justify-center shadow-lg mb-4">
            <Cloud className="w-9 h-9 text-white" />
          </div>
          <h1 className="text-2xl font-bold theme-text-main tracking-tight">Personal Cloud Drive</h1>
          <p className="text-sm theme-text-muted mt-1">
            Secure, high-performance personal cloud storage
          </p>
        </div>

        {/* Auth Tab Switcher */}
        <div className="flex theme-bg-input p-1 rounded-xl mb-6 border theme-border-subtle">
          <button
            type="button"
            className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-all ${
              !isRegister
                ? "theme-btn-primary font-semibold shadow-sm"
                : "theme-text-muted hover:theme-text-main"
            }`}
            onClick={() => {
              setIsRegister(false);
              setError(null);
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-all ${
              isRegister
                ? "theme-btn-primary font-semibold shadow-sm"
                : "theme-text-muted hover:theme-text-main"
            }`}
            onClick={() => {
              setIsRegister(true);
              setError(null);
            }}
          >
            Create Account
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-6 p-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm flex flex-col gap-1.5">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-red-500 shrink-0" />
              <span>{error}</span>
            </div>
            {isRegister && (error.toLowerCase().includes("taken") || error.toLowerCase().includes("registered") || error.toLowerCase().includes("exists")) && (
              <button
                type="button"
                onClick={() => {
                  setIsRegister(false);
                  setError(null);
                }}
                className="text-xs font-semibold text-blue-400 hover:text-blue-300 underline text-left pl-3.5 cursor-pointer"
              >
                Already have this account? Click here to Sign In &rarr;
              </button>
            )}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="block text-xs font-semibold theme-text-sub uppercase tracking-wider mb-1.5">
                Full Name
              </label>
              <div className="relative">
                <UserCheck className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
                <input
                  type="text"
                  required={isRegister}
                  placeholder="e.g. Alex Johnson"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl pl-11 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] transition-colors"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold theme-text-sub uppercase tracking-wider mb-1.5">
              Username
            </label>
            <div className="relative">
              <User className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
              <input
                type="text"
                required
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full theme-bg-input border theme-border rounded-xl pl-11 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] transition-colors"
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-semibold theme-text-sub uppercase tracking-wider mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
                <input
                  type="email"
                  required={isRegister}
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full theme-bg-input border theme-border rounded-xl pl-11 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] transition-colors"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold theme-text-sub uppercase tracking-wider mb-1.5">
              Password
            </label>
            <div className="relative">
              <Lock className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
              <input
                type="password"
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full theme-bg-input border theme-border rounded-xl pl-11 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] transition-colors"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-3 theme-btn-primary font-semibold rounded-xl flex items-center justify-center gap-2 disabled:opacity-60 transition-all cursor-pointer"
          >
            {loading ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <span>{isRegister ? "Register & Access Drive" : "Sign In to Drive"}</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        {/* Security Footer Notes */}
        <div className="mt-8 pt-6 border-t theme-border-subtle flex items-center justify-between text-xs theme-text-muted">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Encrypted & Private</span>
          </div>
          <div className="flex items-center gap-1.5">
            <HardDrive className="w-4 h-4 text-[var(--app-primary-light)]" />
            <span>500 MB Free Storage</span>
          </div>
        </div>
      </div>
    </div>
  );
}
