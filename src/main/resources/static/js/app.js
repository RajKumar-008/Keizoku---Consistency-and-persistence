/**
 * KEIZOKU - CONSISTENCY AND PERSISTENCE
 * Client-Side Controller & State Engine
 */

const App = (() => {
  // Application State
  const state = {
    token: localStorage.getItem('keizoku_token') || localStorage.getItem('dayforge_token') || '',
    user: null,
    currentDate: null, // YYYY-MM-DD string
    todayDate: null,
    todayChecklist: null,
    activeCategoryFilter: 'ALL',
    habits: [],
    goals: [],
    streak: null,
    achievements: [],
    history: [],
    calendarMonth: new Date().getMonth() + 1,
    calendarYear: new Date().getFullYear(),
    charts: {
      trend: null,
      category: null
    }
  };

  // Dynamic API Base URL (handles both http://localhost:8080 and direct file:/// opening)
  const API_BASE = (window.location.protocol === 'file:' || !window.location.port) 
    ? 'http://localhost:8080' 
    : '';

  // API helper with JWT token header
  async function api(endpoint, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    if (state.token) {
      headers['Authorization'] = `Bearer ${state.token}`;
    }

    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;

    try {
      const res = await fetch(url, { ...options, headers });
      const contentType = res.headers.get('content-type') || '';
      
      let data = null;
      if (contentType.includes('application/json')) {
        data = await res.json();
      } else {
        data = await res.text();
      }

      if (!res.ok) {
        if (res.status === 401) {
          state.token = '';
          state.user = null;
          localStorage.removeItem('keizoku_token');
          localStorage.removeItem('dayforge_token');
          showModal('modalAuth');
          const errorMsg = (data && data.message) ? data.message : 'Please sign in to continue.';
          throw new Error(errorMsg);
        }
        if (res.status === 403) {
          const isDateSecurity = data && data.message && (data.message.includes('protection') || data.message.includes('Date') || data.message.includes('archived'));
          if (!isDateSecurity) {
            state.token = '';
            state.user = null;
            localStorage.removeItem('keizoku_token');
            localStorage.removeItem('dayforge_token');
            showModal('modalAuth');
            const errorMsg = (data && data.message) ? data.message : 'Session invalid. Please sign in.';
            throw new Error(errorMsg);
          }
        }
        const errorMsg = data && data.message ? data.message : `Request failed with status ${res.status}`;
        throw new Error(errorMsg);
      }
      return data;
    } catch (err) {
      console.error(`API Error on [${url}]:`, err);
      if (err.name === 'TypeError' && err.message.toLowerCase().includes('fetch')) {
        showToast('Cannot connect to KEIZOKU server. Please ensure the backend server is running on http://localhost:8080', 'error');
      } else if (!url.includes('/api/auth/me')) {
        showToast(err.message, 'error');
      }
      throw err;
    }
  }

  // Toast Notifications
  function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast-forge ${type}`;
    
    let icon = '🌱';
    if (type === 'error') icon = '⚠️';
    if (type === 'warning') icon = '⏳';

    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  // Navigation Controller
  function navigateTo(sectionId) {
    document.querySelectorAll('.section-view').forEach(sec => sec.classList.remove('active'));
    document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
    document.querySelectorAll('.mobile-nav-btn').forEach(btn => btn.classList.remove('active'));

    const targetView = document.getElementById(`view-${sectionId}`);
    if (targetView) {
      targetView.classList.add('active');
    }

    // Update sidebar nav
    const activeLink = document.querySelector(`.nav-link[data-section="${sectionId}"]`);
    if (activeLink) activeLink.classList.add('active');

    // Update mobile nav
    const activeMobileBtn = document.querySelector(`.mobile-nav-btn[data-section="${sectionId}"]`);
    if (activeMobileBtn) activeMobileBtn.classList.add('active');

    // Close mobile drawer if open
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.remove('open');

    // Trigger section-specific refreshes
    if (sectionId === 'calendar') renderCalendar();
    if (sectionId === 'reports') renderReports();
    if (sectionId === 'history') loadHistory();
    if (sectionId === 'streaks') loadStreaks();
    if (sectionId === 'habits') loadHabits();
    if (sectionId === 'notifications') loadNotifications();
  }

  // Modal Controls
  function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('active');
      if (modalId === 'modalAddHabit') {
        const rows = document.querySelectorAll('.habit-activity-row');
        if (rows.length === 0) resetHabitRows();
        const firstInp = document.querySelector('.habit-row-title');
        if (firstInp) firstInp.focus();
      }
    }
  }

  function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
  }

  // Formatting helpers
  function formatDateReadable(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    return d.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  function formatShortDate(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  // =========================================================================
  // AUTHENTICATION & INITIALIZATION
  // =========================================================================
  async function syncClientTimezone() {
    if (!state.token || !state.user) return;
    try {
      const clientTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata';
      if (state.user.timezone !== clientTz) {
        const res = await api('/api/user/settings', {
          method: 'PUT',
          body: JSON.stringify({ timezone: clientTz })
        });
        if (res && res.timezone) {
          state.user.timezone = res.timezone;
        }
      }
    } catch (e) {
      console.warn('Failed to sync client timezone:', e);
    }
  }

  async function initAuth() {
    if (!state.token) {
      showModal('modalAuth');
      return;
    }

    try {
      const user = await api('/api/auth/me');
      state.user = user;
      await syncClientTimezone();
      updateUserUI();
    } catch (e) {
      console.warn('Session expired or user not found:', e);
      state.token = '';
      state.user = null;
      localStorage.removeItem('keizoku_token');
      localStorage.removeItem('dayforge_token');
      showModal('modalAuth');
    }
  }

  function updateUserUI() {
    if (!state.user) return;
    const name = state.user.fullName || state.user.username;
    const initial = name.charAt(0).toUpperCase();

    const avatarInitial = document.getElementById('avatarInitial');
    if (avatarInitial) avatarInitial.textContent = initial;

    const userNameLabel = document.getElementById('userNameLabel');
    if (userNameLabel) userNameLabel.textContent = name;

    const profileAvatarLarge = document.getElementById('profileAvatarLarge');
    if (profileAvatarLarge) profileAvatarLarge.textContent = initial;

    const profileFullNameLabel = document.getElementById('profileFullNameLabel');
    if (profileFullNameLabel) profileFullNameLabel.textContent = name;

    const profileEmailLabel = document.getElementById('profileEmailLabel');
    if (profileEmailLabel) profileEmailLabel.textContent = state.user.email || 'user@keizoku.local';

    const detectedTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    const profileTimezoneLabel = document.getElementById('profileTimezoneLabel');
    if (profileTimezoneLabel) profileTimezoneLabel.textContent = state.user.timezone || detectedTz;

    const settingTimezoneSelect = document.getElementById('settingTimezoneSelect');
    if (settingTimezoneSelect && state.user.timezone) {
      let optFound = false;
      for (let i = 0; i < settingTimezoneSelect.options.length; i++) {
        if (settingTimezoneSelect.options[i].value === state.user.timezone) {
          optFound = true;
          break;
        }
      }
      if (!optFound) {
        const newOpt = document.createElement('option');
        newOpt.value = state.user.timezone;
        newOpt.textContent = `${state.user.timezone} (Local System)`;
        settingTimezoneSelect.appendChild(newOpt);
      }
      settingTimezoneSelect.value = state.user.timezone;
    }
  }

  // =========================================================================
  // TODAY'S CHECKLIST SYSTEM
  // =========================================================================
  async function loadTodayChecklist() {
    if (!state.token) return;
    try {
      const data = await api('/api/checklist/today');
      state.todayChecklist = data;
      state.currentDate = data.date;
      state.todayDate = data.date;

      // Update Topbar and Date Badges
      const topbarDateText = document.getElementById('topbarDateText');
      if (topbarDateText) {
        topbarDateText.textContent = formatDateReadable(data.date);
        topbarDateText.title = data.date;
      }

      const todayChecklistDateLabel = document.getElementById('todayChecklistDateLabel');
      if (todayChecklistDateLabel) {
        todayChecklistDateLabel.textContent = formatDateReadable(data.date);
        todayChecklistDateLabel.title = data.date;
      }

      renderTodayChecklist(data);
      renderDashboardPreview(data);
      loadStreaks();
      loadGoals();
    } catch (err) {
      console.error('Failed to load today checklist:', err);
    }
  }

  function renderTodayChecklist(data) {
    const container = document.getElementById('todayFullChecklistContainer');
    if (!container) return;

    // Update Progress Stats
    const percent = Math.round(data.completionPercentage || 0);
    const percentLabel = document.getElementById('todayCompletionPercentLabel');
    if (percentLabel) percentLabel.textContent = `${percent}%`;

    const barFill = document.getElementById('todayProgressBarFill');
    if (barFill) {
      barFill.style.width = `${percent}%`;
      if (percent === 100) barFill.classList.add('complete');
      else barFill.classList.remove('complete');
    }

    const totalEl = document.getElementById('todayTotalTasksCount');
    if (totalEl) totalEl.textContent = data.totalTasks || 0;

    const doneEl = document.getElementById('todayCompletedTasksCount');
    if (doneEl) doneEl.textContent = data.completedTasks || 0;

    const remEl = document.getElementById('todayRemainingTasksCount');
    if (remEl) remEl.textContent = Math.max(0, (data.totalTasks || 0) - (data.completedTasks || 0));

    // Filter items
    let items = data.items || [];
    if (state.activeCategoryFilter !== 'ALL') {
      items = items.filter(it => it.category === state.activeCategoryFilter);
    }

    if (items.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding:36px 16px; color:var(--text-muted); background:var(--cream-light); border-radius:var(--radius-md); border:1px dashed var(--cream-dark);">
          <div style="font-size:1.8rem; margin-bottom:8px;">🌿</div>
          <div style="font-weight:600; color:var(--forest-green); margin-bottom:4px;">No checklist activities found for today</div>
          <p style="font-size:0.86rem; margin:0 0 14px 0;">Configure your daily habits under My Habits to automatically generate your daily checklist.</p>
          <button class="btn-forge btn-forge-primary btn-forge-sm" onclick="App.navigateTo('habits')">Manage Habits</button>
        </div>
      `;
      return;
    }

    container.innerHTML = items.map((item, index) => {
      const isCompleted = item.completed;

      return `
        <div class="checklist-item ${isCompleted ? 'completed' : ''}" data-item-id="${item.id}">
          <div class="item-left">
            <button 
              type="button" 
              class="custom-checkbox-nature ${isCompleted ? 'checked' : ''}"
              onclick="App.toggleItemStatus(${item.id})"
              title="${isCompleted ? 'Click to uncheck' : 'Click to complete'}"
              aria-label="Toggle ${item.title}"
            >
              ${isCompleted ? '✓' : ''}
            </button>

            <div class="item-details">
              <span class="item-title">${escapeHtml(item.title)}</span>
              <div class="item-meta">
                <span class="item-category-tag">${item.category || 'GENERAL'}</span>
                ${item.completedAt ? `<span>• Completed ${formatTime(item.completedAt)}</span>` : ''}
              </div>
            </div>
          </div>

          <div class="item-actions">
            <!-- Reorder arrows -->
            <button class="btn-item-action" onclick="App.reorderItem(${item.id}, -1)" title="Move Up" ${index === 0 ? 'disabled style="opacity:0.3;"' : ''}>↑</button>
            <button class="btn-item-action" onclick="App.reorderItem(${item.id}, 1)" title="Move Down" ${index === items.length - 1 ? 'disabled style="opacity:0.3;"' : ''}>↓</button>
            
            <!-- Edit -->
            <button class="btn-item-action" onclick="App.openEditItemModal(${item.id}, '${escapeAttr(item.title)}', '${item.category}')" title="Edit Name">✎</button>

            <!-- Delete with Confirmation -->
            <button class="btn-item-action delete" onclick="App.openDeleteConfirmModal(${item.id}, '${escapeAttr(item.title)}')" title="Delete Activity">✕</button>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderDashboardPreview(data) {
    // Score
    const score = Math.round(data.completionPercentage || 0);
    const scoreEl = document.getElementById('dashTodayScore');
    if (scoreEl) scoreEl.textContent = `${score}%`;

    const badgeEl = document.getElementById('dashCompletedCountBadge');
    if (badgeEl) badgeEl.textContent = `${data.completedTasks || 0} / ${data.totalTasks || 0} Done`;

    // Preview list
    const container = document.getElementById('dashTodayItemList');
    if (!container) return;

    const items = data.items || [];
    if (items.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding:18px; color:var(--text-muted); font-size:0.86rem;">
          No items yet for today. Use the box above to add activities.
        </div>
      `;
      return;
    }

    container.innerHTML = items.slice(0, 5).map(item => `
      <div class="checklist-item ${item.completed ? 'completed' : ''}" style="padding:8px 12px; margin-bottom:6px;">
        <div class="item-left">
          <button 
            type="button" 
            class="custom-checkbox-nature ${item.completed ? 'checked' : ''}"
            style="width:20px; height:20px; font-size:0.8rem;"
            onclick="App.toggleItemStatus(${item.id})"
          >
            ${item.completed ? '✓' : ''}
          </button>
          <div class="item-details">
            <span class="item-title" style="font-size:0.88rem;">${escapeHtml(item.title)}</span>
            <div class="item-meta">
              <span class="item-category-tag" style="font-size:0.68rem; padding:1px 6px;">${item.category || 'GENERAL'}</span>
            </div>
          </div>
        </div>
      </div>
    `).join('');
  }

  function filterTodayCategory(cat) {
    state.activeCategoryFilter = cat;
    document.querySelectorAll('[data-filter-cat]').forEach(btn => {
      if (btn.getAttribute('data-filter-cat') === cat) btn.classList.add('active');
      else btn.classList.remove('active');
    });
    if (state.todayChecklist) {
      renderTodayChecklist(state.todayChecklist);
    }
  }

  // Check/Uncheck toggle
  async function toggleItemStatus(itemId) {
    try {
      const updated = await api(`/api/checklist/item/${itemId}/toggle`, { method: 'PATCH' });
      showToast(updated.completed ? `✓ Marked "${updated.title}" as completed` : `Marked "${updated.title}" as incomplete`);
      await loadTodayChecklist();
    } catch (err) {
      // Error message displayed via toast in api()
    }
  }

  // Batch Add Activities (comma separated)
  async function submitBatchAdd(activitiesText, defaultCat) {
    if (!activitiesText || !activitiesText.trim()) {
      showToast('Please enter at least one activity', 'warning');
      return;
    }

    try {
      const res = await api('/api/checklist/batch', {
        method: 'POST',
        body: JSON.stringify({
          activities: activitiesText,
          defaultCategory: defaultCat || 'GENERAL',
          date: state.currentDate
        })
      });

      showToast(`Added ${res.items.length - (state.todayChecklist ? state.todayChecklist.items.length : 0)} new activities to checklist`);
      hideModal('modalBatchAdd');
      document.getElementById('batchInputTextarea').value = '';
      const dashInput = document.getElementById('dashQuickBatchInput');
      if (dashInput) dashInput.value = '';
      await loadTodayChecklist();
    } catch (err) {}
  }

  // Single Item Add
  async function submitSingleAdd(title, category) {
    if (!title || !title.trim()) {
      showToast('Activity title cannot be empty', 'warning');
      return;
    }

    try {
      await api('/api/checklist/item', {
        method: 'POST',
        body: JSON.stringify({
          title: title.trim(),
          category: category || 'GENERAL',
          date: state.currentDate
        })
      });
      showToast(`Added "${title.trim()}" to checklist`);
      hideModal('modalSingleAdd');
      document.getElementById('singleItemTitle').value = '';
      await loadTodayChecklist();
    } catch (err) {}
  }

  // Reorder Item
  async function reorderItem(itemId, direction) {
    try {
      await api(`/api/checklist/item/${itemId}/reorder`, {
        method: 'POST',
        body: JSON.stringify({ direction })
      });
      await loadTodayChecklist();
    } catch (err) {}
  }

  // Edit Item Modal Handlers
  function openEditItemModal(id, title, category) {
    document.getElementById('editItemId').value = id;
    document.getElementById('editItemTitle').value = title;
    document.getElementById('editItemCategory').value = category || 'GENERAL';
    showModal('modalEditItem');
  }

  async function submitEditItem() {
    const id = document.getElementById('editItemId').value;
    const title = document.getElementById('editItemTitle').value;
    const category = document.getElementById('editItemCategory').value;

    if (!title || !title.trim()) {
      showToast('Title is required', 'warning');
      return;
    }

    try {
      await api(`/api/checklist/item/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ title: title.trim(), category })
      });
      showToast('Activity updated successfully');
      hideModal('modalEditItem');
      await loadTodayChecklist();
    } catch (err) {}
  }

  // Delete Item Modal Handlers
  function openDeleteConfirmModal(id, title) {
    document.getElementById('deleteTargetItemId').value = id;
    document.getElementById('confirmDeleteMessage').textContent = `Are you sure you want to remove "${title}" from today's checklist?`;
    showModal('modalConfirmDelete');
  }

  async function submitConfirmDelete() {
    const id = document.getElementById('deleteTargetItemId').value;
    try {
      await api(`/api/checklist/item/${id}`, { method: 'DELETE' });
      showToast('Activity removed from checklist');
      hideModal('modalConfirmDelete');
      await loadTodayChecklist();
    } catch (err) {}
  }

  // =========================================================================
  // HABITS MANAGEMENT SYSTEM
  // =========================================================================
  async function loadHabits() {
    if (!state.token) return;
    try {
      const habits = await api('/api/habits');
      state.habits = habits;
      renderHabitsList(habits);
      const hCount = document.getElementById('dashTotalHabitsCount');
      if (hCount) hCount.textContent = habits.filter(h => h.active).length;
    } catch (err) {
      console.error('Failed to load habits:', err);
    }
  }

  function renderHabitsList(habits) {
    const container = document.getElementById('habitsListContainer');
    if (!container) return;

    if (habits.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding:36px; color:var(--text-muted); background:var(--cream-light); border-radius:var(--radius-md);">
          <div style="font-size:1.8rem; margin-bottom:8px;">🌱</div>
          <div style="font-weight:600; color:var(--forest-green);">No recurring habits defined yet</div>
          <p style="font-size:0.86rem; margin-top:4px;">Define habits with recurring frequencies (Daily, Weekdays, Custom) to auto-populate your daily checklist.</p>
          <button class="btn-forge btn-forge-primary btn-forge-sm" onclick="App.showModal('modalAddHabit')">+ Create Habit</button>
        </div>
      `;
      return;
    }

    container.innerHTML = habits.map(h => {
      let freqLabel = 'Every Day';
      if (h.frequencyType === 'WEEKDAYS') freqLabel = 'Weekdays (Mon-Fri)';
      if (h.frequencyType === 'CUSTOM_DAYS') freqLabel = `Custom Days (${h.customDays || ''})`;

      return `
        <div class="card-forge" style="padding:16px 20px; margin-bottom:12px; display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px;">
          <div>
            <div style="display:flex; align-items:center; gap:8px;">
              <h4 style="margin:0; font-size:1.05rem; color:var(--forest-green);">${escapeHtml(h.title)}</h4>
              <span class="item-category-tag">${h.category || 'GENERAL'}</span>
              <span class="today-badge-pill" style="font-size:0.75rem; padding:2px 8px;">${freqLabel}</span>
            </div>
            <div style="font-size:0.78rem; color:var(--text-muted); margin-top:4px;">
              Active from: ${formatShortDate(h.startDate)} ${h.endDate ? `to ${formatShortDate(h.endDate)}` : '(Ongoing)'}
              • Status: <strong style="color:${h.active ? 'var(--forest-green)' : 'var(--text-muted)'};">${h.active ? 'Active' : 'Paused'}</strong>
            </div>
          </div>

          <div style="display:flex; align-items:center; gap:8px;">
            <button class="btn-forge btn-forge-secondary btn-forge-sm" onclick="App.toggleHabitActive(${h.id})">
              ${h.active ? 'Pause' : 'Activate'}
            </button>
            <button class="btn-forge btn-forge-danger btn-forge-sm" onclick="App.deleteHabit(${h.id}, '${escapeAttr(h.title)}')">
              Delete
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  function addHabitRow(title = '', category = 'CAREER') {
    const container = document.getElementById('habitRowsContainer');
    if (!container) return;
    const row = document.createElement('div');
    row.className = 'habit-activity-row';
    row.style.cssText = 'display:flex; gap:8px; align-items:center;';
    row.innerHTML = `
      <input type="text" class="form-control-nature habit-row-title" placeholder="e.g. Java, Gym, Reading..." value="${escapeAttr(title)}" style="flex:1.4;">
      <select class="form-control-nature habit-row-category" style="flex:1;">
        <option value="CAREER" ${category === 'CAREER' ? 'selected' : ''}>Work &amp; Coding</option>
        <option value="LEARNING" ${category === 'LEARNING' ? 'selected' : ''}>Learning &amp; Study</option>
        <option value="HEALTH" ${category === 'HEALTH' ? 'selected' : ''}>Health &amp; Fitness</option>
        <option value="MINDFULNESS" ${category === 'MINDFULNESS' ? 'selected' : ''}>Mindfulness &amp; Wellness</option>
        <option value="GENERAL" ${category === 'GENERAL' ? 'selected' : ''}>General</option>
      </select>
      <button type="button" class="btn-item-action habit-row-remove" title="Remove" style="color:var(--accent-red); font-size:1.2rem; min-width:28px;">&times;</button>
    `;
    const removeBtn = row.querySelector('.habit-row-remove');
    if (removeBtn) {
      removeBtn.addEventListener('click', () => {
        row.remove();
        updateHabitRowsState();
      });
    }
    const titleInp = row.querySelector('.habit-row-title');
    if (titleInp) {
      titleInp.addEventListener('input', updateHabitRowsState);
    }
    container.appendChild(row);
    updateHabitRowsState();
    if (titleInp) titleInp.focus();
  }

  function updateHabitRowsState() {
    const rows = document.querySelectorAll('.habit-activity-row');
    const btnSubmit = document.getElementById('btnSubmitAddHabit');
    rows.forEach((r) => {
      const removeBtn = r.querySelector('.habit-row-remove');
      if (removeBtn) {
        if (rows.length <= 1) {
          removeBtn.disabled = true;
          removeBtn.style.opacity = '0.3';
          removeBtn.style.cursor = 'default';
        } else {
          removeBtn.disabled = false;
          removeBtn.style.opacity = '1';
          removeBtn.style.cursor = 'pointer';
        }
      }
    });

    if (btnSubmit) {
      let count = 0;
      rows.forEach(r => {
        const inp = r.querySelector('.habit-row-title');
        if (inp && inp.value.trim().length > 0) count++;
      });
      btnSubmit.textContent = count > 1 ? `Save ${count} Habit Schedules` : 'Save Habit Schedule';
    }
  }

  function resetHabitRows() {
    const container = document.getElementById('habitRowsContainer');
    if (!container) return;
    container.innerHTML = `
      <div class="habit-activity-row" style="display:flex; gap:8px; align-items:center;">
        <input type="text" class="form-control-nature habit-row-title" placeholder="e.g. Java" style="flex:1.4;">
        <select class="form-control-nature habit-row-category" style="flex:1;">
          <option value="CAREER">Work &amp; Coding</option>
          <option value="LEARNING">Learning &amp; Study</option>
          <option value="HEALTH">Health &amp; Fitness</option>
          <option value="MINDFULNESS">Mindfulness &amp; Wellness</option>
          <option value="GENERAL">General</option>
        </select>
        <button type="button" class="btn-item-action habit-row-remove" title="Remove" style="opacity:0.3; cursor:default; min-width:28px;" disabled>&times;</button>
      </div>
    `;
    const titleInp = container.querySelector('.habit-row-title');
    if (titleInp) titleInp.addEventListener('input', updateHabitRowsState);
    updateHabitRowsState();
  }

  async function submitAddHabit() {
    const rows = document.querySelectorAll('.habit-activity-row');
    const frequencyType = document.getElementById('habitFrequencySelect').value;
    const startDate = document.getElementById('habitStartDateInput').value;
    const endDate = document.getElementById('habitEndDateInput').value;

    const habitItems = [];
    rows.forEach(r => {
      const titleInp = r.querySelector('.habit-row-title');
      const catSelect = r.querySelector('.habit-row-category');
      const title = titleInp ? titleInp.value.trim() : '';
      const category = catSelect ? catSelect.value : 'CAREER';
      if (title.length > 0) {
        habitItems.push({
          title,
          category,
          frequencyType,
          startDate: startDate || state.todayDate,
          endDate: endDate || null,
          active: true
        });
      }
    });

    if (habitItems.length === 0) {
      showToast('Please enter at least one habit activity', 'warning');
      return;
    }

    let customDays = null;
    if (frequencyType === 'CUSTOM_DAYS') {
      const checkedDays = Array.from(document.querySelectorAll('input[name="customDay"]:checked')).map(cb => cb.value);
      if (checkedDays.length === 0) {
        showToast('Please select at least one day for custom schedule', 'warning');
        return;
      }
      customDays = checkedDays.join(',');
      habitItems.forEach(h => h.customDays = customDays);
    }

    try {
      await api('/api/habits/batch', {
        method: 'POST',
        body: JSON.stringify(habitItems)
      });

      if (habitItems.length === 1) {
        showToast(`Habit "${habitItems[0].title}" created successfully`);
      } else {
        showToast(`Added ${habitItems.length} habits with their assigned categories!`);
      }

      hideModal('modalAddHabit');
      resetHabitRows();
      await loadHabits();
      await loadTodayChecklist();
    } catch (err) {}
  }

  async function toggleHabitActive(id) {
    try {
      const h = await api(`/api/habits/${id}/toggle`, { method: 'PATCH' });
      showToast(`Habit "${h.title}" is now ${h.active ? 'active' : 'paused'}`);
      await loadHabits();
      await loadTodayChecklist();
    } catch (err) {}
  }

  async function deleteHabit(id, title) {
    if (!confirm(`Are you sure you want to delete habit "${title}"?`)) return;
    try {
      await api(`/api/habits/${id}`, { method: 'DELETE' });
      showToast('Habit deleted');
      await loadHabits();
    } catch (err) {}
  }

  // =========================================================================
  // GOALS & TARGET SYSTEM
  // =========================================================================
  async function loadGoals() {
    if (!state.token) return;
    try {
      const goals = await api('/api/goals');
      state.goals = goals;
      renderGoals(goals);

      const countEl = document.getElementById('dashActiveGoalsCount');
      if (countEl) countEl.textContent = goals.length;
    } catch (err) {
      console.error('Failed to load goals:', err);
    }
  }

  function renderGoals(goals) {
    // 1. Dashboard Goals Widget
    const dashContainer = document.getElementById('dashGoalsList');
    if (dashContainer) {
      if (goals.length === 0) {
        dashContainer.innerHTML = `
          <div style="font-size:0.86rem; color:var(--text-muted); text-align:center; padding:12px;">
            No active goals. Set a 7, 14, 28, or 30-day discipline goal.
          </div>
        `;
      } else {
        dashContainer.innerHTML = goals.slice(0, 3).map(g => `
          <div style="margin-bottom:12px; padding:10px 14px; background:var(--cream-light); border-radius:var(--radius-md); border:1px solid var(--cream-dark);">
            <div style="display:flex; justify-content:space-between; font-size:0.88rem; font-weight:600; margin-bottom:4px;">
              <span style="color:var(--forest-green);">${escapeHtml(g.title)}</span>
              <span>${g.completionPercentage}%</span>
            </div>
            <div class="progress-bar-nature" style="height:6px;">
              <div class="progress-bar-fill ${g.completionPercentage >= 100 ? 'complete' : ''}" style="width:${Math.min(100, g.completionPercentage)}%;"></div>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted); margin-top:4px;">
              <span>${g.currentProgressDays} / ${g.targetDays} Days</span>
              <span>${g.remainingDays} days left</span>
            </div>
          </div>
        `).join('');
      }
    }

    // 2. Full Goals View
    const gridContainer = document.getElementById('goalsGridContainer');
    if (gridContainer) {
      if (goals.length === 0) {
        gridContainer.innerHTML = `
          <div style="grid-column:1/-1; text-align:center; padding:40px; background:var(--cream-light); border-radius:var(--radius-md);">
            <div style="font-size:2rem; margin-bottom:8px;">🎯</div>
            <div style="font-weight:600; color:var(--forest-green);">No goals active right now</div>
            <p style="font-size:0.86rem; color:var(--text-muted);">Commit to a 7-day, 14-day, 28-day, or custom duration target.</p>
            <button class="btn-forge btn-forge-primary btn-forge-sm" onclick="App.showModal('modalAddGoal')">+ Create Goal Target</button>
          </div>
        `;
        return;
      }

      gridContainer.innerHTML = goals.map(g => `
        <div class="card-forge">
          <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px;">
            <div>
              <h3 style="font-size:1.15rem; color:var(--forest-green); margin:0 0 4px 0;">${escapeHtml(g.title)}</h3>
              <span class="today-badge-pill" style="font-size:0.72rem; padding:2px 8px;">
                ${g.targetDays}-Day Target
              </span>
            </div>
            <button class="btn-item-action delete" onclick="App.deleteGoal(${g.id}, '${escapeAttr(g.title)}')" title="Remove Goal">✕</button>
          </div>

          <div style="margin-bottom:14px;">
            <div style="display:flex; justify-content:space-between; font-weight:600; font-size:0.9rem; margin-bottom:6px;">
              <span style="color:var(--text-secondary);">Goal Progress</span>
              <span style="color:var(--forest-green);">${g.completionPercentage}%</span>
            </div>
            <div class="progress-bar-nature" style="height:10px;">
              <div class="progress-bar-fill ${g.completionPercentage >= 100 ? 'complete' : ''}" style="width:${Math.min(100, g.completionPercentage)}%;"></div>
            </div>
          </div>

          <div style="display:flex; justify-content:space-between; font-size:0.8rem; color:var(--text-muted); border-top:1px solid var(--cream); padding-top:10px;">
            <span>Completed Days: <strong style="color:var(--forest-green);">${g.currentProgressDays}</strong></span>
            <span>Target Date: <strong>${formatShortDate(g.targetDate)}</strong></span>
            <span>Remaining: <strong style="color:var(--accent-amber);">${g.remainingDays} days</strong></span>
          </div>
        </div>
      `).join('');
    }
  }

  async function submitAddGoal() {
    const title = document.getElementById('goalTitleInput').value;
    const preset = document.getElementById('goalDurationPreset').value;
    const startDate = document.getElementById('goalStartDateInput').value;

    let targetDays = parseInt(preset, 10);
    if (preset === 'custom') {
      targetDays = parseInt(document.getElementById('goalCustomDaysInput').value, 10);
    }

    if (!title || !title.trim()) {
      showToast('Goal title is required', 'warning');
      return;
    }
    if (isNaN(targetDays) || targetDays < 1) {
      showToast('Target days must be at least 1', 'warning');
      return;
    }

    try {
      await api('/api/goals', {
        method: 'POST',
        body: JSON.stringify({
          title: title.trim(),
          targetDays,
          startDate: startDate || state.todayDate
        })
      });
      showToast(`Goal "${title.trim()}" activated`);
      hideModal('modalAddGoal');
      document.getElementById('goalTitleInput').value = '';
      await loadGoals();
    } catch (err) {}
  }

  async function deleteGoal(id, title) {
    if (!confirm(`Remove goal "${title}"?`)) return;
    try {
      await api(`/api/goals/${id}`, { method: 'DELETE' });
      showToast('Goal removed');
      await loadGoals();
    } catch (err) {}
  }

  // =========================================================================
  // STREAKS & ACHIEVEMENTS ENGINE
  // =========================================================================
  async function loadStreaks() {
    if (!state.token) return;
    try {
      const streak = await api('/api/streaks');
      state.streak = streak;

      // Topbar streak
      const topbarStreakText = document.getElementById('topbarStreakText');
      if (topbarStreakText) topbarStreakText.textContent = `${streak.currentStreak} Day Streak`;

      // Dashboard streak
      const dashCurrent = document.getElementById('dashCurrentStreak');
      if (dashCurrent) dashCurrent.textContent = streak.currentStreak;

      const dashLongest = document.getElementById('dashLongestStreak');
      if (dashLongest) dashLongest.textContent = streak.longestStreak;

      // Streaks View big numbers
      const bigCurr = document.getElementById('streakBigNumber');
      if (bigCurr) bigCurr.textContent = streak.currentStreak;

      const bigLong = document.getElementById('longestBigNumber');
      if (bigLong) bigLong.textContent = streak.longestStreak;

      const bigTotal = document.getElementById('totalSuccessfulBigNumber');
      if (bigTotal) bigTotal.textContent = streak.totalSuccessfulDays;

      // Load Achievements
      const achievements = await api('/api/streaks/achievements');
      state.achievements = achievements;
      renderAchievements(achievements);
    } catch (err) {
      console.error('Failed to load streaks:', err);
    }
  }

  function renderAchievements(achievements) {
    // 1. Dashboard Milestones
    const dashContainer = document.getElementById('dashMilestonesList');
    if (dashContainer) {
      const unlocked = achievements.filter(a => a.unlocked);
      if (unlocked.length === 0) {
        dashContainer.innerHTML = `
          <div style="font-size:0.84rem; color:var(--text-muted); padding:8px;">
            Complete your daily checklist 100% to unlock your first milestone badge!
          </div>
        `;
      } else {
        dashContainer.innerHTML = unlocked.slice(0, 4).map(a => `
          <div style="min-width:110px; text-align:center; padding:10px; background:var(--cream-light); border-radius:var(--radius-md); border:1px solid var(--leaf-border);">
            <div style="font-size:1.6rem; margin-bottom:4px;">${a.icon}</div>
            <div style="font-weight:700; font-size:0.75rem; color:var(--forest-green); line-height:1.2;">${escapeHtml(a.title)}</div>
          </div>
        `).join('');
      }
    }

    // 2. Full Streaks View Badges Grid
    const fullContainer = document.getElementById('badgesContainer');
    if (fullContainer) {
      fullContainer.innerHTML = achievements.map(a => `
        <div class="badge-card ${a.unlocked ? '' : 'locked'}">
          <div class="badge-icon-box">${a.icon}</div>
          <div class="badge-title">${escapeHtml(a.title)}</div>
          <div class="badge-desc">${escapeHtml(a.description)}</div>
          <div style="margin-top:8px; font-size:0.72rem; font-weight:600; color:${a.unlocked ? 'var(--forest-green)' : 'var(--text-muted)'};">
            ${a.unlocked ? '✓ Unlocked' : '🔒 Locked'}
          </div>
        </div>
      `).join('');
    }
  }

  // =========================================================================
  // CALENDAR & HISTORY INSPECTOR
  // =========================================================================
  async function renderCalendar() {
    const month = state.calendarMonth;
    const year = state.calendarYear;

    const label = document.getElementById('calendarMonthYearLabel');
    if (label) {
      const d = new Date(year, month - 1, 1);
      label.textContent = d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    }

    try {
      const historyData = await api(`/api/reports/month?year=${year}&month=${month}`);
      const historyMap = {};
      historyData.forEach(h => {
        historyMap[h.date] = h;
      });

      const container = document.getElementById('calendarCellsContainer');
      if (!container) return;

      container.innerHTML = '';

      const firstDayOfMonth = new Date(year, month - 1, 1).getDay(); // 0 = Sun
      const daysInMonth = new Date(year, month, 0).getDate();

      // Empty padding cells
      for (let i = 0; i < firstDayOfMonth; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'calendar-cell';
        emptyCell.style.border = 'none';
        emptyCell.style.background = 'transparent';
        container.appendChild(emptyCell);
      }

      const todayStr = state.todayDate || new Date().toISOString().split('T')[0];

      // Day cells
      for (let day = 1; day <= daysInMonth; day++) {
        const monthPad = String(month).padStart(2, '0');
        const dayPad = String(day).padStart(2, '0');
        const dateStr = `${year}-${monthPad}-${dayPad}`;

        const isToday = (dateStr === todayStr);
        const isFuture = (dateStr > todayStr);
        const rec = historyMap[dateStr];

        const cell = document.createElement('div');
        cell.className = 'calendar-cell';
        cell.setAttribute('data-date', dateStr);

        let cellClass = '';
        let dotColor = '';

        if (isToday) {
          cell.classList.add('today');
        }

        if (isFuture) {
          cell.classList.add('future');
          cell.title = 'Upcoming date (protected from premature modification)';
        } else if (rec) {
          if (rec.successful && rec.total > 0) {
            cell.classList.add('perfect');
            cell.title = `100% Completed (${rec.completed}/${rec.total})`;
          } else if (rec.completed > 0) {
            cell.classList.add('partial');
            cell.title = `Partial (${rec.completed}/${rec.total} Done)`;
          } else if (rec.total > 0) {
            cell.classList.add('missed');
            cell.title = 'Missed / Incomplete';
          }
        }

        cell.innerHTML = `<span>${day}</span>`;
        if (rec && rec.total > 0) {
          const dot = document.createElement('div');
          dot.className = 'calendar-cell-dot';
          dot.style.background = rec.successful ? '#FFFFFF' : 'var(--forest-green)';
          cell.appendChild(dot);
        }

        cell.addEventListener('click', () => {
          document.querySelectorAll('.calendar-cell').forEach(c => c.classList.remove('selected'));
          cell.classList.add('selected');
          inspectCalendarDate(dateStr, isToday, isFuture);
        });

        container.appendChild(cell);
      }

      // Automatically inspect today's date on calendar open
      inspectCalendarDate(todayStr, true, false);

    } catch (err) {
      console.error('Failed to render calendar:', err);
    }
  }

  async function inspectCalendarDate(dateStr, isToday, isFuture) {
    const titleEl = document.getElementById('selectedDateInspectorTitle');
    const badgeEl = document.getElementById('selectedDateProtectionBadge');
    const scoreEl = document.getElementById('selectedDateScoreBadge');
    const container = document.getElementById('selectedDateChecklistDetails');

    if (titleEl) titleEl.textContent = formatDateReadable(dateStr);

    if (badgeEl) {
      if (isToday) {
        badgeEl.textContent = 'Today (Active Checklist)';
        badgeEl.style.background = 'var(--leaf-green)';
        badgeEl.style.color = 'var(--forest-green)';
      } else if (isFuture) {
        badgeEl.textContent = 'Upcoming Day (Locked)';
        badgeEl.style.background = '#E2E8F0';
        badgeEl.style.color = '#718096';
      } else {
        badgeEl.textContent = 'Archived History (Read-Only)';
        badgeEl.style.background = 'var(--leaf-soft)';
        badgeEl.style.color = 'var(--forest-green)';
      }
    }

    try {
      const data = await api(`/api/checklist/date/${dateStr}`);
      if (scoreEl) scoreEl.textContent = `${Math.round(data.completionPercentage || 0)}%`;

      if (!container) return;

      if (!data.items || data.items.length === 0) {
        container.innerHTML = `
          <div style="text-align:center; padding:20px; color:var(--text-muted); font-size:0.86rem;">
            ${isFuture ? 'No activities planned yet for this upcoming date.' : 'No checklist records found for this past date.'}
          </div>
        `;
        return;
      }

      container.innerHTML = data.items.map(item => `
        <div class="checklist-item ${item.completed ? 'completed' : ''}" style="padding:10px 14px; margin-bottom:6px;">
          <div class="item-left">
            <span style="font-weight:700; color:${item.completed ? 'var(--forest-green)' : 'var(--text-muted)'};">
              ${item.completed ? '✓' : '☐'}
            </span>
            <div class="item-details">
              <span class="item-title" style="font-size:0.9rem;">${escapeHtml(item.title)}</span>
              <div class="item-meta">
                <span class="item-category-tag">${item.category || 'GENERAL'}</span>
                <span>${item.completed ? 'Completed' : 'Not completed'}</span>
              </div>
            </div>
          </div>
        </div>
      `).join('');

    } catch (err) {
      if (container) container.innerHTML = `<p style="color:var(--text-muted); font-size:0.86rem;">Could not load checklist details for ${dateStr}.</p>`;
    }
  }

  // =========================================================================
  // HISTORICAL LOG TABLE
  // =========================================================================
  async function loadHistory() {
    if (!state.token) return;
    try {
      const records = await api('/api/reports/history');
      state.history = records;
      renderHistoryTable(records);
    } catch (err) {
      console.error('Failed to load history:', err);
    }
  }

  function renderHistoryTable(records) {
    const container = document.getElementById('historyTableContainer');
    if (!container) return;

    if (records.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding:30px; color:var(--text-muted);">
          No historical records yet. Complete today's checklist to start building your record!
        </div>
      `;
      return;
    }

    container.innerHTML = `
      <div style="overflow-x:auto;">
        <table style="width:100%; border-collapse:collapse; font-size:0.88rem; text-align:left;">
          <thead>
            <tr style="border-bottom:2px solid var(--cream-dark); color:var(--forest-green);">
              <th style="padding:10px 12px;">Date</th>
              <th style="padding:10px 12px;">Total Tasks</th>
              <th style="padding:10px 12px;">Completed</th>
              <th style="padding:10px 12px;">Score</th>
              <th style="padding:10px 12px;">Outcome Status</th>
              <th style="padding:10px 12px;">Action</th>
            </tr>
          </thead>
          <tbody>
            ${records.map(r => {
              let statusBadge = '<span class="correction-badge safe">100% Perfect</span>';
              if (r.completionPercentage < 100 && r.completedTasks > 0) {
                statusBadge = '<span class="correction-badge warning">Partial</span>';
              } else if (r.completedTasks === 0 && r.totalTasks > 0) {
                statusBadge = '<span class="correction-badge locked">Missed</span>';
              }

              return `
                <tr style="border-bottom:1px solid var(--cream-dark);">
                  <td style="padding:12px; font-weight:600; color:var(--text-primary);">${formatDateReadable(r.date)}</td>
                  <td style="padding:12px;">${r.totalTasks}</td>
                  <td style="padding:12px; color:var(--forest-green); font-weight:600;">${r.completedTasks}</td>
                  <td style="padding:12px; font-weight:700;">${Math.round(r.completionPercentage)}%</td>
                  <td style="padding:12px;">${statusBadge}</td>
                  <td style="padding:12px;">
                    <button class="btn-forge btn-forge-secondary btn-forge-sm" onclick="App.viewHistoricalDay('${r.date}')">
                      View Tasks
                    </button>
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;
  }

  function viewHistoricalDay(dateStr) {
    navigateTo('calendar');
    setTimeout(() => {
      inspectCalendarDate(dateStr, false, false);
    }, 150);
  }

  // =========================================================================
  // PRODUCTIVITY REPORTS & CHARTS
  // =========================================================================
  async function renderReports() {
    if (!state.token) return;
    try {
      const summary = await api('/api/reports/summary');

      // 1. Line Chart: 14-Day Trend
      const trendCanvas = document.getElementById('chartDailyTrend');
      if (trendCanvas && summary.trend14Days) {
        const labels = Object.keys(summary.trend14Days);
        const dataValues = Object.values(summary.trend14Days);

        if (state.charts.trend) state.charts.trend.destroy();

        if (typeof Chart !== 'undefined') {
          state.charts.trend = new Chart(trendCanvas, {
            type: 'line',
            data: {
              labels: labels,
              datasets: [{
                label: 'Completion %',
                data: dataValues,
                borderColor: '#244C3B',
                backgroundColor: 'rgba(120, 155, 118, 0.2)',
                tension: 0.3,
                fill: true,
                pointBackgroundColor: '#244C3B',
                pointRadius: 4
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  min: 0,
                  max: 100,
                  ticks: { callback: v => `${v}%` }
                }
              },
              plugins: {
                legend: { display: false }
              }
            }
          });
        }
      }

      // 2. Doughnut Chart: Category Breakdown
      const catCanvas = document.getElementById('chartCategoryDist');
      if (catCanvas && summary.categoryDistribution) {
        const cats = Object.keys(summary.categoryDistribution);
        const counts = Object.values(summary.categoryDistribution);

        if (state.charts.category) state.charts.category.destroy();

        if (typeof Chart !== 'undefined' && cats.length > 0) {
          state.charts.category = new Chart(catCanvas, {
            type: 'doughnut',
            data: {
              labels: cats,
              datasets: [{
                data: counts,
                backgroundColor: ['#244C3B', '#789B76', '#C9D9B8', '#D9822B', '#3E7B99']
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: { position: 'bottom' }
              }
            }
          });
        }
      }

      // 3. Summary Performance Table
      const summaryTable = document.getElementById('reportsSummaryTable');
      if (summaryTable) {
        summaryTable.innerHTML = `
          <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(180px, 1fr)); gap:14px;">
            <div style="background:var(--cream-light); padding:14px; border-radius:var(--radius-md); border:1px solid var(--cream-dark);">
              <div style="font-size:0.75rem; color:var(--text-muted); text-transform:uppercase;">Total Completed Tasks</div>
              <div style="font-size:1.6rem; font-weight:800; color:var(--forest-green);">${summary.totalCompletedTasks}</div>
            </div>
            <div style="background:var(--cream-light); padding:14px; border-radius:var(--radius-md); border:1px solid var(--cream-dark);">
              <div style="font-size:0.75rem; color:var(--text-muted); text-transform:uppercase;">Total Missed Tasks</div>
              <div style="font-size:1.6rem; font-weight:800; color:var(--accent-red);">${summary.totalMissedTasks}</div>
            </div>
            <div style="background:var(--cream-light); padding:14px; border-radius:var(--radius-md); border:1px solid var(--cream-dark);">
              <div style="font-size:0.75rem; color:var(--text-muted); text-transform:uppercase;">Active Streak Days</div>
              <div style="font-size:1.6rem; font-weight:800; color:var(--accent-amber);">${summary.currentStreak}</div>
            </div>
            <div style="background:var(--cream-light); padding:14px; border-radius:var(--radius-md); border:1px solid var(--cream-dark);">
              <div style="font-size:0.75rem; color:var(--text-muted); text-transform:uppercase;">Best Streak Record</div>
              <div style="font-size:1.6rem; font-weight:800; color:var(--forest-green);">${summary.longestStreak}</div>
            </div>
          </div>
        `;
      }

    } catch (err) {
      console.error('Failed to render reports:', err);
    }
  }

  // Data Export Functions
  function exportReportCSV() {
    if (!state.history || state.history.length === 0) {
      showToast('No history available to export', 'warning');
      return;
    }
    let csv = 'Date,Total Tasks,Completed Tasks,Completion Percentage,Successful\n';
    state.history.forEach(r => {
      csv += `${r.date},${r.totalTasks},${r.completedTasks},${r.completionPercentage},${r.isSuccessful}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `keizoku_productivity_${state.todayDate || 'export'}.csv`;
    a.click();
    showToast('Productivity CSV exported successfully');
  }

  function exportReportJSON() {
    exportFullBackup();
  }

  function exportFullBackup() {
    const backupData = {
      user: state.user,
      today: state.todayDate,
      streak: state.streak,
      goals: state.goals,
      habits: state.habits,
      history: state.history,
      exportedAt: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `keizoku_backup_${state.todayDate || 'export'}.json`;
    a.click();
    showToast('Full JSON backup downloaded');
  }

  // =========================================================================
  // NOTIFICATIONS SYSTEM
  // =========================================================================
  async function loadNotifications() {
    if (!state.token) return;
    try {
      const notices = await api('/api/notifications');
      const container = document.getElementById('notificationsFeedList');
      if (container) {
        if (notices.length === 0) {
          container.innerHTML = `
            <div style="text-align:center; padding:20px; color:var(--text-muted); font-size:0.86rem;">
              No pending notifications. All systems clean!
            </div>
          `;
        } else {
          container.innerHTML = notices.map(n => `
            <div class="checklist-item" style="border-left:4px solid var(--forest-green); padding:12px 16px;">
              <div>
                <div style="font-weight:700; font-size:0.92rem; color:var(--forest-green);">${escapeHtml(n.title)}</div>
                <div style="font-size:0.82rem; color:var(--text-secondary); margin-top:2px;">${escapeHtml(n.message)}</div>
                <div style="font-size:0.72rem; color:var(--text-muted); margin-top:4px;">${n.time}</div>
              </div>
            </div>
          `).join('');
        }
      }

      // Check Notification Preferences
      const prefs = await api('/api/notifications/preferences');
      const timeInput = document.getElementById('prefDailyReminderTime');
      if (timeInput) timeInput.value = prefs.dailyReminderTime || '08:00';

      const reminderCheck = document.getElementById('prefDailyReminderEnabled');
      if (reminderCheck) reminderCheck.checked = prefs.dailyReminderEnabled;

      const pendingCheck = document.getElementById('prefPendingReminder');
      if (pendingCheck) pendingCheck.checked = prefs.pendingTaskReminder;

      const streakCheck = document.getElementById('prefStreakWarning');
      if (streakCheck) streakCheck.checked = prefs.streakWarningEnabled;

      const qStart = document.getElementById('prefQuietStart');
      if (qStart) qStart.value = prefs.quietHoursStart || '22:00';

      const qEnd = document.getElementById('prefQuietEnd');
      if (qEnd) qEnd.value = prefs.quietHoursEnd || '07:00';

      // Browser Notification Permission status
      updateBrowserNotifStatus();

    } catch (err) {
      console.error('Failed to load notifications:', err);
    }
  }

  function updateBrowserNotifStatus() {
    const textEl = document.getElementById('browserNotifStatusText');
    if (!textEl) return;
    if (!('Notification' in window)) {
      textEl.textContent = 'Web notifications not supported in this browser.';
    } else if (Notification.permission === 'granted') {
      textEl.textContent = '✓ Browser notifications granted and active.';
      textEl.style.color = 'var(--forest-green)';
    } else if (Notification.permission === 'denied') {
      textEl.textContent = '✕ Browser notifications blocked in browser settings.';
      textEl.style.color = 'var(--accent-red)';
    } else {
      textEl.textContent = 'Browser notification permission has not yet been requested.';
    }
  }

  async function requestBrowserNotificationPermission() {
    if (!('Notification' in window)) {
      showToast('Notifications are not supported by this browser', 'warning');
      return;
    }
    const perm = await Notification.requestPermission();
    updateBrowserNotifStatus();
    if (perm === 'granted') {
      showToast('Notification permission granted!');
      try {
        new Notification('KEIZOKU', {
          body: 'Discipline notifications are now active.',
          icon: '/assets/logo.png'
        });
      } catch (e) {}
    } else {
      showToast('Notification permission denied or dismissed', 'warning');
    }
  }

  async function saveNotificationPreferences() {
    const dailyReminderTime = document.getElementById('prefDailyReminderTime').value;
    const dailyReminderEnabled = document.getElementById('prefDailyReminderEnabled').checked;
    const pendingTaskReminder = document.getElementById('prefPendingReminder').checked;
    const streakWarningEnabled = document.getElementById('prefStreakWarning').checked;
    const quietHoursStart = document.getElementById('prefQuietStart').value;
    const quietHoursEnd = document.getElementById('prefQuietEnd').value;

    try {
      await api('/api/notifications/preferences', {
        method: 'PUT',
        body: JSON.stringify({
          dailyReminderEnabled,
          dailyReminderTime,
          pendingTaskReminder,
          streakWarningEnabled,
          quietHoursEnabled: true,
          quietHoursStart,
          quietHoursEnd,
          browserNotificationsEnabled: (Notification.permission === 'granted')
        })
      });
      showToast('Notification settings saved successfully');
    } catch (err) {}
  }

  function clearAllNotifications() {
    const container = document.getElementById('notificationsFeedList');
    if (container) {
      container.innerHTML = '<div style="text-align:center; padding:20px; color:var(--text-muted); font-size:0.86rem;">All notifications cleared.</div>';
    }
    showToast('Notifications cleared');
  }

  // =========================================================================
  // GENERAL SETTINGS & TIMEZONE
  // =========================================================================
  async function saveGeneralSettings() {
    const tz = document.getElementById('settingTimezoneSelect').value;
    const theme = document.getElementById('settingThemeSelect').value;

    try {
      await api('/api/user/settings', {
        method: 'PUT',
        body: JSON.stringify({ timezone: tz })
      });

      // Apply theme
      applyTheme(theme);
      showToast(`Settings saved. Local timezone updated to ${tz}`);
      if (state.user) state.user.timezone = tz;
      updateUserUI();
      await loadTodayChecklist();
    } catch (err) {}
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('keizoku_theme', theme);
    localStorage.setItem('dayforge_theme', theme);
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    const next = (current === 'light') ? 'dark' : 'light';
    applyTheme(next);
  }

  // Utility helpers
  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, m => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[m]));
  }

  function escapeAttr(str) {
    if (!str) return '';
    return str.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function formatTime(isoStr) {
    if (!isoStr) return '';
    try {
      const d = new Date(isoStr);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return '';
    }
  }

  // Setup Event Listeners
  function bindEvents() {
    // Nav menu items
    document.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const section = link.getAttribute('data-section');
        if (section) navigateTo(section);
      });
    });

    // Mobile nav buttons
    document.querySelectorAll('.mobile-nav-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const section = btn.getAttribute('data-section');
        if (section) navigateTo(section);
      });
    });

    // Mobile drawer toggle
    const mobileMenuBtn = document.getElementById('btnMobileMenuToggle');
    if (mobileMenuBtn) {
      mobileMenuBtn.addEventListener('click', () => {
        const sidebar = document.getElementById('sidebar');
        if (sidebar) sidebar.classList.toggle('open');
      });
    }

    // Top theme toggle
    const themeBtn = document.getElementById('btnThemeToggle');
    if (themeBtn) {
      themeBtn.addEventListener('click', toggleTheme);
    }

    // Top bell
    const bellBtn = document.getElementById('btnTopBell');
    if (bellBtn) {
      bellBtn.addEventListener('click', () => navigateTo('notifications'));
    }

    // Sidebar auth button
    const authToggle = document.getElementById('btnSidebarAuthToggle');
    if (authToggle) {
      authToggle.addEventListener('click', () => showModal('modalAuth'));
    }

    // Modal Add Buttons
    const btnOpenBatch = document.getElementById('btnOpenBatchAddModal');
    if (btnOpenBatch) {
      btnOpenBatch.addEventListener('click', () => showModal('modalBatchAdd'));
    }

    const btnOpenSingle = document.getElementById('btnOpenSingleAddModal');
    if (btnOpenSingle) {
      btnOpenSingle.addEventListener('click', () => showModal('modalSingleAdd'));
    }

    const btnOpenHabit = document.getElementById('btnOpenAddHabitModal');
    if (btnOpenHabit) {
      btnOpenHabit.addEventListener('click', () => showModal('modalAddHabit'));
    }

    // Dashboard quick batch add
    const btnDashBatch = document.getElementById('btnDashBatchAdd');
    if (btnDashBatch) {
      btnDashBatch.addEventListener('click', () => {
        const input = document.getElementById('dashQuickBatchInput');
        if (input) submitBatchAdd(input.value, 'GENERAL');
      });
    }

    // Submits inside modals
    const btnSubmitBatch = document.getElementById('btnSubmitBatchAdd');
    if (btnSubmitBatch) {
      btnSubmitBatch.addEventListener('click', () => {
        const text = document.getElementById('batchInputTextarea').value;
        const cat = document.getElementById('batchCategorySelect').value;
        submitBatchAdd(text, cat);
      });
    }

    const btnSubmitSingle = document.getElementById('btnSubmitSingleAdd');
    if (btnSubmitSingle) {
      btnSubmitSingle.addEventListener('click', () => {
        const title = document.getElementById('singleItemTitle').value;
        const cat = document.getElementById('singleItemCategory').value;
        submitSingleAdd(title, cat);
      });
    }

    const btnSubmitEdit = document.getElementById('btnSubmitEditItem');
    if (btnSubmitEdit) {
      btnSubmitEdit.addEventListener('click', submitEditItem);
    }

    const btnSubmitDelete = document.getElementById('btnSubmitConfirmDelete');
    if (btnSubmitDelete) {
      btnSubmitDelete.addEventListener('click', submitConfirmDelete);
    }

    const btnSubmitHabit = document.getElementById('btnSubmitAddHabit');
    if (btnSubmitHabit) {
      btnSubmitHabit.addEventListener('click', submitAddHabit);
    }

    // Dynamic Habit Rows Builder Listener
    const btnAddHabitRow = document.getElementById('btnAddHabitRow');
    if (btnAddHabitRow) {
      btnAddHabitRow.addEventListener('click', () => addHabitRow());
    }

    const initialHabitInp = document.querySelector('.habit-row-title');
    if (initialHabitInp) {
      initialHabitInp.addEventListener('input', updateHabitRowsState);
    }

    const btnSubmitGoal = document.getElementById('btnSubmitAddGoal');
    if (btnSubmitGoal) {
      btnSubmitGoal.addEventListener('click', submitAddGoal);
    }

    // Habit frequency dropdown listener for custom days toggle
    const habitFreq = document.getElementById('habitFrequencySelect');
    if (habitFreq) {
      habitFreq.addEventListener('change', (e) => {
        const customGroup = document.getElementById('habitCustomDaysGroup');
        if (customGroup) {
          customGroup.style.display = (e.target.value === 'CUSTOM_DAYS') ? 'block' : 'none';
        }
      });
    }

    // Goal duration preset toggle for custom duration input
    const goalPreset = document.getElementById('goalDurationPreset');
    if (goalPreset) {
      goalPreset.addEventListener('change', (e) => {
        const customGroup = document.getElementById('goalCustomDurationGroup');
        if (customGroup) {
          customGroup.style.display = (e.target.value === 'custom') ? 'block' : 'none';
        }
      });
    }

    // Calendar month controls
    const btnPrevMonth = document.getElementById('btnCalPrevMonth');
    if (btnPrevMonth) {
      btnPrevMonth.addEventListener('click', () => {
        state.calendarMonth--;
        if (state.calendarMonth < 1) {
          state.calendarMonth = 12;
          state.calendarYear--;
        }
        renderCalendar();
      });
    }

    const btnNextMonth = document.getElementById('btnCalNextMonth');
    if (btnNextMonth) {
      btnNextMonth.addEventListener('click', () => {
        state.calendarMonth++;
        if (state.calendarMonth > 12) {
          state.calendarMonth = 1;
          state.calendarYear++;
        }
        renderCalendar();
      });
    }

    // History filter reset
    const btnResetHistory = document.getElementById('btnResetHistoryFilter');
    if (btnResetHistory) {
      btnResetHistory.addEventListener('click', () => {
        const picker = document.getElementById('historyDateFilter');
        if (picker) picker.value = '';
        renderHistoryTable(state.history);
      });
    }

    const historyFilter = document.getElementById('historyDateFilter');
    if (historyFilter) {
      historyFilter.addEventListener('change', (e) => {
        const val = e.target.value;
        if (!val) {
          renderHistoryTable(state.history);
        } else {
          const filtered = state.history.filter(h => h.date === val);
          renderHistoryTable(filtered);
        }
      });
    }

    // Notification permission & settings buttons
    const btnReqNotif = document.getElementById('btnRequestBrowserNotif');
    if (btnReqNotif) {
      btnReqNotif.addEventListener('click', requestBrowserNotificationPermission);
    }

    const btnSaveNotif = document.getElementById('btnSaveNotifSettings');
    if (btnSaveNotif) {
      btnSaveNotif.addEventListener('click', saveNotificationPreferences);
    }

    // General settings save
    const btnSaveGen = document.getElementById('btnSaveGeneralSettings');
    if (btnSaveGen) {
      btnSaveGen.addEventListener('click', saveGeneralSettings);
    }

    // Sign out button
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
      btnLogout.addEventListener('click', () => {
        localStorage.removeItem('keizoku_token');
        localStorage.removeItem('dayforge_token');
        state.token = '';
        state.user = null;
        showToast('Signed out successfully');
        initAuth().then(() => loadTodayChecklist());
      });
    }

    // Auth Modal Tabs & Forms
    const tabLogin = document.getElementById('btnTabLogin');
    const tabRegister = document.getElementById('btnTabRegister');
    const formLogin = document.getElementById('formLogin');
    const formRegister = document.getElementById('formRegister');

    if (tabLogin && tabRegister) {
      tabLogin.addEventListener('click', () => {
        tabLogin.style.borderBottom = '2px solid var(--forest-green)';
        tabRegister.style.borderBottom = 'none';
        formLogin.style.display = 'block';
        formRegister.style.display = 'none';
        document.getElementById('authModalTitle').textContent = 'Sign In to KEIZOKU';
      });

      tabRegister.addEventListener('click', () => {
        tabRegister.style.borderBottom = '2px solid var(--forest-green)';
        tabLogin.style.borderBottom = 'none';
        formLogin.style.display = 'none';
        formRegister.style.display = 'block';
        document.getElementById('authModalTitle').textContent = 'Create KEIZOKU Account';
      });
    }

    if (formLogin) {
      formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        const u = document.getElementById('loginUsername').value;
        const p = document.getElementById('loginPassword').value;
        try {
          const res = await api('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username: u, password: p })
          });
          state.token = res.token;
          localStorage.setItem('keizoku_token', state.token);
          localStorage.setItem('dayforge_token', state.token);
          state.user = res;
          showToast(`Welcome back, ${res.fullName}!`);
          updateUserUI();
          hideModal('modalAuth');
          await loadTodayChecklist();
        } catch (err) {}
      });
    }

    if (formRegister) {
      formRegister.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fullName = document.getElementById('regFullName').value;
        const username = document.getElementById('regUsername').value;
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;

        try {
          const clientTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata';
          const res = await api('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ fullName, username, email, password, timezone: clientTz })
          });
          state.token = res.token;
          localStorage.setItem('keizoku_token', state.token);
          localStorage.setItem('dayforge_token', state.token);
          state.user = res;
          showToast(`Account created! Welcome, ${res.fullName}!`);
          updateUserUI();
          hideModal('modalAuth');
          await loadTodayChecklist();
        } catch (err) {}
      });
    }
  }

  async function resetAllUserData() {
    if (!confirm('Are you sure you want to permanently delete all checklist items, habits, goals, and history? This cannot be undone.')) {
      return;
    }
    try {
      await api('/api/user/reset-data', { method: 'POST' });
      showToast('All records deleted successfully. Slate is clean.');
      await loadTodayChecklist();
      await loadHabits();
      await loadGoals();
      await loadStreaks();
      await loadHistory();
      renderReports();
    } catch (err) {}
  }

  // App Bootstrap
  async function init() {
    // Theme restore
    const savedTheme = localStorage.getItem('keizoku_theme') || localStorage.getItem('dayforge_theme') || 'light';
    applyTheme(savedTheme);

    bindEvents();
    await initAuth();
    if (state.token) {
      await loadTodayChecklist();
    }
  }

  // Public Interface
  return {
    init,
    navigateTo,
    showModal,
    hideModal,
    filterTodayCategory,
    toggleItemStatus,
    openEditItemModal,
    openDeleteConfirmModal,
    reorderItem,
    toggleHabitActive,
    deleteHabit,
    deleteGoal,
    inspectCalendarDate,
    viewHistoricalDay,
    clearAllNotifications,
    exportReportCSV,
    exportReportJSON,
    exportFullBackup,
    resetAllUserData
  };
})();

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

