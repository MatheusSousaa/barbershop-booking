// Dicionários para tradução de enums e status em português
const DAY_NAMES = {
  'MONDAY': 'Segunda-feira',
  'TUESDAY': 'Terça-feira',
  'WEDNESDAY': 'Quarta-feira',
  'THURSDAY': 'Quinta-feira',
  'FRIDAY': 'Sexta-feira',
  'SATURDAY': 'Sábado',
  'SUNDAY': 'Domingo'
};

const STATUS_NAMES = {
  'ACTIVE': 'Confirmado',
  'CANCELLED': 'Cancelado'
};

// JWT armazenado estritamente em memória JS
let authToken = null;
let currentBarber = null;

document.addEventListener('DOMContentLoaded', () => {
  setupLogin();
  setupTabs();
  setupAgendaControls();
  setupServiceControls();
  setupScheduleControls();
  setupBlockControls();
});

// Autenticação com tratamento de 401
function setupLogin() {
  const form = document.getElementById('login-form');
  const logoutBtn = document.getElementById('btn-logout');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAdminMessage();

    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('btn-login');

    btn.disabled = true;
    btn.textContent = 'Autenticando...';

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (res.ok) {
        const data = await res.json();
        authToken = data.token;
        currentBarber = data;
        showDashboard();
      } else {
        const err = await res.json().catch(() => ({}));
        showAdminMessage(res.status === 401
          ? 'E-mail ou senha incorretos.'
          : err.detail || 'Não foi possível acessar o painel agora. Tente novamente.', 'error');
      }
    } catch (err) {
      showAdminMessage('Não foi possível conectar ao painel. Verifique a conexão e tente novamente.', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Entrar';
    }
  });

  logoutBtn.addEventListener('click', () => {
    authToken = null;
    currentBarber = null;
    document.getElementById('dashboard-section').classList.add('hidden');
    document.getElementById('login-section').classList.remove('hidden');
    document.getElementById('login-password').value = '';
    showAdminMessage('Sessão encerrada.', 'success');
  });
}

function showDashboard() {
  document.getElementById('login-section').classList.add('hidden');
  document.getElementById('dashboard-section').classList.remove('hidden');
  document.getElementById('logged-user-name').textContent = currentBarber.displayName || currentBarber.email;

  // Inicializa a data da agenda para hoje (sem bug de fuso)
  const dateInput = document.getElementById('agenda-date');
  const todayStr = BarbershopUtils.getBahiaDateString();
  dateInput.value = todayStr;
  document.getElementById('blk-date').value = todayStr;

  loadDailyAgenda(todayStr);
  loadAdminServices();
  loadAdminSchedules();
  loadAdminBlocks();
}

// Wrapper seguro para chamadas autenticadas
async function authFetch(url, options = {}) {
  if (!authToken) {
    handleUnauthorized();
    throw new Error('Não autenticado');
  }

  options.headers = options.headers || {};
  options.headers['Authorization'] = `Bearer ${authToken}`;

  const res = await fetch(url, options);
  if (res.status === 401 || res.status === 403) {
    handleUnauthorized();
    throw new Error('Sessão expirada ou não autorizada');
  }
  return res;
}

function handleUnauthorized() {
  authToken = null;
  currentBarber = null;
  document.getElementById('dashboard-section').classList.add('hidden');
  document.getElementById('login-section').classList.remove('hidden');
  showAdminMessage('Sua sessão expirou. Faça login novamente.', 'error');
  document.getElementById('login-email').focus();
}

// Controle de abas
function setupTabs() {
  const tabs = document.querySelectorAll('.tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => {
        t.classList.remove('active');
        t.setAttribute('aria-selected', 'false');
        t.tabIndex = -1;
      });
      document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));

      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');
      tab.tabIndex = 0;
      const targetId = tab.getAttribute('data-tab');
      document.getElementById(targetId).classList.remove('hidden');
    });

    tab.addEventListener('keydown', (event) => {
      const currentIndex = Array.from(tabs).indexOf(tab);
      let nextIndex;

      if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % tabs.length;
      else if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
      else if (event.key === 'Home') nextIndex = 0;
      else if (event.key === 'End') nextIndex = tabs.length - 1;
      else return;

      event.preventDefault();
      tabs[nextIndex].focus();
      tabs[nextIndex].click();
    });
  });
}

// --- AGENDA ---
function setupAgendaControls() {
  const btnDaily = document.getElementById('btn-load-daily');
  const btnWeekly = document.getElementById('btn-load-weekly');
  const dateInput = document.getElementById('agenda-date');

  btnDaily.addEventListener('click', () => {
    if (dateInput.value) loadDailyAgenda(dateInput.value);
  });

  btnWeekly.addEventListener('click', () => {
    if (dateInput.value) loadWeeklyAgenda(dateInput.value);
  });
}

async function loadDailyAgenda(dateStr) {
  const listEl = document.getElementById('agenda-list');
  listEl.replaceChildren();

  const loading = document.createElement('div');
  loading.className = 'feedback-box feedback-loading';
  loading.textContent = 'Carregando agenda do dia...';
  listEl.appendChild(loading);

  try {
    const res = await authFetch(`/api/admin/appointments/daily?date=${encodeURIComponent(dateStr)}`);
    if (!res.ok) throw new Error('Não foi possível carregar a agenda do dia.');
    const data = await res.json();
    renderAppointments(data, listEl, `Nenhum agendamento ativo para ${BarbershopUtils.formatDateDisplay(dateStr)}.`);
  } catch (err) {
    showListError(listEl, err.message || 'Falha ao carregar a agenda. Tente novamente.');
  }
}

async function loadWeeklyAgenda(dateStr) {
  const listEl = document.getElementById('agenda-list');
  listEl.replaceChildren();

  const loading = document.createElement('div');
  loading.className = 'feedback-box feedback-loading';
  loading.textContent = 'Carregando agenda da semana...';
  listEl.appendChild(loading);

  try {
    const res = await authFetch(`/api/admin/appointments/weekly?fromDate=${encodeURIComponent(dateStr)}`);
    if (!res.ok) throw new Error('Não foi possível carregar a agenda da semana.');
    const data = await res.json();
    renderAppointments(data, listEl, 'Nenhum agendamento para este período de 7 dias.');
  } catch (err) {
    showListError(listEl, err.message || 'Falha ao carregar a agenda. Tente novamente.');
  }
}

function renderAppointments(appointments, container, emptyMsg) {
  container.replaceChildren();

  if (appointments.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'feedback-box feedback-empty';
    empty.setAttribute('role', 'status');
    empty.textContent = emptyMsg;
    container.appendChild(empty);
    return;
  }

  appointments.forEach(apt => {
    const item = document.createElement('div');
    item.className = 'appointment-item';

    const header = document.createElement('div');
    header.className = 'appointment-header';

    const timeEl = document.createElement('span');
    timeEl.className = 'appointment-time';
    timeEl.textContent = `${BarbershopUtils.formatDateDisplay(apt.appointmentDate)} às ${apt.startTime.substring(0, 5)}`;

    const statusBadge = document.createElement('span');
    statusBadge.className = `badge-status badge-${apt.status.toLowerCase()}`;
    statusBadge.textContent = STATUS_NAMES[apt.status] || apt.status;

    header.appendChild(timeEl);
    header.appendChild(statusBadge);
    item.appendChild(header);

    const clientInfo = document.createElement('div');
    clientInfo.textContent = `Cliente: ${apt.clientName} (${apt.clientPhone})`;
    item.appendChild(clientInfo);

    const serviceInfo = document.createElement('div');
    serviceInfo.textContent = `Serviço: ${apt.serviceName} — R$ ${parseFloat(apt.servicePrice).toFixed(2).replace('.', ',')}`;
    item.appendChild(serviceInfo);

    if (apt.status === 'ACTIVE') {
      const cancelBtn = document.createElement('button');
      cancelBtn.type = 'button';
      cancelBtn.className = 'btn-danger';
      cancelBtn.textContent = 'Cancelar Agendamento';
      cancelBtn.addEventListener('click', () => promptCancelAppointment(apt, item, statusBadge, cancelBtn));
      item.appendChild(cancelBtn);
    }

    container.appendChild(item);
  });
}

async function promptCancelAppointment(appointment, item, statusBadge, cancelBtn) {
  const reason = prompt('Informe o motivo do cancelamento:');
  if (!reason || !reason.trim()) return;

  try {
    const res = await authFetch(`/api/admin/appointments/${appointment.id}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: reason.trim() })
    });

    if (res.ok) {
      showAdminMessage('Agendamento cancelado com sucesso. O horário voltou a ficar livre!', 'success');
      statusBadge.className = 'badge-status badge-cancelled';
      statusBadge.textContent = STATUS_NAMES.CANCELLED;
      cancelBtn.remove();

      const phone = BarbershopUtils.normalizeBrazilianPhone(appointment.clientPhone);
      if (phone) {
        const whatsappLink = document.createElement('a');
        whatsappLink.className = 'btn-whatsapp';
        whatsappLink.textContent = 'Avisar cliente no WhatsApp';
        whatsappLink.href = `https://wa.me/${phone}?text=${encodeURIComponent(
          `Olá, ${appointment.clientName}. Seu agendamento de ${appointment.serviceName} para ${BarbershopUtils.formatDateDisplay(appointment.appointmentDate)} às ${appointment.startTime.substring(0, 5)} foi cancelado.`
        )}`;
        whatsappLink.target = '_blank';
        whatsappLink.rel = 'noopener noreferrer';
        item.appendChild(whatsappLink);
      } else {
        showAdminMessage('Agendamento cancelado, mas o telefone cadastrado não é válido para o WhatsApp.', 'error');
      }
    } else {
      const err = await res.json().catch(() => ({}));
      showAdminMessage(err.detail || 'Não foi possível cancelar o agendamento.', 'error');
    }

  } catch (err) {
    showAdminMessage('Erro ao processar cancelamento.', 'error');
  }
}

// --- SERVIÇOS ---
function setupServiceControls() {
  const form = document.getElementById('service-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAdminMessage();

    const name = document.getElementById('srv-name').value.trim();
    const description = document.getElementById('srv-desc').value.trim();
    const price = parseFloat(document.getElementById('srv-price').value);

    try {
      const res = await authFetch('/api/admin/services', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description, price })
      });

      if (res.ok) {
        showAdminMessage('Serviço cadastrado com sucesso!', 'success');
        form.reset();
        await loadAdminServices();
      } else {
        const err = await res.json().catch(() => ({}));
        showAdminMessage(err.detail || 'Erro ao cadastrar serviço.', 'error');
      }
    } catch (err) {
      showAdminMessage('Erro de conexão.', 'error');
    }
  });
}

async function loadAdminServices() {
  const listEl = document.getElementById('admin-services-list');
  showLoadingState(listEl, 'Carregando serviços...');
  try {
    const res = await authFetch('/api/admin/services');
    if (!res.ok) throw new Error('Não foi possível carregar os serviços cadastrados.');
    const services = await res.json();
    listEl.replaceChildren();

    if (services.length === 0) {
      showEmptyState(listEl, 'Nenhum serviço cadastrado.');
      return true;
    }

    services.forEach(s => {
      const card = document.createElement('div');
      card.className = 'service-card';
      card.style.marginBottom = '0.5rem';

      const info = document.createElement('div');
      info.className = 'service-info';

      const name = document.createElement('div');
      name.className = 'name';
      name.textContent = `${s.name} ${s.active ? '' : '(Inativo)'}`;
      if (!s.active) name.style.color = 'var(--color-muted)';
      info.appendChild(name);

      const price = document.createElement('div');
      price.className = 'price';
      price.textContent = `R$ ${parseFloat(s.price).toFixed(2).replace('.', ',')}`;

      card.appendChild(info);
      card.appendChild(price);

      const photoControls = document.createElement('div');
      photoControls.className = 'admin-service-photo';
      if (s.photoUrl) {
        const preview = document.createElement('img');
        preview.src = s.photoUrl;
        preview.alt = `Foto de ${s.name}`;
        preview.width = 64;
        preview.height = 80;
        photoControls.appendChild(preview);
      } else {
        const missingPhoto = document.createElement('span');
        missingPhoto.textContent = 'Sem foto';
        photoControls.appendChild(missingPhoto);
      }

      const photoInput = document.createElement('input');
      photoInput.type = 'file';
      photoInput.accept = 'image/jpeg,image/png';
      photoInput.className = 'visually-hidden';
      photoInput.tabIndex = -1;
      photoInput.setAttribute('aria-label', `Escolher foto para ${s.name}`);
      photoInput.addEventListener('change', () => uploadServicePhoto(s.id, photoInput, photoButton));

      const photoButton = document.createElement('button');
      photoButton.type = 'button';
      photoButton.className = 'btn-outline service-photo-upload';
      photoButton.textContent = s.photoUrl ? 'Trocar foto' : 'Adicionar foto';
      photoButton.setAttribute('aria-label', `${photoButton.textContent} para ${s.name}`);
      photoButton.addEventListener('click', () => photoInput.click());
      photoControls.append(photoInput, photoButton);
      card.appendChild(photoControls);

      if (s.active) {
        const delBtn = document.createElement('button');
        delBtn.type = 'button';
        delBtn.className = 'btn-danger';
        delBtn.style.margin = '0 0 0 1rem';
        delBtn.textContent = 'Desativar';
        delBtn.addEventListener('click', async () => {
          if (confirm(`Deseja desativar o serviço "${s.name}"?`)) {
            try {
              const deleteResponse = await authFetch(`/api/admin/services/${s.id}`, { method: 'DELETE' });
              if (!deleteResponse.ok) throw new Error('Não foi possível desativar o serviço.');
              await loadAdminServices();
            } catch (err) {
              showAdminMessage(err.message || 'Falha ao desativar o serviço.', 'error');
            }
          }
        });
        card.appendChild(delBtn);
      }

      listEl.appendChild(card);
    });
    return true;
  } catch (err) {
    showListError(listEl, err.message || 'Falha ao carregar os serviços cadastrados.');
    return false;
  }
}

async function uploadServicePhoto(serviceId, photoInput, uploadButton) {
  const file = photoInput.files?.[0];
  if (!file) return;
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    showAdminMessage('Escolha uma imagem JPG ou PNG.', 'error');
    photoInput.value = '';
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    showAdminMessage('A foto deve ter no máximo 5 MB.', 'error');
    photoInput.value = '';
    return;
  }

  const originalLabel = uploadButton.textContent;
  uploadButton.disabled = true;
  uploadButton.textContent = 'Enviando...';
  const formData = new FormData();
  formData.append('photo', file);

  try {
    const response = await authFetch(`/api/admin/services/${serviceId}/photo`, {
      method: 'POST',
      body: formData
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.detail || 'Não foi possível salvar a foto.');
    }

    if (await loadAdminServices()) {
      showAdminMessage('Foto do serviço salva com sucesso.', 'success');
    }
  } catch (error) {
    showAdminMessage(error.message || 'Não foi possível enviar a foto. Verifique a conexão e tente novamente.', 'error');
  } finally {
    photoInput.value = '';
    uploadButton.disabled = false;
    uploadButton.textContent = originalLabel;
  }
}

// --- EXPEDIENTE ---
function setupScheduleControls() {
  const workdayCb = document.getElementById('sch-workday');
  const timesContainer = document.getElementById('sch-times-container');
  workdayCb.addEventListener('change', () => {
    timesContainer.style.display = workdayCb.checked ? 'block' : 'none';
  });

  const form = document.getElementById('schedule-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAdminMessage();

    const dayOfWeek = document.getElementById('sch-day').value;
    const workDay = workdayCb.checked;
    const startTime = workDay ? document.getElementById('sch-start').value + ':00' : null;
    const endTime = workDay ? document.getElementById('sch-end').value + ':00' : null;
    const breakStart = (workDay && document.getElementById('sch-break-start').value) ? document.getElementById('sch-break-start').value + ':00' : null;
    const breakEnd = (workDay && document.getElementById('sch-break-end').value) ? document.getElementById('sch-break-end').value + ':00' : null;

    try {
      const res = await authFetch('/api/admin/schedules', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ dayOfWeek, workDay, startTime, endTime, breakStart, breakEnd })
      });

      if (res.ok) {
        showAdminMessage('Expediente atualizado com sucesso!', 'success');
        loadAdminSchedules();
      } else {
        const err = await res.json().catch(() => ({}));
        showAdminMessage(err.detail || 'Erro ao salvar expediente.', 'error');
      }
    } catch (err) {
      showAdminMessage(err.message || 'Não foi possível salvar o expediente. Tente novamente.', 'error');
    }
  });
}

async function loadAdminSchedules() {
  const listEl = document.getElementById('admin-schedules-list');
  showLoadingState(listEl, 'Carregando expediente...');
  try {
    const res = await authFetch('/api/admin/schedules');
    if (!res.ok) throw new Error('Não foi possível carregar o expediente atual.');
    const schedules = await res.json();
    listEl.replaceChildren();

    if (schedules.length === 0) {
      showEmptyState(listEl, 'Nenhum expediente configurado.');
      return;
    }

    schedules.forEach(sc => {
      const row = document.createElement('div');
      row.style.padding = '0.5rem 0';
      row.style.borderBottom = '1px solid var(--color-border)';

      const title = document.createElement('strong');
      title.textContent = DAY_NAMES[sc.dayOfWeek] || sc.dayOfWeek;
      row.appendChild(title);

      const desc = document.createElement('div');
      desc.style.color = 'var(--color-muted)';
      desc.style.fontSize = '0.85rem';

      if (sc.workDay) {
        desc.textContent = `${sc.startTime.substring(0, 5)} até ${sc.endTime.substring(0, 5)}` +
          (sc.breakStart ? ` (Intervalo: ${sc.breakStart.substring(0, 5)} - ${sc.breakEnd.substring(0, 5)})` : '');
      } else {
        desc.textContent = 'Folga semanal';
      }

      row.appendChild(desc);
      listEl.appendChild(row);
    });
  } catch (err) {
    showListError(listEl, err.message || 'Falha ao carregar o expediente.');
  }
}

// --- BLOQUEIOS ---
function setupBlockControls() {
  const dateInput = document.getElementById('blk-date');
  const todayStr = BarbershopUtils.getBahiaDateString();
  dateInput.min = todayStr;
  dateInput.value = todayStr;
  const fulldayCb = document.getElementById('blk-fullday');
  const timesContainer = document.getElementById('blk-times-container');
  fulldayCb.addEventListener('change', () => {
    timesContainer.style.display = fulldayCb.checked ? 'none' : 'block';
  });

  const form = document.getElementById('block-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearAdminMessage();

    const date = document.getElementById('blk-date').value;
    const fullDay = fulldayCb.checked;
    const startTime = (!fullDay && document.getElementById('blk-start').value) ? document.getElementById('blk-start').value + ':00' : null;
    const endTime = (!fullDay && document.getElementById('blk-end').value) ? document.getElementById('blk-end').value + ':00' : null;
    const reason = document.getElementById('blk-reason').value.trim();

    try {
      const res = await authFetch('/api/admin/blocked-slots', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ date, fullDay, startTime, endTime, reason })
      });

      if (res.ok) {
        showAdminMessage('Bloqueio cadastrado!', 'success');
        form.reset();
        dateInput.value = BarbershopUtils.getBahiaDateString();
        timesContainer.style.display = 'block';
        loadAdminBlocks();
      } else {
        const err = await res.json().catch(() => ({}));
        showAdminMessage(err.detail || 'Erro ao criar bloqueio.', 'error');
      }
    } catch (err) {
      showAdminMessage(err.message || 'Não foi possível salvar o bloqueio. Tente novamente.', 'error');
    }
  });
}

async function loadAdminBlocks() {
  const listEl = document.getElementById('admin-blocks-list');
  showLoadingState(listEl, 'Carregando bloqueios...');
  try {
    const res = await authFetch('/api/admin/blocked-slots');
    if (!res.ok) throw new Error('Não foi possível carregar os bloqueios ativos.');
    const blocks = await res.json();
    listEl.replaceChildren();

    if (blocks.length === 0) {
      showEmptyState(listEl, 'Nenhum bloqueio cadastrado.');
      return;
    }

    blocks.forEach(b => {
      const item = document.createElement('div');
      item.className = 'appointment-item';

      const title = document.createElement('div');
      title.textContent = `Data: ${BarbershopUtils.formatDateDisplay(b.date)}`;
      item.appendChild(title);

      const period = document.createElement('div');
      period.style.color = 'var(--color-muted)';
      period.style.fontSize = '0.85rem';
      period.textContent = b.fullDay ? 'Dia inteiro bloqueado' : `Horário: ${b.startTime.substring(0, 5)} às ${b.endTime.substring(0, 5)}`;
      item.appendChild(period);

      if (b.reason) {
        const r = document.createElement('div');
        r.style.fontSize = '0.85rem';
        r.textContent = `Motivo: ${b.reason}`;
        item.appendChild(r);
      }

      const delBtn = document.createElement('button');
      delBtn.type = 'button';
      delBtn.className = 'btn-danger';
      delBtn.textContent = 'Remover Bloqueio';
      delBtn.addEventListener('click', async () => {
        try {
          const deleteResponse = await authFetch(`/api/admin/blocked-slots/${b.id}`, { method: 'DELETE' });
          if (!deleteResponse.ok) throw new Error('Não foi possível remover o bloqueio.');
          await loadAdminBlocks();
        } catch (err) {
          showAdminMessage(err.message || 'Falha ao remover o bloqueio.', 'error');
        }
      });

      item.appendChild(delBtn);
      listEl.appendChild(item);
    });
  } catch (err) {
    showListError(listEl, err.message || 'Falha ao carregar os bloqueios ativos.');
  }
}

function showListError(container, message) {
  container.replaceChildren();
  const error = document.createElement('div');
  error.className = 'feedback-box feedback-error';
  error.setAttribute('role', 'alert');
  error.textContent = message;
  container.appendChild(error);
}

function showLoadingState(container, message) {
  container.replaceChildren();
  const loading = document.createElement('div');
  loading.className = 'feedback-box feedback-loading';
  loading.setAttribute('role', 'status');
  loading.textContent = message;
  container.appendChild(loading);
}

function showEmptyState(container, message) {
  const empty = document.createElement('div');
  empty.className = 'feedback-box feedback-empty';
  empty.setAttribute('role', 'status');
  empty.textContent = message;
  container.appendChild(empty);
}

function showAdminMessage(msg, type) {
  const el = document.getElementById('admin-message');
  el.textContent = msg;
  el.className = `feedback-box feedback-${type}`;
  el.setAttribute('role', type === 'error' ? 'alert' : 'status');
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
  el.scrollIntoView({ behavior, block: 'nearest' });
}

function clearAdminMessage() {
  const el = document.getElementById('admin-message');
  el.textContent = '';
  el.className = 'hidden';
}
