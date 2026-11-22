// Auto-preserve Google Cloud Shell Web Preview session parameter to prevent proxy redirects
if (typeof window !== "undefined" && window.location && window.location.hostname.includes(".cloudshell.dev")) {
  const params = new URLSearchParams(window.location.search || "");
  if (!params.has("authuser")) {
    params.set("authuser", "0");
    const newSearch = "?" + params.toString();
    const targetUrl = window.location.pathname + newSearch + window.location.hash;
    window.location.replace(targetUrl);
  }
}

import { createRoot } from "react-dom/client";
import App from "./App";
import "./styles.css";

const root = document.getElementById("root");
if (root) {
  createRoot(root).render(<App />);
}

