import { api, saveSession } from '../api.js';
import { showToast } from '../components/Toast.js';
import { navigateTo } from '../app.js';

export function setupLogin() {
  const loginForm = document.getElementById("login-form");
  const loginBtn = document.getElementById("login-btn");

  if (!loginForm) return;

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    
    const originalText = loginBtn.textContent;
    loginBtn.textContent = "Authenticating...";
    loginBtn.disabled = true;

    try {
      const result = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
      });

      const session = {
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
      showToast("Login successful!", "success");
      
      // Complete login
      document.getElementById("login-view").style.display = "none";
      document.getElementById("app-shell").style.display = "flex";
      
      // Update header
      document.getElementById("bank-name").textContent = session.bloodBankName;
      document.getElementById("bank-location").textContent = `${session.city}, ${session.state}`;
      
      // Dispatch event to load dashboard data
      window.dispatchEvent(new Event('sessionLoaded'));
      navigateTo('home');

    } catch (error) {
      showToast(error.message || "Login failed", "error");
    } finally {
      loginBtn.textContent = originalText;
      loginBtn.disabled = false;
    }
  });
}
