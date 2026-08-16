import { navigateTo } from '../app.js';
import { clearSession } from '../api.js';

export function setupSidebar() {
  const navItems = document.querySelectorAll('.nav-item');
  const logoutBtn = document.getElementById('logout-btn');
  const mobileToggle = document.getElementById('mobile-toggle');
  const sidebar = document.getElementById('sidebar');

  // Handle navigation clicks
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      
      // Remove active class from all
      navItems.forEach(nav => nav.classList.remove('active'));
      
      // Add active class to clicked
      e.currentTarget.classList.add('active');
      
      // Navigate to view
      const viewId = e.currentTarget.getAttribute('data-view');
      navigateTo(viewId);

      // Close sidebar on mobile
      if (window.innerWidth <= 980) {
        sidebar.classList.remove('open');
      }
    });
  });

  // Handle Logout
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      clearSession();
      window.location.reload();
    });
  }

  // Mobile Toggle
  if (mobileToggle) {
    mobileToggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });
  }

  // Close sidebar when clicking outside on mobile
  document.addEventListener('click', (e) => {
    if (window.innerWidth <= 980) {
      if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target) && sidebar.classList.contains('open')) {
        sidebar.classList.remove('open');
      }
    }
  });
}
