import { getSession, saveSession, authHeaders, api } from './api.js';
import { setupLogin } from './views/Login.js';
import { renderHomeView } from './views/Home.js';
import { renderBloodRequestsView } from './views/BloodRequests.js';
import { renderInventoryView } from './views/Inventory.js';
import { renderFulfilledRequestsView } from './views/FulfilledRequests.js';
import { setupSidebar } from './components/Sidebar.js';
import { showToast } from './components/Toast.js';

let currentView = 'home';
let refreshTimer = null;
const REFRESH_MS = 10000;

document.addEventListener('DOMContentLoaded', () => {
  init();
});

function init() {
  const session = getSession();
  
  // Setup static listeners
  setupLogin();
  setupSidebar();
  
  const refreshBtn = document.getElementById('refresh-btn');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => loadDashboardData(true));
  }

  const hospitalSelector = document.getElementById('hospital-selector');
  if (hospitalSelector) {
    hospitalSelector.addEventListener('change', () => loadDashboardData(true));
  }

  // Handle Session
  if (session) {
    document.getElementById("login-view").style.display = "none";
    document.getElementById("app-shell").style.display = "flex";
    
    document.getElementById("bank-name").textContent = session.bloodBankName;
    document.getElementById("bank-location").textContent = `${session.city}, ${session.state}`;
    
    window.dispatchEvent(new Event('sessionLoaded'));
    navigateTo('home');
  } else {
    document.getElementById("login-view").style.display = "flex";
    document.getElementById("app-shell").style.display = "none";
  }
}

// Router
export function navigateTo(viewId) {
  currentView = viewId;
  
  // Hide all sections
  document.querySelectorAll('.view-section').forEach(el => {
    el.classList.remove('active');
  });
  
  // Show target section
  const targetEl = document.getElementById(`view-${viewId}`);
  if (targetEl) {
    targetEl.classList.add('active');
  }

  // Update URL hash for better UX (optional but nice)
  window.location.hash = viewId;

  // Render content
  loadDashboardData();
}

export async function loadDashboardData(manualRefresh = false) {
  const session = getSession();
  if (!session) return;

  // Initial load of hospitals if selector is empty
  const hospitalSelector = document.getElementById('hospital-selector');
  if (hospitalSelector && hospitalSelector.options.length === 0) {
    try {
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
    } catch (e) {
      console.error("Failed to load hospitals", e);
    }
  }

  if (hospitalSelector && hospitalSelector.value) {
    session.creatorHospitalId = hospitalSelector.value;
    saveSession(session);
  }

  // Fetch all necessary data to pass to views
  try {
    let dashboard = null, inventory = null, requests = null, incomingRequests = null;

    // We can optimize by only fetching what the current view needs, 
    // but for simplicity and immediate cross-tab consistency, we fetch them here concurrently.
    const promises = [];
    
    promises.push(api(`/dashboard?bloodBankId=${session.bloodBankId}&creatorHospitalId=${session.creatorHospitalId}`, { headers: authHeaders() }).then(res => dashboard = res));
    
    promises.push(api(`/inventory/${session.bloodBankId}`, { headers: authHeaders() }).then(res => inventory = res));
    
    promises.push(api(`/requests?creatorHospitalId=${session.creatorHospitalId}`, { headers: authHeaders() }).then(res => requests = res));
    
    promises.push(api(`/requests/bank/${session.bloodBankId}`, { headers: authHeaders() }).then(res => incomingRequests = res));

    await Promise.allSettled(promises);

    // Route rendering
    if (currentView === 'home') {
      renderHomeView(document.getElementById('view-home'), dashboard, incomingRequests);
    } else if (currentView === 'requests') {
      renderBloodRequestsView(document.getElementById('view-requests'), requests);
    } else if (currentView === 'inventory') {
      renderInventoryView(document.getElementById('view-inventory'), inventory);
    } else if (currentView === 'fulfilled') {
      renderFulfilledRequestsView(document.getElementById('view-fulfilled'), requests, incomingRequests);
    }

    if (manualRefresh) {
      showToast("Data refreshed");
    }

    // Auto-refresh timer
    clearInterval(refreshTimer);
    refreshTimer = setInterval(() => loadDashboardData(false), REFRESH_MS);

  } catch (error) {
    console.error("Dashboard load failed:", error);
    if (manualRefresh) {
      showToast(`Error loading data: ${error.message}`, "error");
    }
  }
}
