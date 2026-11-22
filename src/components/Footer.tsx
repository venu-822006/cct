import { ShieldCheck, HardDrive } from "lucide-react";
import { BRAND_TITLE } from "../constants";

export function Footer() {
    const year = new Date().getFullYear();

    return (
        <footer className="w-full py-6 mt-auto border-t border-[rgba(28,37,43,0.05)]">
            <div className="flex flex-col items-center justify-center gap-2 text-sm text-[var(--muted)]">
                <p className="font-light">
                    &copy; {year} {BRAND_TITLE}. Secure Personal Cloud Storage.
                </p>
                <div className="flex items-center gap-4 text-xs opacity-75">
                    <span className="flex items-center gap-1">
                        <ShieldCheck size={14} className="text-emerald-500" />
                        <span>Encrypted & Private</span>
                    </span>
                    <span className="opacity-30">•</span>
                    <span className="flex items-center gap-1">
                        <HardDrive size={14} className="text-blue-500" />
                        <span>Self-Hosted Storage</span>
                    </span>
                </div>
            </div>
        </footer>
    );
}
