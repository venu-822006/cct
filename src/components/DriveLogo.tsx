export function DriveLogo({ className = "w-9 h-9" }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="driveLogoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#2563eb" />
          <stop offset="100%" stopColor="#1d4ed8" />
        </linearGradient>
      </defs>
      <rect width="36" height="36" rx="10" fill="url(#driveLogoGrad)" />
      <path
        d="M26 22H11a4 4 0 0 1-.95-7.88 5.5 5.5 0 0 1 10.74-1.92A4 4 0 0 1 26 22z"
        fill="#ffffff"
      />
      <rect x="10" y="24" width="16" height="2.5" rx="1.25" fill="#93c5fd" />
      <circle cx="23.5" cy="25.25" r="0.75" fill="#1d4ed8" />
    </svg>
  );
}
