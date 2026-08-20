const API_BASE = "http://localhost:8080/api";
const REFRESH_MS = 10000;
const FULFILLED_PAGE_SIZE = 5;

let session = loadSession();
let refreshTimer = null;
let fulfilledCache = [];
let fulfilledPage = 0;

/* ---------- element refs ---------- */
const loginView = document.getElementById("login-view");
const appView = document.getElementById("app-view");

const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const hospitalSelector = document.getElementById("hospital-selector");

const sidebarLocation = document.getElementById("sidebar-location");
const userAvatar = document.getElementById("user-avatar");
const pageTitle = document.getElementById("page-title");
const pageSubtitle = document.getElementById("page-subtitle");
const globalStatus = document.getElementById("global-status");

const statTotalUnits = document.getElementById("stat-total-units");
const statActive = document.getElementById("stat-active");
const statFulfilled = document.getElementById("stat-fulfilled");

const inventoryBody = document.getElementById("inventory-body");
const inventoryEmpty = document.getElementById("inventory-empty");
const inventoryForm = document.getElementById("inventory-form");
const inventoryStatus = document.getElementById("inventory-status");
const inventoryFilter = document.getElementById("inventory-filter");

const incomingRequestBody = document.getElementById("incoming-request-body");
const incomingEmpty = document.getElementById("incoming-empty");

const requestBody = document.getElementById("request-body");
const requestsEmpty = document.getElementById("requests-empty");
const requestForm = document.getElementById("request-form");
const requestStatus = document.getElementById("request-status");

const fulfilledBody = document.getElementById("fulfilled-body");
const fulfilledEmpty = document.getElementById("fulfilled-empty");
const fulfilledFilter = document.getElementById("fulfilled-filter");
const fulfilledPrev = document.getElementById("fulfilled-prev");
const fulfilledNext = document.getElementById("fulfilled-next");
const fulfilledPaginationLabel = document.getElementById(
  "fulfilled-pagination-label",
);
const statUnitsDistributed = document.getElementById("stat-units-distributed");
const statAvgTurnaround = document.getElementById("stat-avg-turnaround");
const statTopComponent = document.getElementById("stat-top-component");

const refreshBtn = document.getElementById("refresh-btn");
const requestsRefreshBtn = document.getElementById("requests-refresh-btn");
const logoutBtn = document.getElementById("logout-btn");

const navItems = document.querySelectorAll(".nav-item");
const views = {
  home: document.getElementById("view-home"),
  requests: document.getElementById("view-requests"),
  inventory: document.getElementById("view-inventory"),
  fulfilled: document.getElementById("view-fulfilled"),
};
const viewMeta = {
  home: { title: "Home", subtitle: "Daily Overview" },
  requests: {
    title: "Blood Requests",
    subtitle: "Create and track your emergency requests",
  },
  inventory: {
    title: "Inventory Management",
    subtitle: "Monitor and update current blood stock levels.",
  },
  fulfilled: {
    title: "Fulfilled Requests",
    subtitle: "Review completed distributions to regional facilities.",
  },
};

init();

function init() {
  loginForm.addEventListener("submit", onLogin);
  inventoryForm.addEventListener("submit", onAddInventory);
  requestForm.addEventListener("submit", onCreateRequest);
  refreshBtn.addEventListener("click", () => loadDashboardData(true));
  requestsRefreshBtn?.addEventListener("click", () => loadDashboardData(true));
  logoutBtn.addEventListener("click", onLogout);
  hospitalSelector.addEventListener("change", () => loadDashboardData(true));
  inventoryFilter.addEventListener("input", () => filterInventoryTable());
  fulfilledFilter.addEventListener("input", () => {
    fulfilledPage = 0;
    renderFulfilled();
  });
  fulfilledPrev.addEventListener("click", () => {
    fulfilledPage--;
    renderFulfilled();
  });
  fulfilledNext.addEventListener("click", () => {
    fulfilledPage++;
    renderFulfilled();
  });

  navItems.forEach((item) => {
    item.addEventListener("click", () => switchView(item.dataset.view));
  });

  if (session) {
    showDashboard();
  } else {
    showLogin();
  }
}

/* ---------- navigation ---------- */
function switchView(name) {
  navItems.forEach((item) =>
    item.classList.toggle("active", item.dataset.view === name),
  );
  Object.entries(views).forEach(([key, el]) => {
    el.hidden = key !== name;
  });
  pageTitle.textContent = viewMeta[name].title;
  pageSubtitle.textContent = viewMeta[name].subtitle;
}

/* ---------- auth ---------- */
async function onLogin(event) {
  event.preventDefault();
  setLoginError("");

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;

  try {
    const result = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });

    session = {
      userId: result.userId,
      fullName: result.fullName,
      email: result.email,
      bloodBankId: result.bloodBankId,
      bloodBankName: result.bloodBankName,
      city: result.city,
      state: result.state,
      creatorHospitalId: null,
    };

    saveSession(session);
    showDashboard();
  } catch (error) {
    setLoginError(error.message || "Login failed");
  }
}

function showLogin() {
  loginView.hidden = false;
  appView.hidden = true;
  clearInterval(refreshTimer);
}

async function showDashboard() {
  loginView.hidden = true;
  appView.hidden = false;

  sidebarLocation.textContent = `${session.city}, ${session.state}`;
  userAvatar.textContent = initialsFor(
    session.fullName || session.bloodBankName,
  );
  switchView("home");

  await loadHospitalOptions();
  await loadDashboardData(true);

  clearInterval(refreshTimer);
  refreshTimer = setInterval(() => loadDashboardData(false), REFRESH_MS);
}

function initialsFor(name) {
  if (!name) return "?";
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase())
      .join("") || "?"
  );
}

async function loadHospitalOptions() {
  const hospitals = await api("/meta/hospitals", { headers: authHeaders() });
  hospitalSelector.innerHTML = "";

  hospitals.forEach((hospital, idx) => {
    const option = document.createElement("option");
    option.value = hospital.hospitalId;
    option.textContent = `${hospital.hospitalName} (${hospital.city})`;
    if (session.creatorHospitalId) {
      option.selected = session.creatorHospitalId === hospital.hospitalId;
    } else if (idx === 0) {
      option.selected = true;
    }
    hospitalSelector.appendChild(option);
  });

  session.creatorHospitalId = hospitalSelector.value;
  saveSession(session);
}

/* ---------- data loading ---------- */
async function loadDashboardData(showStatus) {
  if (!session) return;

  session.creatorHospitalId = hospitalSelector.value;
  saveSession(session);

  try {
    const dashboard = await api(
      `/dashboard?bloodBankId=${session.bloodBankId}&creatorHospitalId=${session.creatorHospitalId}`,
      { headers: authHeaders() },
    );
    const inventory = await api(`/inventory/${session.bloodBankId}`, {
      headers: authHeaders(),
    });
    const requests = await api(
      `/requests?creatorHospitalId=${session.creatorHospitalId}`,
      { headers: authHeaders() },
    );
    const incomingRequests = await api(
      `/requests/bank/${session.bloodBankId}`,
      { headers: authHeaders() },
    );

    statTotalUnits.textContent = dashboard.totalUnits;
    statActive.textContent = dashboard.activeRequests;
    statFulfilled.textContent = dashboard.fulfilledRequests;

    renderInventory(inventory);
    renderRequests(requests);
    renderIncomingRequests(incomingRequests);

    // Fulfilled Requests view is derived from this hospital context's own
    // request history, since the API does not (yet) expose a bank-wide
    // fulfillment feed. Swap this for a dedicated endpoint if you add one.
    fulfilledCache = requests.filter((r) => isFulfilledStatus(r.status));
    fulfilledPage = 0;
    renderFulfilled();

    if (showStatus)
      showGlobalStatus(`Updated at ${new Date().toLocaleTimeString()}`, false);
  } catch (error) {
    console.error("Dashboard load failed:", error);
    if (showStatus)
      showGlobalStatus(`Error loading data: ${error.message}`, true);
  }
}

function showGlobalStatus(text, isError) {
  globalStatus.hidden = false;
  globalStatus.textContent = text;
  globalStatus.classList.toggle("error-text", !!isError);
}

/* ---------- inventory ---------- */
function renderInventory(items) {
  inventoryBody.innerHTML = "";
  inventoryBody.dataset.raw = JSON.stringify(items);
  paintInventoryRows(items);
}

function filterInventoryTable() {
  const raw = JSON.parse(inventoryBody.dataset.raw || "[]");
  const term = inventoryFilter.value.trim().toLowerCase();
  const filtered = term
    ? raw.filter((i) =>
        `${i.bloodGroup} ${i.componentType}`.toLowerCase().includes(term),
      )
    : raw;
  paintInventoryRows(filtered, raw.length);
}

function paintInventoryRows(items, totalCount) {
  inventoryBody.innerHTML = "";
  inventoryEmpty.hidden = items.length > 0;

  for (const item of items) {
    const row = document.createElement("tr");
    const low = item.unitsAvailable <= 5;
    if (low) row.classList.add("row-flag");

    row.innerHTML = `
      <td>${groupBadge(item.bloodGroup)}</td>
      <td>${item.componentType}</td>
      <td>${low ? unitsWarning(item.unitsAvailable) : item.unitsAvailable}</td>
      <td>${formatDate(item.lastUpdatedAt)}</td>
      <td>
        <div class="row-actions">
          <button class="btn btn-outline btn-sm edit-btn" data-id="${item.inventoryId}" data-units="${item.unitsAvailable}">Edit</button>
          <button class="btn btn-danger btn-sm delete-btn" data-id="${item.inventoryId}">Delete</button>
        </div>
      </td>
    `;

    row.querySelector(".edit-btn").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      const currentUnits = Number(button.dataset.units);
      const value = prompt("Enter new units available:", String(currentUnits));
      if (value === null) return;

      const units = Number(value);
      if (Number.isNaN(units) || units < 0) {
        alert("Units must be a non-negative number.");
        return;
      }

      await api(`/inventory/${button.dataset.id}`, {
        method: "PUT",
        headers: authHeaders(),
        body: JSON.stringify({
          updatedBy: session.userId,
          unitsAvailable: units,
        }),
      });

      await loadDashboardData(true);
    });

    row
      .querySelector(".delete-btn")
      .addEventListener("click", async (event) => {
        const button = event.currentTarget;
        if (!confirm("Are you sure you want to delete this inventory row?"))
          return;
        try {
          await api(`/inventory/${button.dataset.id}`, {
            method: "DELETE",
            headers: {
              ...authHeaders(),
              "X-Blood-Bank-Id": session.bloodBankId,
            },
          });
          await loadDashboardData(true);
        } catch (error) {
          alert("Failed to delete: " + error.message);
        }
      });

    inventoryBody.appendChild(row);
  }
}

async function onAddInventory(event) {
  event.preventDefault();

  const payload = {
    bloodBankId: session.bloodBankId,
    bloodGroup: document.getElementById("inv-blood-group").value,
    componentType: document.getElementById("inv-component").value,
    unitsAvailable: Number(document.getElementById("inv-units").value),
    createdBy: session.userId,
  };

  try {
    await api("/inventory", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(payload),
    });

    inventoryStatus.hidden = false;
    inventoryStatus.classList.remove("error-text");
    inventoryStatus.textContent = "Inventory row added successfully.";
    document.getElementById("inv-units").value = "0";

    await loadDashboardData(true);
  } catch (error) {
    inventoryStatus.hidden = false;
    inventoryStatus.classList.add("error-text");
    inventoryStatus.textContent = `Failed to add inventory: ${error.message}`;
  }
}

/* ---------- my created requests ---------- */
function renderRequests(items) {
  requestBody.innerHTML = "";
  requestsEmpty.hidden = items.length > 0;

  for (const req of items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${shortId(req.emergencyId)}</td>
      <td>${groupBadge(req.bloodGroupRequired)}</td>
      <td>${req.componentRequired ?? "-"}</td>
      <td>${req.unitsRequired}</td>
      <td>${statusPill(req.status)}</td>
      <td>${formatDate(req.createdAt)}</td>
    `;
    requestBody.appendChild(row);
  }
}

/* ---------- incoming requests (home) ---------- */
function renderIncomingRequests(items) {
  incomingRequestBody.innerHTML = "";
  incomingEmpty.hidden = items.length > 0;

  for (const req of items) {
    const row = document.createElement("tr");
    const critical = req.urgencyLevel === "CRITICAL";
    row.innerHTML = `
      <td>${shortId(req.emergencyId)}</td>
      <td>${groupBadge(req.bloodGroupRequired)}</td>
      <td>${req.componentRequired}</td>
      <td>${req.unitsRequired}</td>
      <td>${urgencyPill(req.urgencyLevel)}</td>
      <td>${req.hospitalCity ?? "-"}</td>
      <td>${statusPill(req.status, true)}</td>
      <td>${formatDate(req.createdAt)}</td>
      <td>
        <div class="row-actions">
          <button class="btn ${critical ? "btn-fulfill-critical" : "btn-outline"} btn-sm fulfill-btn" data-id="${req.emergencyId}" data-units="${req.unitsRequired}">Fulfill</button>
          <button class="icon-x-btn dismiss-btn" data-id="${req.emergencyId}" title="Dismiss">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="M6 6l12 12"/></svg>
          </button>
        </div>
      </td>
    `;

    row
      .querySelector(".fulfill-btn")
      .addEventListener("click", async (event) => {
        const button = event.currentTarget;
        const requestedUnits = Number(button.dataset.units);
        const value = prompt(
          `Enter units to fulfill (max ${requestedUnits}):`,
          String(requestedUnits),
        );
        if (!value) return;

        const units = Number(value);
        if (Number.isNaN(units) || units <= 0 || units > requestedUnits) {
          alert("Invalid units amount.");
          return;
        }

        try {
          await api(`/requests/${button.dataset.id}/fulfill`, {
            method: "POST",
            headers: {
              ...authHeaders(),
              "X-Blood-Bank-Id": session.bloodBankId,
            },
            body: JSON.stringify({ unitsFulfilled: units }),
          });
          await loadDashboardData(true);
        } catch (error) {
          alert("Failed to fulfill: " + error.message);
        }
      });

    row
      .querySelector(".dismiss-btn")
      .addEventListener("click", async (event) => {
        const button = event.currentTarget;
        try {
          await api(`/requests/${button.dataset.id}/dismiss`, {
            method: "POST",
            headers: {
              ...authHeaders(),
              "X-Blood-Bank-Id": session.bloodBankId,
            },
          });
          await loadDashboardData(true);
        } catch (error) {
          alert("Failed to dismiss: " + error.message);
        }
      });

    incomingRequestBody.appendChild(row);
  }
}

/* ---------- fulfilled requests ---------- */
function isFulfilledStatus(status) {
  return ["FULFILLED_BY_BANK", "DONOR_CONFIRMED", "CLOSED"].includes(status);
}

function renderFulfilled() {
  const term = fulfilledFilter.value.trim().toLowerCase();
  const filtered = term
    ? fulfilledCache.filter(
        (r) =>
          r.emergencyId.toLowerCase().includes(term) ||
          (r.hospitalCity || "").toLowerCase().includes(term),
      )
    : fulfilledCache;

  // stats
  const unitsDistributed = filtered.reduce(
    (sum, r) => sum + (r.unitsFulfilled || 0),
    0,
  );
  statUnitsDistributed.textContent = unitsDistributed;

  const turnarounds = filtered
    .filter((r) => r.createdAt && r.closedAt)
    .map((r) => (new Date(r.closedAt) - new Date(r.createdAt)) / 60000);
  statAvgTurnaround.textContent = turnarounds.length
    ? `${Math.round(turnarounds.reduce((a, b) => a + b, 0) / turnarounds.length)}m`
    : "-";

  const componentCounts = {};
  filtered.forEach((r) => {
    const key = `${r.componentRequired} (${r.bloodGroupRequired})`;
    componentCounts[key] = (componentCounts[key] || 0) + 1;
  });
  const top = Object.entries(componentCounts).sort((a, b) => b[1] - a[1])[0];
  statTopComponent.textContent = top ? top[0] : "-";

  // pagination
  const total = filtered.length;
  const totalPages = Math.max(1, Math.ceil(total / FULFILLED_PAGE_SIZE));
  fulfilledPage = Math.min(fulfilledPage, totalPages - 1);
  const start = fulfilledPage * FULFILLED_PAGE_SIZE;
  const pageItems = filtered.slice(start, start + FULFILLED_PAGE_SIZE);

  fulfilledBody.innerHTML = "";
  fulfilledEmpty.hidden = pageItems.length > 0;

  for (const req of pageItems) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${shortId(req.emergencyId)}</td>
      <td>${groupBadge(req.bloodGroupRequired)}</td>
      <td>${req.componentRequired ?? "-"}</td>
      <td>${req.unitsFulfilled}</td>
      <td>${req.hospitalCity ?? "-"}</td>
      <td>${statusPill(req.status)}</td>
      <td>${formatDate(req.closedAt || req.createdAt)}</td>
    `;
    fulfilledBody.appendChild(row);
  }

  fulfilledPaginationLabel.textContent = total
    ? `Showing ${start + 1}-${Math.min(start + FULFILLED_PAGE_SIZE, total)} of ${total} records`
    : "Showing 0-0 of 0 records";
  fulfilledPrev.disabled = fulfilledPage <= 0;
  fulfilledNext.disabled = start + FULFILLED_PAGE_SIZE >= total;
}

async function onCreateRequest(event) {
  event.preventDefault();

  const payload = {
    creatorHospitalId: hospitalSelector.value,
    bloodGroupRequired: document.getElementById("blood-group").value,
    componentRequired: document.getElementById("component").value,
    unitsRequired: Number(document.getElementById("units-required").value),
    urgencyLevel: document.getElementById("urgency").value,
  };

  try {
    const created = await api("/requests", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(payload),
    });

    requestStatus.hidden = false;
    requestStatus.classList.remove("error-text");
    requestStatus.textContent = `Request ${shortId(created.emergencyId)} created. Donors notified: ${created.donorsNotified}.`;

    await loadDashboardData(false);
  } catch (error) {
    requestStatus.hidden = false;
    requestStatus.classList.add("error-text");
    requestStatus.textContent = `Failed to create request: ${error.message}`;
  }
}

function onLogout() {
  clearSession();
  session = null;
  showLogin();
}

/* ---------- render helpers ---------- */
function groupBadge(group) {
  return `<span class="group-badge">${group ?? "-"}</span>`;
}

function urgencyPill(level) {
  const map = {
    CRITICAL: "pill-critical",
    HIGH: "pill-high",
    ROUTINE: "pill-routine",
  };
  return `<span class="pill ${map[level] || "pill-routine"}">${level || "ROUTINE"}</span>`;
}

function statusPill(status, isIncoming) {
  if (isFulfilledStatus(status))
    return `<span class="pill pill-fulfilled">FULFILLED</span>`;
  if (status === "DISMISSED")
    return `<span class="pill pill-dismissed">DISMISSED</span>`;
  if (isIncoming) return `<span class="pill pill-pending">PENDING</span>`;
  return `<span class="pill pill-active">ACTIVE</span>`;
}

function unitsWarning(units) {
  return `<span class="units-warn"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>${units}</span>`;
}

function shortId(id) {
  return id ? `${id.substring(0, 8)}...` : "-";
}

/* ---------- plumbing ---------- */
function authHeaders() {
  return { "Content-Type": "application/json", "X-User-Id": session.userId };
}

async function api(path, options = {}) {
  const config = {
    method: "GET",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  };

  const response = await fetch(`${API_BASE}${path}`, config);
  let data = null;
  try {
    data = await response.json();
  } catch (_ignore) {
    data = null;
  }

  if (!response.ok) {
    const message = data?.error || `Request failed (${response.status})`;
    throw new Error(message);
  }

  return data;
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString();
}

function setLoginError(message) {
  loginError.hidden = !message;
  loginError.textContent = message || "";
}

function saveSession(data) {
  localStorage.setItem("bloodlink_session", JSON.stringify(data));
}

function loadSession() {
  const raw = localStorage.getItem("bloodlink_session");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (_ignore) {
    return null;
  }
}

function clearSession() {
  localStorage.removeItem("bloodlink_session");
}
