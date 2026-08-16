import { api, authHeaders, formatDate, getSession } from '../api.js';
import { showToast } from '../components/Toast.js';
import { loadDashboardData } from '../app.js';

export function renderBloodRequestsView(container, requests) {
  container.innerHTML = `
    <div class="section-head mb-4">
      <h2>Blood Requests</h2>
    </div>

    <div class="card mb-4">
      <div class="card-header">
        <h3>Create Emergency Request</h3>
      </div>
      <div class="card-body">
        <form id="create-request-form" class="form-grid">
          <div class="form-group">
            <label>Blood Group</label>
            <select id="req-blood-group" required>
              <option value="A+">A+</option><option value="A-">A-</option>
              <option value="B+">B+</option><option value="B-">B-</option>
              <option value="O+">O+</option><option value="O-">O-</option>
              <option value="AB+">AB+</option><option value="AB-">AB-</option>
            </select>
          </div>
          <div class="form-group">
            <label>Component</label>
            <select id="req-component" required>
              <option value="Whole Blood">Whole Blood</option>
              <option value="Packed Red Cells">Packed Red Cells</option>
              <option value="Platelets">Platelets</option>
              <option value="Fresh Frozen Plasma">Fresh Frozen Plasma</option>
              <option value="Cryoprecipitate">Cryoprecipitate</option>
            </select>
          </div>
          <div class="form-group">
            <label>Units Required</label>
            <input id="req-units" type="number" min="1" value="1" required />
          </div>
          <div class="form-group">
            <label>Urgency</label>
            <select id="req-urgency">
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="form-group" style="justify-content: flex-end;">
            <button type="submit" class="btn btn-primary" id="req-submit-btn">Create Request</button>
          </div>
        </form>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>My Created Requests</h3>
      </div>
      <div class="card-body" style="padding: 0;">
        <div class="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Emergency ID</th>
                <th>Group</th>
                <th>Units</th>
                <th>Status</th>
                <th>Created At</th>
              </tr>
            </thead>
            <tbody id="my-requests-body">
              ${requests && requests.length > 0 ? '' : '<tr><td colspan="5" class="empty-state">No requests found.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  // Bind Form Submit
  const form = container.querySelector('#create-request-form');
  const submitBtn = container.querySelector('#req-submit-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const session = getSession();
    // Default to the first hospital if none is explicitly selected in session
    // (We will ensure session.creatorHospitalId is set globally by app.js)
    const hospitalId = session.creatorHospitalId || document.getElementById('hospital-selector').value;

    const payload = {
      creatorHospitalId: hospitalId,
      bloodGroupRequired: document.getElementById("req-blood-group").value,
      componentRequired: document.getElementById("req-component").value,
      unitsRequired: Number(document.getElementById("req-units").value),
      urgencyLevel: document.getElementById("req-urgency").value
    };

    const originalText = submitBtn.textContent;
    submitBtn.textContent = "Creating...";
    submitBtn.disabled = true;

    try {
      const created = await api("/requests", {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(payload)
      });
      showToast(`Request created. Donors notified: ${created.donorsNotified}`);
      form.reset();
      loadDashboardData();
    } catch (error) {
      showToast(`Failed to create request: ${error.message}`, "error");
    } finally {
      submitBtn.textContent = originalText;
      submitBtn.disabled = false;
    }
  });

  // Render Table Rows
  if (requests && requests.length > 0) {
    const tbody = container.querySelector('#my-requests-body');
    requests.forEach(req => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td><span title="${req.emergencyId}">${req.emergencyId.substring(0, 8)}...</span></td>
        <td><span class="badge badge-danger">${req.bloodGroupRequired}</span></td>
        <td>${req.unitsRequired}</td>
        <td><span class="badge badge-neutral">${req.status}</span></td>
        <td>${formatDate(req.createdAt)}</td>
      `;
      tbody.appendChild(row);
    });
  }
}
