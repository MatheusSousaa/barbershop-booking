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
let editingService = null;
let serviceEditorReturnFocus = null;
let deletingService = null;
let deleteServiceReturnFocus = null;

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

  setupServiceEditor();
  setupServiceDeletionDialog();
}

async function loadAdminServices() {
  const activeList = document.getElementById('admin-active-services-list');
  const inactiveList = document.getElementById('admin-inactive-services-list');
  showLoadingState(activeList, 'Carregando serviços ativos...');
  showLoadingState(inactiveList, 'Carregando serviços desativados...');

  try {
    const res = await authFetch('/api/admin/services');
    if (!res.ok) throw new Error('Não foi possível carregar os serviços cadastrados.');
    const services = await res.json();
    activeList.replaceChildren();
    inactiveList.replaceChildren();

    const activeServices = services.filter(service => service.active);
    const inactiveServices = services.filter(service => !service.active);
    if (activeServices.length === 0) showEmptyState(activeList, 'Nenhum serviço ativo.');
    if (inactiveServices.length === 0) showEmptyState(inactiveList, 'Nenhum serviço desativado.');
    activeServices.forEach(service => activeList.appendChild(createAdminServiceCard(service)));
    inactiveServices.forEach(service => inactiveList.appendChild(createAdminServiceCard(service)));
    return true;
  } catch (err) {
    const message = err.message || 'Falha ao carregar os serviços cadastrados.';
    showListError(activeList, message);
    showListError(inactiveList, message);
    return false;
  }
}

function createAdminServiceCard(service) {
  const card = document.createElement('article');
  card.className = `service-card admin-service-card${service.active ? '' : ' is-inactive'}`;

  const info = document.createElement('div');
  info.className = 'service-info';
  const name = document.createElement('div');
  name.className = 'name';
  name.textContent = service.name;
  info.appendChild(name);

  const badge = document.createElement('span');
  badge.className = `service-status-badge${service.active ? '' : ' is-inactive'}`;
  badge.textContent = service.active ? 'Ativo' : 'Desativado';
  info.appendChild(badge);

  if (service.description) {
    const description = document.createElement('p');
    description.className = 'desc';
    description.textContent = service.description;
    info.appendChild(description);
  }
  card.appendChild(info);

  const price = document.createElement('div');
  price.className = 'price';
  price.textContent = `R$ ${parseFloat(service.price).toFixed(2).replace('.', ',')}`;
  card.appendChild(price);

  const photoControls = document.createElement('div');
  photoControls.className = 'admin-service-photo';
  if (service.photoUrl) {
    const preview = document.createElement('img');
    preview.src = service.photoUrl;
    preview.alt = `Foto de ${service.name}`;
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
  photoInput.setAttribute('aria-label', `Escolher foto para ${service.name}`);
  const photoButton = createServiceButton(
    service.photoUrl ? 'Trocar foto' : 'Adicionar foto',
    'btn-outline service-photo-upload',
    () => photoInput.click()
  );
  photoButton.setAttribute('aria-label', `${photoButton.textContent} para ${service.name}`);
  photoInput.addEventListener('change', () => uploadServicePhoto(service.id, photoInput, photoButton));
  photoControls.append(photoInput, photoButton);
  card.appendChild(photoControls);

  const actions = document.createElement('div');
  actions.className = 'admin-service-actions';
  actions.appendChild(createServiceButton('Editar', 'btn-outline', button => openServiceEditor(service, button)));
  if (service.active) {
    actions.appendChild(createServiceButton('Desativar', 'btn-secondary', button => {
      if (window.confirm(`Deseja desativar o serviço "${service.name}"?`)) {
        updateServiceActive(service, false, button);
      }
    }));
  } else {
    actions.appendChild(createServiceButton('Reativar', 'btn-secondary', button => {
      updateServiceActive(service, true, button);
    }));
    actions.appendChild(createServiceButton('Apagar', 'btn-danger', button => openDeleteServiceDialog(service, button)));
  }
  card.appendChild(actions);
  return card;
}

function createServiceButton(label, className, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = className;
  button.textContent = label;
  button.addEventListener('click', () => onClick(button));
  return button;
}

function setupServiceEditor() {
  const form = document.getElementById('service-editor-form');
  const editor = document.getElementById('service-editor');
  const saveButton = document.getElementById('edit-srv-save');
  const fields = {
    name: document.getElementById('edit-srv-name'),
    description: document.getElementById('edit-srv-desc'),
    price: document.getElementById('edit-srv-price')
  };

  Object.entries(fields).forEach(([key, field]) => {
    field.addEventListener('input', () => setServiceFieldError(key, ''));
  });

  document.getElementById('edit-srv-cancel').addEventListener('click', closeServiceEditor);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!editingService || !validateServiceEditor(fields)) return;

    saveButton.disabled = true;
    saveButton.textContent = 'Salvando...';
    const payload = {
      name: fields.name.value.trim(),
      description: fields.description.value.trim(),
      price: Number(fields.price.value),
      active: document.getElementById('edit-srv-active').checked
    };

    try {
      const response = await authFetch(`/api/admin/services/${editingService.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (response.status === 404) {
        showAdminMessage('Este serviço não foi encontrado. A lista foi atualizada.', 'error');
        closeServiceEditor();
        await loadAdminServices();
        document.getElementById('tab-services-control').focus();
        return;
      }
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        showServiceValidationErrors(error.detail || 'Não foi possível salvar o serviço.');
        return;
      }

      closeServiceEditor();
      await loadAdminServices();
      document.getElementById('tab-services-control').focus();
      showAdminMessage('Serviço atualizado com sucesso.', 'success');
    } catch (error) {
      if (authToken) showAdminMessage('Não foi possível conectar. Verifique a conexão e tente novamente.', 'error');
    } finally {
      saveButton.disabled = false;
      saveButton.textContent = 'Salvar alterações';
    }
  });

  editor.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeServiceEditor();
  });
}

function validateServiceEditor(fields) {
  let valid = true;
  const name = fields.name.value.trim();
  const price = Number(fields.price.value);
  setServiceFieldError('name', '');
  setServiceFieldError('description', '');
  setServiceFieldError('price', '');

  if (!name) {
    setServiceFieldError('name', 'Informe o nome do serviço.');
    valid = false;
  } else if (name.length > 100) {
    setServiceFieldError('name', 'O nome deve ter no máximo 100 caracteres.');
    valid = false;
  }
  if (fields.description.value.length > 500) {
    setServiceFieldError('description', 'A descrição deve ter no máximo 500 caracteres.');
    valid = false;
  }
  if (!Number.isFinite(price) || price <= 0) {
    setServiceFieldError('price', 'Informe um preço maior que zero.');
    valid = false;
  }
  return valid;
}

function setServiceFieldError(fieldName, message) {
  const fieldId = fieldName === 'description' ? 'desc' : fieldName;
  const input = document.getElementById(`edit-srv-${fieldId}`);
  const error = document.getElementById(`edit-srv-${fieldId}-error`);
  input.setAttribute('aria-invalid', message ? 'true' : 'false');
  error.textContent = message;
}

function showServiceValidationErrors(detail) {
  const messages = detail.split(';').map(message => message.trim()).filter(Boolean);
  let shownInline = false;
  messages.forEach(message => {
    const separator = message.indexOf(':');
    if (separator < 0) return;
    const field = message.slice(0, separator).trim();
    const key = field === 'name' ? 'name' : field === 'description' ? 'description' : field === 'price' ? 'price' : null;
    if (!key) return;
    setServiceFieldError(key, message.slice(separator + 1).trim());
    shownInline = true;
  });
  if (!shownInline) showAdminMessage(detail, 'error');
}

function openServiceEditor(service, trigger) {
  editingService = service;
  serviceEditorReturnFocus = trigger;
  document.getElementById('edit-srv-name').value = service.name;
  document.getElementById('edit-srv-desc').value = service.description || '';
  document.getElementById('edit-srv-price').value = service.price;
  document.getElementById('edit-srv-active').checked = service.active;
  ['name', 'description', 'price'].forEach(field => setServiceFieldError(field, ''));
  const editor = document.getElementById('service-editor');
  editor.classList.remove('hidden');
  editor.scrollIntoView({ behavior: 'smooth', block: 'center' });
  document.getElementById('edit-srv-name').focus();
}

function closeServiceEditor() {
  const editor = document.getElementById('service-editor');
  editor.classList.add('hidden');
  editingService = null;
  if (serviceEditorReturnFocus?.isConnected) serviceEditorReturnFocus.focus();
  serviceEditorReturnFocus = null;
}

async function updateServiceActive(service, active, button) {
  button.disabled = true;
  try {
    const response = await authFetch(`/api/admin/services/${service.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: service.name,
        description: service.description || '',
        price: Number(service.price),
        active
      })
    });
    if (response.status === 404) {
      showAdminMessage('Este serviço não foi encontrado. A lista foi atualizada.', 'error');
    } else if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      showAdminMessage(error.detail || 'Não foi possível atualizar o status do serviço.', 'error');
      return;
    } else {
      showAdminMessage(active ? 'Serviço reativado.' : 'Serviço desativado.', 'success');
    }
    await loadAdminServices();
  } catch (error) {
    if (authToken) showAdminMessage('Não foi possível conectar. Verifique a conexão e tente novamente.', 'error');
  } finally {
    button.disabled = false;
  }
}

function setupServiceDeletionDialog() {
  const dialog = document.getElementById('delete-service-dialog');
  const deleteButton = document.getElementById('delete-service-confirm');
  const error = document.getElementById('delete-service-error');

  dialog.addEventListener('close', () => {
    deletingService = null;
    error.textContent = '';
    if (deleteServiceReturnFocus?.isConnected) deleteServiceReturnFocus.focus();
    else document.getElementById('tab-services-control').focus();
    deleteServiceReturnFocus = null;
  });
  deleteButton.addEventListener('click', async () => {
    if (!deletingService) return;
    deleteButton.disabled = true;
    error.textContent = '';
    try {
      const response = await authFetch(`/api/admin/services/${deletingService.id}`, { method: 'DELETE' });
      if (response.status === 404) {
        dialog.close();
        await loadAdminServices();
        document.getElementById('tab-services-control').focus();
        showAdminMessage('Este serviço não foi encontrado. A lista foi atualizada.', 'error');
        return;
      }
      if (!response.ok) {
        const detail = await response.json().then(data => data.detail).catch(() => '');
        error.textContent = detail || 'Não foi possível apagar o serviço.';
        return;
      }
      dialog.close();
      await loadAdminServices();
      document.getElementById('tab-services-control').focus();
      showAdminMessage('Serviço apagado permanentemente.', 'success');
    } catch (requestError) {
      if (authToken) error.textContent = 'Não foi possível conectar. Verifique a conexão e tente novamente.';
    } finally {
      deleteButton.disabled = false;
    }
  });
}

function openDeleteServiceDialog(service, trigger) {
  deletingService = service;
  deleteServiceReturnFocus = trigger;
  document.getElementById('delete-service-description').textContent =
    `Deseja apagar "${service.name}"? Esta ação é permanente.`;
  document.getElementById('delete-service-error').textContent = '';
  const dialog = document.getElementById('delete-service-dialog');
  dialog.showModal();
  document.getElementById('delete-service-cancel').focus();
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
