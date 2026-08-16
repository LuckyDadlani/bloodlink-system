import { api, authHeaders, formatDate, getSession } from '../api.js';
import { showToast } from '../components/Toast.js';
import { loadDashboardData } from '../app.js';

export function renderInventoryView(container, inventory) {
  container.innerHTML = `
    <div class="section-head mb-4">
      <h2>Inventory Management</h2>
    </div>

    <div class="card mb-4">
      <div class="card-header">
        <h3>Add Inventory Stock</h3>
      </div>
      <div class="card-body">
        <form id="add-inventory-form" class="form-grid">
          <div class="form-group">
            <label>Blood Group</label>
            <select id="inv-blood-group" required>
              <option value="A+">A+</option><option value="A-">A-</option>
              <option value="B+">B+</option><option value="B-">B-</option>
              <option value="O+">O+</option><option value="O-">O-</option>
              <option value="AB+">AB+</option><option value="AB-">AB-</option>
            </select>
          </div>
          <div class="form-group">
            <label>Component</label>
            <select id="inv-component" required>
              <option value="Whole Blood">Whole Blood</option>
              <option value="Packed Red Cells">Packed Red Cells</option>
              <option value="Platelets">Platelets</option>
              <option value="Fresh Frozen Plasma">Fresh Frozen Plasma</option>
              <option value="Cryoprecipitate">Cryoprecipitate</option>
            </select>
          </div>
          <div class="form-group">
            <label>Initial Units</label>
            <input id="inv-units" type="number" min="0" value="0" required />
          </div>
          <div class="form-group" style="justify-content: flex-end;">
            <button type="submit" class="btn btn-primary" id="inv-submit-btn">Add Row</button>
          </div>
        </form>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>Current Stock</h3>
      </div>
      <div class="card-body" style="padding: 0;">
        <div class="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Blood Group</th>
                <th>Component</th>
                <th>Units</th>
                <th>Last Updated</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody id="inventory-body">
              ${inventory && inventory.length > 0 ? '' : '<tr><td colspan="5" class="empty-state">No inventory items found. Add some stock above.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  // Bind Form Submit
  const form = container.querySelector('#add-inventory-form');
  const submitBtn = container.querySelector('#inv-submit-btn');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const session = getSession();
    
    const payload = {
      bloodBankId: session.bloodBankId,
      bloodGroup: document.getElementById("inv-blood-group").value,
      componentType: document.getElementById("inv-component").value,
      unitsAvailable: Number(document.getElementById("inv-units").value),
      createdBy: session.userId
    };

    const originalText = submitBtn.textContent;
    submitBtn.textContent = "Adding...";
    submitBtn.disabled = true;

    try {
      await api("/inventory", {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(payload)
      });
      showToast("Inventory row added successfully!");
      document.getElementById("inv-units").value = "0";
      loadDashboardData();
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      submitBtn.textContent = originalText;
      submitBtn.disabled = false;
    }
  });

  // Render Table Rows
  if (inventory && inventory.length > 0) {
    const tbody = container.querySelector('#inventory-body');
    inventory.forEach(item => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td><span class="badge badge-danger">${item.bloodGroup}</span></td>
        <td>${item.componentType}</td>
        <td><strong>${item.unitsAvailable}</strong></td>
        <td>${formatDate(item.lastUpdatedAt)}</td>
        <td>
          <div class="action-group">
            <button class="btn btn-secondary edit-btn" data-id="${item.inventoryId}" data-units="${item.unitsAvailable}">Edit</button>
            <button class="btn btn-danger delete-btn" data-id="${item.inventoryId}">Delete</button>
          </div>
        </td>
      `;

      row.querySelector('.edit-btn').addEventListener('click', async (event) => {
        const button = event.currentTarget;
        const currentUnits = Number(button.dataset.units);
        const value = prompt("Enter new units available:", String(currentUnits));
        if (value === null) return;
  
        const units = Number(value);
        if (Number.isNaN(units) || units < 0) {
          showToast("Units must be a non-negative number.", "error");
          return;
        }

        try {
          await api(`/inventory/${button.dataset.id}`, {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify({
              updatedBy: getSession().userId,
              unitsAvailable: units
            })
          });
          showToast("Inventory updated!");
          loadDashboardData();
        } catch (error) {
          showToast(`Failed to update: ${error.message}`, "error");
        }
      });

      row.querySelector('.delete-btn').addEventListener('click', async (event) => {
        const button = event.currentTarget;
        if (!confirm("Are you sure you want to delete this inventory row?")) return;
        
        try {
          await api(`/inventory/${button.dataset.id}`, {
            method: "DELETE",
            headers: authHeaders()
          });
          showToast("Inventory row deleted!");
          loadDashboardData();
        } catch (error) {
          showToast(`Failed to delete: ${error.message}`, "error");
        }
      });

      tbody.appendChild(row);
    });
  }
}
