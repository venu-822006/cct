// Cloudflare Pages Serverless Edge Function - Reverse Proxy for /api/**
// Automatically routes all API requests from Cloudflare Pages domain to the backend cloud service (Cloud Run / Render)

interface Env {
  BACKEND_URL?: string;
}

export const onRequest = async (context: {
  request: Request;
  env: Env;
  params: { path?: string | string[] };
}): Promise<Response> => {
  const backendBaseUrl = (context.env.BACKEND_URL || "https://cct-3c4n.onrender.com").replace(/\/+$/, "");
  const url = new URL(context.request.url);

  // Construct target backend API URL with pathname and query parameters
  const targetUrl = new URL(url.pathname + url.search, backendBaseUrl);

  // Clone headers and forward important client proxy headers
  const headers = new Headers(context.request.headers);
  headers.set("X-Forwarded-Host", url.host);
  headers.set("X-Forwarded-Proto", url.protocol.replace(":", ""));

  const clientIp = context.request.headers.get("CF-Connecting-IP");
  if (clientIp) {
    headers.set("X-Forwarded-For", clientIp);
    headers.set("X-Real-IP", clientIp);
  }

  // Handle CORS preflight cleanly at Cloudflare Edge
  if (context.request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": url.origin,
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD",
        "Access-Control-Allow-Headers": headers.get("Access-Control-Request-Headers") || "*",
        "Access-Control-Allow-Credentials": "true",
        "Access-Control-Max-Age": "86400",
      },
    });
  }

  // Prepare outgoing fetch options
  const fetchOptions: RequestInit = {
    method: context.request.method,
    headers,
    redirect: "follow",
  };

  // Attach body for mutation methods (POST, PUT, PATCH, DELETE)
  if (context.request.method !== "GET" && context.request.method !== "HEAD") {
    fetchOptions.body = context.request.body;
    (fetchOptions as any).duplex = "half";
  }

  try {
    const backendResponse = await fetch(targetUrl.toString(), fetchOptions);

    // Copy backend response headers and add CORS credentials
    const responseHeaders = new Headers(backendResponse.headers);
    responseHeaders.set("Access-Control-Allow-Origin", url.origin);
    responseHeaders.set("Access-Control-Allow-Credentials", "true");
    responseHeaders.set("Access-Control-Expose-Headers", "Content-Disposition, Authorization");

    return new Response(backendResponse.body, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
      headers: responseHeaders,
    });
  } catch (error: any) {
    return new Response(
      JSON.stringify({
        error: "Failed to connect to backend service from Cloudflare edge proxy",
        message: error?.message || String(error),
        targetUrl: targetUrl.origin,
      }),
      {
        status: 502,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": url.origin,
          "Access-Control-Allow-Credentials": "true",
        },
      }
    );
  }
};
