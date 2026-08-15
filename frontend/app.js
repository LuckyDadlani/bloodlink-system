const API_BASE = "http://localhost:8080/api";
const REFRESH_MS = 10000;

let session = loadSession();
let refreshTimer = null;

const loginView = document.getElementById("login-view");
const dashboardView = document.getElementById("dashboard-view");

const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const hospitalSelector = document.getElementById("hospital-selector");

const bankName = document.getElementById("bank-name");
const bankLocation = document.getElementById("bank-location");
const statTotalUnits = document.getElementById("stat-total-units");
const statActive = document.getElementById("stat-active");
const statFulfilled = document.getElementById("stat-fulfilled");

const inventoryBody = document.getElementById("inventory-body");
const inventoryForm = document.getElementById("inventory-form");
const inventoryStatus = document.getElementById("inventory-status");
const incomingRequestBody = document.getElementById("incoming-request-body");
const requestBody = document.getElementById("request-body");
const requestForm = document.getElementById("request-form");
const requestStatus = document.getElementById("request-status");

const refreshBtn = document.getElementById("refresh-btn");
const logoutBtn = document.getElementById("logout-btn");

init();

function init() {
  loginForm.addEventListener("submit", onLogin);
  inventoryForm.addEventListener("submit", onAddInventory);
  requestForm.addEventListener("submit", onCreateRequest);
  refreshBtn.addEventListener("click", () => loadDashboardData(true));
  logoutBtn.addEventListener("click", onLogout);
  hospitalSelector.addEventListener("change", () => loadDashboardData(true));

  if (session) {
    showDashboard();
  } else {
    showLogin();
  }
}

async function onLogin(event) {
  event.preventDefault();
  setLoginError("");

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;

  try {
    const result = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });

    session = {
      userId: result.userId,
      fullName: result.fullName,
      email: result.email,
      bloodBankId: result.bloodBankId,
      bloodBankName: result.bloodBankName,
      city: result.city,
      state: result.state,
      creatorHospitalId: null
    };

    saveSession(session);
    showDashboard();
  } catch (error) {
    setLoginError(error.message || "Login failed");
  }
}

function showLogin() {
  loginView.hidden = false;
  dashboardView.hidden = true;
  clearInterval(refreshTimer);
}

async function showDashboard() {
  loginView.hidden = true;
  dashboardView.hidden = false;

  bankName.textContent = session.bloodBankName;
  bankLocation.textContent = `${session.city}, ${session.state}`;

  await loadHospitalOptions();
  await loadDashboardData(true);

  clearInterval(refreshTimer);
  refreshTimer = setInterval(() => loadDashboardData(false), REFRESH_MS);
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

async function loadDashboardData(showStatus) {
  if (!session) {
    return;
  }

  session.creatorHospitalId = hospitalSelector.value;
  saveSession(session);

  try {
    const dashboard = await api(
      `/dashboard?bloodBankId=${session.bloodBankId}&creatorHospitalId=${session.creatorHospitalId}`,
      { headers: authHeaders() }
    );
    const inventory = await api(`/inventory/${session.bloodBankId}`, { headers: authHeaders() });
    const requests = await api(`/requests?creatorHospitalId=${session.creatorHospitalId}`, { headers: authHeaders() });
    const incomingRequests = await api(`/requests/bank/${session.bloodBankId}`, { headers: authHeaders() });

    statTotalUnits.textContent = dashboard.totalUnits;
    statActive.textContent = dashboard.activeRequests;
    statFulfilled.textContent = dashboard.fulfilledRequests;

    renderInventory(inventory);
    renderRequests(requests);
    renderIncomingRequests(incomingRequests);

    if (showStatus) {
      requestStatus.hidden = false;
      requestStatus.textContent = `Updated at ${new Date().toLocaleTimeString()}`;
    }
  } catch (error) {
    console.error("Dashboard load failed:", error);
    if (showStatus) {
      requestStatus.hidden = false;
      requestStatus.textContent = `Error loading data: ${error.message}`;
      requestStatus.classList.add("error-text");
      requestStatus.classList.remove("ok-text");
    }
  }
}

function renderInventory(items) {
  inventoryBody.innerHTML = "";

  for (const item of items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${item.bloodGroup}</td>
      <td>${item.componentType}</td>
      <td>${item.unitsAvailable}</td>
      <td>${formatDate(item.lastUpdatedAt)}</td>
      <td>
        <button class="btn btn-ghost edit-btn" data-id="${item.inventoryId}" data-units="${item.unitsAvailable}">Edit</button>
        <button class="btn btn-danger delete-btn" data-id="${item.inventoryId}">Delete</button>
      </td>
    `;

    row.querySelector(".edit-btn").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      const currentUnits = Number(button.dataset.units);
      const value = prompt("Enter new units available:", String(currentUnits));
      if (value === null) {
        return;
      }

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
          unitsAvailable: units
        })
      });

      await loadDashboardData(true);
    });

    row.querySelector(".delete-btn").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      if (!confirm("Are you sure you want to delete this inventory row?")) {
        return;
      }
      try {
        await api(`/inventory/${button.dataset.id}`, {
          method: "DELETE",
          headers: { ...authHeaders(), "X-Blood-Bank-Id": session.bloodBankId }
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
    createdBy: session.userId
  };

  try {
    await api("/inventory", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });

    inventoryStatus.hidden = false;
    inventoryStatus.classList.remove("error-text");
    inventoryStatus.classList.add("ok-text");
    inventoryStatus.textContent = "Inventory row added successfully.";
    
    document.getElementById("inv-units").value = "0";

    await loadDashboardData(true);
  } catch (error) {
    inventoryStatus.hidden = false;
    inventoryStatus.classList.add("error-text");
    inventoryStatus.classList.remove("ok-text");
    inventoryStatus.textContent = `Failed to add inventory: ${error.message}`;
  }
}

function renderRequests(items) {
  requestBody.innerHTML = "";

  for (const req of items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${req.emergencyId}</td>
      <td>${req.bloodGroupRequired}</td>
      <td>${req.unitsRequired}</td>
      <td>${req.status}</td>
      <td>${formatDate(req.createdAt)}</td>
    `;
    requestBody.appendChild(row);
  }
}

function renderIncomingRequests(items) {
  incomingRequestBody.innerHTML = "";

  for (const req of items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${req.emergencyId.substring(0, 8)}...</td>
      <td>${req.bloodGroupRequired}</td>
      <td>${req.componentRequired}</td>
      <td>${req.unitsRequired}</td>
      <td>${req.urgencyLevel}</td>
      <td>${req.hospitalCity}</td>
      <td>${req.status}</td>
      <td>${formatDate(req.createdAt)}</td>
      <td>
        <button class="btn btn-primary fulfill-btn" data-id="${req.emergencyId}" data-units="${req.unitsRequired}">Fulfill</button>
        <button class="btn btn-ghost dismiss-btn" data-id="${req.emergencyId}">Dismiss</button>
      </td>
    `;

    row.querySelector(".fulfill-btn").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      const requestedUnits = Number(button.dataset.units);
      const value = prompt(`Enter units to fulfill (max ${requestedUnits}):`, String(requestedUnits));
      if (!value) return;

      const units = Number(value);
      if (Number.isNaN(units) || units <= 0 || units > requestedUnits) {
        alert("Invalid units amount.");
        return;
      }

      try {
        await api(`/requests/${button.dataset.id}/fulfill`, {
          method: "POST",
          headers: { ...authHeaders(), "X-Blood-Bank-Id": session.bloodBankId },
          body: JSON.stringify({ unitsFulfilled: units })
        });
        await loadDashboardData(true);
      } catch (error) {
        alert("Failed to fulfill: " + error.message);
      }
    });

    row.querySelector(".dismiss-btn").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      try {
        await api(`/requests/${button.dataset.id}/dismiss`, {
          method: "POST",
          headers: { ...authHeaders(), "X-Blood-Bank-Id": session.bloodBankId }
        });
        await loadDashboardData(true);
      } catch (error) {
        alert("Failed to dismiss: " + error.message);
      }
    });

    incomingRequestBody.appendChild(row);
  }
}

async function onCreateRequest(event) {
  event.preventDefault();

  const payload = {
    creatorHospitalId: hospitalSelector.value,
    bloodGroupRequired: document.getElementById("blood-group").value,
    componentRequired: document.getElementById("component").value,
    unitsRequired: Number(document.getElementById("units-required").value),
    urgencyLevel: document.getElementById("urgency").value
  };

  try {
    const created = await api("/requests", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });

    requestStatus.hidden = false;
    requestStatus.classList.remove("error-text");
    requestStatus.classList.add("ok-text");
    requestStatus.textContent = `Request ${created.emergencyId} created. Donors notified: ${created.donorsNotified}.`;

    await loadDashboardData(false);
  } catch (error) {
    requestStatus.hidden = false;
    requestStatus.classList.add("error-text");
    requestStatus.classList.remove("ok-text");
    requestStatus.textContent = `Failed to create request: ${error.message}`;
  }
}

function onLogout() {
  clearSession();
  session = null;
  showLogin();
}

function authHeaders() {
  return {
    "Content-Type": "application/json",
    "X-User-Id": session.userId
  };
}

async function api(path, options = {}) {
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
  if (!value) {
    return "-";
  }
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
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (_ignore) {
    return null;
  }
}

function clearSession() {
  localStorage.removeItem("bloodlink_session");
}
