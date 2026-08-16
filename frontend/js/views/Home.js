import { api, authHeaders, formatDate, getSession } from '../api.js';
import { showToast } from '../components/Toast.js';
import { loadDashboardData } from '../app.js';

export function renderHomeView(container, dashboardData, incomingRequests) {
  container.innerHTML = `
    <div class="section-head mb-4">
      <h2>Dashboard Overview</h2>
    </div>
    
    <div class="stats-grid">
      <div class="stat-card">
        <p>Total Inventory Units</p>
        <h3>${dashboardData ? dashboardData.totalUnits : 0}</h3>
      </div>
      <div class="stat-card">
        <p>Active Requests</p>
        <h3>${dashboardData ? dashboardData.activeRequests : 0}</h3>
      </div>
      <div class="stat-card">
        <p>Fulfilled Requests</p>
        <h3>${dashboardData ? dashboardData.fulfilledRequests : 0}</h3>
      </div>
    </div>

    <div class="card mt-4">
      <div class="card-header">
        <h3>Recent Activity (Incoming Requests)</h3>
      </div>
      <div class="card-body" style="padding: 0;">
        <div class="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Emergency ID</th>
                <th>Group</th>
                <th>Component</th>
                <th>Units</th>
                <th>Urgency</th>
                <th>City</th>
                <th>Status</th>
                <th>Time</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody id="home-incoming-body">
              ${incomingRequests && incomingRequests.length > 0 ? '' : '<tr><td colspan="9" class="empty-state">No incoming requests at this time.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  if (incomingRequests && incomingRequests.length > 0) {
    const tbody = container.querySelector('#home-incoming-body');
    
    incomingRequests.forEach(req => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td><span title="${req.emergencyId}">${req.emergencyId.substring(0, 8)}...</span></td>
        <td><span class="badge badge-danger">${req.bloodGroupRequired}</span></td>
        <td>${req.componentRequired}</td>
        <td><strong>${req.unitsRequired}</strong></td>
        <td><span class="badge ${req.urgencyLevel === 'CRITICAL' ? 'badge-danger' : 'badge-warning'}">${req.urgencyLevel}</span></td>
        <td>${req.hospitalCity}</td>
        <td><span class="badge badge-neutral">${req.status}</span></td>
        <td>${formatDate(req.createdAt)}</td>
        <td>
          <div class="action-group">
            <button class="btn btn-primary fulfill-btn" data-id="${req.emergencyId}" data-units="${req.unitsRequired}">Fulfill</button>
            <button class="btn btn-ghost dismiss-btn" data-id="${req.emergencyId}">Dismiss</button>
          </div>
        </td>
      `;

      row.querySelector(".fulfill-btn").addEventListener("click", async (event) => {
        const button = event.currentTarget;
        const requestedUnits = Number(button.dataset.units);
        const value = prompt(`Enter units to fulfill (max ${requestedUnits}):`, String(requestedUnits));
        if (!value) return;

        const units = Number(value);
        if (Number.isNaN(units) || units <= 0 || units > requestedUnits) {
          showToast("Invalid units amount.", "error");
          return;
        }

        const originalText = button.textContent;
        button.textContent = "...";
        button.disabled = true;

        try {
          await api(`/requests/${button.dataset.id}/fulfill`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({ unitsFulfilled: units })
          });
          showToast("Request fulfilled successfully!");
          loadDashboardData();
        } catch (error) {
          showToast(`Failed to fulfill: ${error.message}`, "error");
          button.textContent = originalText;
          button.disabled = false;
        }
      });

      row.querySelector(".dismiss-btn").addEventListener("click", async (event) => {
        const button = event.currentTarget;
        
        if (!confirm("Are you sure you want to dismiss this request?")) return;
        
        const originalText = button.textContent;
        button.textContent = "...";
        button.disabled = true;
        
        try {
          await api(`/requests/${button.dataset.id}/dismiss`, {
            method: "POST",
            headers: authHeaders()
          });
          showToast("Request dismissed");
          loadDashboardData();
        } catch (error) {
          showToast(`Failed to dismiss: ${error.message}`, "error");
          button.textContent = originalText;
          button.disabled = false;
        }
      });

      tbody.appendChild(row);
    });
  }
}
