const API_BASE = "http://localhost:8080/api";

export function getSession() {
  const raw = localStorage.getItem("bloodlink_session");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveSession(data) {
  localStorage.setItem("bloodlink_session", JSON.stringify(data));
}

export function clearSession() {
  localStorage.removeItem("bloodlink_session");
}

export function authHeaders() {
  const session = getSession();
  const headers = {
    "Content-Type": "application/json"
  };
  
  if (session && session.userId) {
    headers["X-User-Id"] = session.userId;
  }
  
  if (session && session.bloodBankId) {
    headers["X-Blood-Bank-Id"] = session.bloodBankId;
  }
  
  return headers;
}

export async function api(path, options = {}) {
  const config = {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  };

  const response = await fetch(`${API_BASE}${path}`, config);
  let data = null;
  
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const message = data?.error || data?.message || `Request failed (${response.status})`;
    throw new Error(message);
  }

  return data;
}

export function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString();
}
