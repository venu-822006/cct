import React, { useState } from "react";
import { Lock, User, Mail, UserCheck, ArrowRight, Eye, EyeOff } from "lucide-react";
import { loginUser, registerUser, setAuthToken, type UserProfile } from "../services/springApi";
import { useTheme } from "../hooks/useTheme";
import { DriveLogo } from "./DriveLogo";
import { ThemeToggle } from "./ThemeToggle";

export { DriveLogo };

interface AuthModalProps {
  onSuccess: (user: UserProfile) => void;
}

export function AuthModal({ onSuccess }: AuthModalProps) {
  const [theme, , toggleTheme] = useTheme();
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
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
      {/* Floating Theme Toggle Switch */}
      <div className="absolute top-6 right-6 z-20">
        <ThemeToggle theme={theme} onToggle={toggleTheme} />
      </div>

      {/* Main Account Card */}
      <div className="w-full max-w-md theme-bg-surface border theme-border rounded-3xl shadow-xl p-8 sm:p-10 relative z-10 transition-colors">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center mb-6">
          <div className="mb-3">
            <DriveLogo className="w-14 h-14" />
          </div>
          <h1 className="text-2xl font-normal theme-text-main tracking-tight font-sans">
            Cloud Drive
          </h1>
          <p className="text-sm theme-text-sub mt-1">
            {isRegister ? "Create account" : "Sign in"}
          </p>
        </div>

        {/* Auth Tab Switcher */}
        <div className="flex theme-bg-subtle p-1 rounded-full mb-6 border theme-border-subtle">
          <button
            type="button"
            className={`flex-1 py-2 text-xs font-medium rounded-full transition-all ${
              !isRegister
                ? "bg-white dark:bg-[#282a2c] shadow-sm theme-text-main font-semibold"
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
            className={`flex-1 py-2 text-xs font-medium rounded-full transition-all ${
              isRegister
                ? "bg-white dark:bg-[#282a2c] shadow-sm theme-text-main font-semibold"
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
          <div className="mb-6 p-3.5 bg-red-500/10 border border-red-500/25 rounded-2xl text-red-600 dark:text-red-400 text-xs flex flex-col gap-1.5">
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
                className="text-xs font-medium text-blue-600 dark:text-blue-400 hover:underline text-left pl-3.5 cursor-pointer"
              >
                Sign in instead
              </button>
            )}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="block text-xs font-medium theme-text-sub mb-1.5">
                Full Name
              </label>
              <div className="relative">
                <UserCheck className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
                <input
                  type="text"
                  required={isRegister}
                  placeholder="e.g. Alex Johnson"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full theme-bg-subtle border theme-border rounded-xl pl-10 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] focus:bg-white dark:focus:bg-[#1e1f20] transition-colors"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium theme-text-sub mb-1.5">
              Username
            </label>
            <div className="relative">
              <User className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
              <input
                type="text"
                required
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full theme-bg-subtle border theme-border rounded-xl pl-10 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] focus:bg-white dark:focus:bg-[#1e1f20] transition-colors"
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-medium theme-text-sub mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
                <input
                  type="email"
                  required={isRegister}
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full theme-bg-subtle border theme-border rounded-xl pl-10 pr-4 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] focus:bg-white dark:focus:bg-[#1e1f20] transition-colors"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium theme-text-sub mb-1.5">
              Password
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 theme-text-muted" />
              <input
                type={showPassword ? "text" : "password"}
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full theme-bg-subtle border theme-border rounded-xl pl-10 pr-10 py-2.5 text-sm theme-text-main placeholder-[var(--app-text-muted)] focus:outline-none focus:border-[var(--app-border-focus)] focus:bg-white dark:focus:bg-[#1e1f20] transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 theme-text-muted hover:theme-text-main p-1 rounded-md focus:outline-none cursor-pointer transition-colors"
                aria-label={showPassword ? "Hide password" : "Show password"}
                title={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-4 py-2.5 theme-btn-primary font-medium rounded-full flex items-center justify-center gap-2 disabled:opacity-60 transition-all cursor-pointer text-sm shadow-sm hover:shadow"
          >
            {loading ? (
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <span>{isRegister ? "Create account" : "Sign in"}</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
