import { formatDate } from '../api.js';

export function renderFulfilledRequestsView(container, createdRequests, incomingRequests) {
  // We identify fulfilled requests from two sources:
  // 1. createdRequests (requests this hospital/bank created)
  // 2. incomingRequests (requests sent to this bank)
  // Combine them, filter by status, and remove duplicates.
  
  const allRequests = [...(createdRequests || []), ...(incomingRequests || [])];
  
  const fulfilledStatuses = ["FULFILLED_BY_BANK", "DONOR_CONFIRMED", "PARTIALLY_FULFILLED", "CLOSED"];
  
  // Filter and deduplicate
  const map = new Map();
  allRequests.forEach(req => {
    if (fulfilledStatuses.includes(req.status)) {
      map.set(req.emergencyId, req);
    }
  });
  
  const fulfilledList = Array.from(map.values()).sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt);
  });

  container.innerHTML = `
    <div class="section-head mb-4">
      <h2>Fulfilled Requests History</h2>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>Completed Transactions</h3>
        <input type="text" id="fulfilled-search" placeholder="Search by ID or Group..." class="search-input" style="max-width: 250px;" />
      </div>
      <div class="card-body" style="padding: 0;">
        <div class="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Emergency ID</th>
                <th>Group</th>
                <th>Component</th>
                <th>Requested</th>
                <th>Fulfilled</th>
                <th>City</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody id="fulfilled-body">
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  const tbody = container.querySelector('#fulfilled-body');
  
  function renderTable(items) {
    tbody.innerHTML = items.length > 0 ? '' : '<tr><td colspan="8" class="empty-state">No fulfilled requests found.</td></tr>';
    
    items.forEach(req => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td><span title="${req.emergencyId}">${req.emergencyId.substring(0, 8)}...</span></td>
        <td><span class="badge badge-danger">${req.bloodGroupRequired}</span></td>
        <td>${req.componentRequired}</td>
        <td>${req.unitsRequired}</td>
        <td><strong>${req.unitsFulfilled !== undefined ? req.unitsFulfilled : '-'}</strong></td>
        <td>${req.hospitalCity}</td>
        <td><span class="badge badge-success">${req.status}</span></td>
        <td>${formatDate(req.createdAt)}</td>
      `;
      tbody.appendChild(row);
    });
  }

  renderTable(fulfilledList);

  // Search logic
  const searchInput = container.querySelector('#fulfilled-search');
  searchInput.addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    const filtered = fulfilledList.filter(req => 
      req.emergencyId.toLowerCase().includes(term) ||
      req.bloodGroupRequired.toLowerCase().includes(term) ||
      req.componentRequired.toLowerCase().includes(term)
    );
    renderTable(filtered);
  });
}
