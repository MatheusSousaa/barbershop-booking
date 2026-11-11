// Estado em memória do fluxo público de agendamento.
const DATE_STRIP_DAYS = 7;
const bookingState = {
  selectedServiceId: null,
  selectedServiceName: '',
  selectedServicePrice: '',
  selectedDate: '',
  selectedTime: '',
  confirmedAppointment: null,
  slotRequestId: 0,
  activeStep: 1
};

document.addEventListener('DOMContentLoaded', () => {
  initializeDateStrip();
  initializePhoneInput();
  initializeBookingEvents();
  loadServices();
});

function getBahiaDateParts() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: BarbershopUtils.timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date());
  return Object.fromEntries(parts.map(({ type, value }) => [type, value]));
}

function getBahiaMinutesOfDay() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: BarbershopUtils.timeZone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return Number(values.hour) * 60 + Number(values.minute);
}

function createDateStripDays() {
  const { year, month, day } = getBahiaDateParts();
  const firstDay = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)));
  const formatters = {
    weekday: new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', weekday: 'long' }),
    weekdayShort: new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', weekday: 'short' }),
    date: new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', day: 'numeric', month: 'long', year: 'numeric' }),
    monthShort: new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', month: 'short' })
  };

  return Array.from({ length: DATE_STRIP_DAYS }, (_, index) => {
    const date = new Date(firstDay.getTime() + index * 24 * 60 * 60 * 1000);
    const previousDate = new Date(date.getTime() - 24 * 60 * 60 * 1000);
    const dateString = [
      date.getUTCFullYear(),
      String(date.getUTCMonth() + 1).padStart(2, '0'),
      String(date.getUTCDate()).padStart(2, '0')
    ].join('-');
    const monthChanged = index === 0
      || date.getUTCMonth() !== previousDate.getUTCMonth()
      || date.getUTCFullYear() !== previousDate.getUTCFullYear();

    return {
      dateString,
      dayNumber: String(date.getUTCDate()),
      weekdayShort: formatters.weekdayShort.format(date).replace('.', ''),
      relativeLabel: index === 0 ? 'Hoje' : index === 1 ? 'Amanhã' : '',
      monthLabel: monthChanged ? formatters.monthShort.format(date).replace('.', '') : '',
      ariaLabel: `${formatters.weekday.format(date)}, ${formatters.date.format(date)}`
    };
  });
}

function initializeDateStrip() {
  const strip = document.getElementById('date-strip');
  const days = createDateStripDays();
  bookingState.selectedDate = days[0].dateString;

  days.forEach((day, index) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'day-option';
    button.setAttribute('role', 'radio');
    button.setAttribute('aria-label', day.ariaLabel);
    button.setAttribute('aria-checked', String(index === 0));
    button.tabIndex = index === 0 ? 0 : -1;
    button.dataset.date = day.dateString;

    const relative = document.createElement('span');
    relative.className = 'day-option-relative';
    relative.textContent = day.relativeLabel;
    const weekday = document.createElement('span');
    weekday.className = 'day-option-weekday';
    weekday.textContent = day.weekdayShort;
    const number = document.createElement('span');
    number.className = 'day-option-number';
    number.textContent = day.dayNumber;
    button.append(relative, weekday, number);
    if (day.monthLabel) {
      const month = document.createElement('span');
      month.className = 'day-option-month';
      month.textContent = day.monthLabel;
      button.appendChild(month);
    }
    button.addEventListener('click', () => selectDateStripDay(button));
    button.addEventListener('keydown', handleDateStripKeydown);
    strip.appendChild(button);
  });

  loadSlots(bookingState.selectedDate);
}

async function selectDateStripDay(button) {
  const dateString = button.dataset.date;
  if (!dateString || dateString < BarbershopUtils.getBahiaDateString()) return;

  document.querySelectorAll('.day-option').forEach((option) => {
    const selected = option === button;
    option.setAttribute('aria-checked', String(selected));
    option.tabIndex = selected ? 0 : -1;
  });
  bookingState.selectedDate = dateString;
  clearSelectedTime();
  await loadSlots(dateString);
  if (bookingState.selectedDate === dateString) goToStep(3);
}

function handleDateStripKeydown(event) {
  const options = [...document.querySelectorAll('.day-option')];
  const currentIndex = options.indexOf(event.currentTarget);
  let nextIndex;
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') nextIndex = (currentIndex + 1) % options.length;
  else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') nextIndex = (currentIndex - 1 + options.length) % options.length;
  else if (event.key === 'Home') nextIndex = 0;
  else if (event.key === 'End') nextIndex = options.length - 1;
  else return;

  event.preventDefault();
  options[nextIndex].focus();
  selectDateStripDay(options[nextIndex]);
}

function initializePhoneInput() {
  const phoneInput = document.getElementById('client-phone');
  phoneInput.addEventListener('input', () => {
    const digits = phoneInput.value.replace(/\D/g, '').slice(0, 11);
    if (digits.length > 6) {
      phoneInput.value = `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    } else if (digits.length > 2) {
      phoneInput.value = `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
    } else if (digits.length > 0) {
      phoneInput.value = `(${digits}`;
    } else {
      phoneInput.value = '';
    }
    clearFieldError('phone-error');
  });

  document.getElementById('client-name').addEventListener('input', () => clearFieldError('name-error'));
}

function initializeBookingEvents() {
  document.getElementById('appointment-form').addEventListener('submit', submitAppointment);
  document.getElementById('btn-new-appointment').addEventListener('click', resetBookingFlow);
  document.getElementById('btn-whatsapp-confirmation').addEventListener('click', openBarberWhatsApp);
  document.getElementById('btn-save-calendar').addEventListener('click', saveAppointmentToCalendar);
  initializePhotoViewer();
  document.getElementById('name-error').controlInput = document.getElementById('client-name');
  document.getElementById('phone-error').controlInput = document.getElementById('client-phone');
  document.getElementById('continue-service').addEventListener('click', () => {
    if (bookingState.selectedServiceId) goToStep(2);
    else showGlobalMessage('Selecione um serviço para continuar.', 'error');
  });
  document.getElementById('continue-time').addEventListener('click', () => {
    if (bookingState.selectedTime) goToStep(4);
    else showGlobalMessage('Escolha um horário disponível para continuar.', 'error');
  });
}

async function loadServices() {
  const loadingEl = document.getElementById('services-loading');
  const errorEl = document.getElementById('services-error');
  const listEl = document.getElementById('services-list');
  loadingEl.classList.remove('hidden');
  errorEl.classList.add('hidden');
  listEl.replaceChildren();

  try {
    const response = await fetch('/api/public/services');
    if (!response.ok) throw new Error('Os serviços não estão disponíveis agora.');
    const services = await response.json();
    loadingEl.classList.add('hidden');

    if (!Array.isArray(services) || services.length === 0) {
      errorEl.textContent = 'Nenhum serviço disponível no momento. Tente novamente mais tarde.';
      errorEl.classList.remove('feedback-error');
      errorEl.classList.add('feedback-empty');
      errorEl.setAttribute('role', 'status');
      errorEl.classList.remove('hidden');
      return;
    }

    window.loadedServices = services;
    listEl.setAttribute('aria-busy', 'false');
    services.forEach((service, index) => {
      const card = createServiceCard(service, index === 0);
      listEl.appendChild(card);
      if (index === 0) selectService(service, card);
    });
  } catch (error) {
    loadingEl.classList.add('hidden');
    errorEl.textContent = 'Não foi possível carregar os serviços. Verifique a conexão e tente novamente.';
    errorEl.classList.remove('feedback-empty');
    errorEl.classList.add('feedback-error');
    errorEl.setAttribute('role', 'alert');
    errorEl.classList.remove('hidden');
  } finally {
    listEl.setAttribute('aria-busy', 'false');
  }
}

function createServiceCard(service, selected) {
  const card = document.createElement('article');
  card.className = 'service-card';
  card.dataset.serviceId = String(service.id);

  const photoSet = getServicePhotos(service);
  const photoButton = document.createElement('button');
  photoButton.type = 'button';
  photoButton.className = 'service-photo-button';
  photoButton.setAttribute('aria-label', `Ampliar foto do serviço ${service.name}`);
  const image = document.createElement('img');
  image.className = 'service-photo';
  image.src = photoSet[0].src;
  image.alt = photoSet[0].alt;
  image.width = 800;
  image.height = 1000;
  image.loading = 'lazy';
  image.decoding = 'async';
  image.addEventListener('error', () => {
    image.src = 'img/services/placeholder.svg';
    image.alt = `Imagem ilustrativa padrão para ${service.name}`;
  }, { once: true });
  photoButton.appendChild(image);
  photoButton.addEventListener('click', (event) => {
    event.stopPropagation();
    selectService(service, card);
    openPhotoViewer(photoSet, 0);
  });

  const info = document.createElement('div');
  info.className = 'service-info';
  const radioLabel = document.createElement('label');
  radioLabel.className = 'service-radio-label';
  const radioInput = document.createElement('input');
  radioInput.type = 'radio';
  radioInput.name = 'selected-service';
  radioInput.value = String(service.id);
  radioInput.checked = selected;
  radioInput.setAttribute('aria-label', `Selecionar ${service.name}`);
  const radioText = document.createElement('span');
  radioText.className = 'visually-hidden';
  radioText.textContent = `Selecionar ${service.name}`;
  radioLabel.append(radioInput, radioText);
  const name = document.createElement('h4');
  name.className = 'name';
  name.textContent = service.name;
  const description = document.createElement('p');
  description.className = 'desc';
  description.textContent = service.description || 'Consulte os detalhes deste serviço com a barbearia.';
  const footer = document.createElement('div');
  footer.className = 'service-card-footer';
  const price = document.createElement('span');
  price.className = 'price';
  price.textContent = `R$ ${formatPrice(service.price)}`;
  footer.append(price, radioLabel);
  info.append(name, description, footer);

  card.append(photoButton, info);
  radioInput.addEventListener('change', () => selectService(service, card));

  if (photoSet.length > 1) {
    const thumbnails = document.createElement('div');
    thumbnails.className = 'service-thumbnails';
    thumbnails.setAttribute('aria-label', `Fotos de ${service.name}`);
    photoSet.forEach((photo, photoIndex) => {
      const thumbnail = document.createElement('button');
      thumbnail.type = 'button';
      thumbnail.className = 'service-thumbnail';
      thumbnail.setAttribute('aria-label', `Ampliar foto ${photoIndex + 1} de ${service.name}`);
      const thumbnailImage = document.createElement('img');
      thumbnailImage.src = photo.src;
      thumbnailImage.alt = '';
      thumbnailImage.width = 64;
      thumbnailImage.height = 80;
      thumbnailImage.loading = 'lazy';
      thumbnailImage.decoding = 'async';
      thumbnail.appendChild(thumbnailImage);
      thumbnail.addEventListener('click', (event) => {
        event.stopPropagation();
        selectService(service, card);
        openPhotoViewer(photoSet, photoIndex);
      });
      thumbnails.appendChild(thumbnail);
    });
    card.appendChild(thumbnails);
  }

  card.addEventListener('click', (event) => {
    if (event.target.closest('button, input, label')) return;
    selectService(service, card);
  });
  return card;
}

function getServicePhotos(service) {
  if (service.photoUrl) {
    return [{
      src: service.photoUrl,
      alt: `Foto do serviço ${service.name}`
    }];
  }
  const configured = window.SERVICE_IMAGES?.byId?.[String(service.id)];
  const photos = Array.isArray(configured) ? configured.slice(0, 3) : [];
  if (photos.length === 0) {
    return [{
      src: 'img/services/placeholder.svg',
      alt: `Imagem ilustrativa padrão para ${service.name}`
    }];
  }
  return photos.map((photo) => ({
    src: photo.src || 'img/services/placeholder.svg',
    alt: photo.alt || `Foto de ${service.name}`
  }));
}

function selectService(service, selectedCard) {
  const serviceChanged = bookingState.selectedServiceId !== service.id;
  document.querySelectorAll('.service-card').forEach((card) => {
    const selected = card === selectedCard;
    card.classList.toggle('selected', selected);
    const radioInput = card.querySelector('input[type="radio"]');
    radioInput.checked = selected;
  });
  bookingState.selectedServiceId = service.id;
  bookingState.selectedServiceName = service.name;
  bookingState.selectedServicePrice = service.price;
  if (serviceChanged) {
    clearSelectedTime();
    if (bookingState.selectedDate) loadSlots(bookingState.selectedDate);
  }
}

function formatPrice(price) {
  const value = Number(price);
  return Number.isFinite(value) ? value.toFixed(2).replace('.', ',') : '0,00';
}

let photoViewerReturnFocus = null;
let photoViewerPhotos = [];
let photoViewerIndex = 0;

function openPhotoViewer(photos, index) {
  const dialog = document.getElementById('photo-viewer');
  photoViewerReturnFocus = document.activeElement;
  photoViewerPhotos = photos;
  photoViewerIndex = index;
  renderPhotoViewer();
  dialog.showModal();
  document.getElementById('photo-viewer-close').focus();
}

function renderPhotoViewer() {
  const photo = photoViewerPhotos[photoViewerIndex];
  const image = document.getElementById('photo-viewer-image');
  image.src = photo.src;
  image.alt = photo.alt;
  image.onerror = () => {
    image.onerror = null;
    image.src = 'img/services/placeholder.svg';
    image.alt = 'Imagem ilustrativa padrão do serviço';
  };
  const counter = document.getElementById('photo-viewer-counter');
  counter.textContent = `${photoViewerIndex + 1} de ${photoViewerPhotos.length}`;
  const hasMultiple = photoViewerPhotos.length > 1;
  document.getElementById('photo-viewer-previous').classList.toggle('hidden', !hasMultiple);
  document.getElementById('photo-viewer-next').classList.toggle('hidden', !hasMultiple);
  counter.classList.toggle('hidden', !hasMultiple);
}

function initializePhotoViewer() {
  const dialog = document.getElementById('photo-viewer');
  document.getElementById('photo-viewer-close').addEventListener('click', () => dialog.close());
  document.getElementById('photo-viewer-previous').addEventListener('click', () => {
    photoViewerIndex = (photoViewerIndex - 1 + photoViewerPhotos.length) % photoViewerPhotos.length;
    renderPhotoViewer();
  });
  document.getElementById('photo-viewer-next').addEventListener('click', () => {
    photoViewerIndex = (photoViewerIndex + 1) % photoViewerPhotos.length;
    renderPhotoViewer();
  });
  dialog.addEventListener('click', (event) => {
    if (event.target === dialog) dialog.close();
  });
  dialog.addEventListener('close', () => {
    if (photoViewerReturnFocus?.isConnected) photoViewerReturnFocus.focus();
  });
}

function clearSelectedTime() {
  bookingState.selectedTime = '';
  document.querySelectorAll('.slot-btn').forEach((button) => {
    button.classList.remove('selected');
    button.setAttribute('aria-pressed', 'false');
  });
}

async function loadSlots(dateString) {
  const requestId = ++bookingState.slotRequestId;
  const placeholder = document.getElementById('slots-placeholder');
  const loading = document.getElementById('slots-loading');
  const error = document.getElementById('slots-error');
  const grid = document.getElementById('slots-grid');
  placeholder.classList.add('hidden');
  error.classList.add('hidden');
  error.classList.remove('feedback-empty');
  error.classList.add('feedback-error');
  grid.classList.add('hidden');
  grid.replaceChildren();

  if (!dateString) {
    loading.classList.add('hidden');
    placeholder.textContent = 'Selecione uma data para verificar os horários livres.';
    placeholder.classList.remove('hidden');
    return false;
  }

  loading.classList.remove('hidden');
  try {
    const response = await fetch(`/api/public/slots?date=${encodeURIComponent(dateString)}`);
    if (!response.ok) throw new Error('A consulta de horários falhou.');
    const data = await response.json();
    if (requestId !== bookingState.slotRequestId) return false;
    loading.classList.add('hidden');

    let slots = Array.isArray(data.availableSlots) ? data.availableSlots : [];
    if (dateString === BarbershopUtils.getBahiaDateString()) {
      const currentMinutes = getBahiaMinutesOfDay();
      slots = slots.filter((slot) => {
        const [hours, minutes] = slot.split(':').map(Number);
        return hours * 60 + minutes > currentMinutes;
      });
    }

    if (slots.length === 0) {
      error.textContent = 'Sem horários neste dia, escolha outro';
      error.classList.remove('feedback-error');
      error.classList.add('feedback-empty');
      error.setAttribute('role', 'status');
      error.classList.remove('hidden');
      return false;
    }

    slots.forEach((slot) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'slot-btn';
      button.textContent = slot.substring(0, 5);
      button.setAttribute('aria-pressed', 'false');
      button.addEventListener('click', () => {
        clearSelectedTime();
        button.classList.add('selected');
        button.setAttribute('aria-pressed', 'true');
        bookingState.selectedTime = slot;
      });
      grid.appendChild(button);
    });
    grid.classList.remove('hidden');
    return true;
  } catch (requestError) {
    if (requestId !== bookingState.slotRequestId) return false;
    loading.classList.add('hidden');
    error.textContent = 'Não foi possível consultar os horários. Verifique a conexão e tente novamente.';
    error.setAttribute('role', 'alert');
    error.classList.remove('hidden');
    return false;
  }
}

function showFieldError(id, message) {
  const error = document.getElementById(id);
  error.textContent = message;
  error.classList.remove('hidden');
  const input = error.controlInput;
  if (input) {
    input.setAttribute('aria-invalid', 'true');
    input.setAttribute('aria-describedby', id);
  }
}

function clearFieldError(id) {
  const error = document.getElementById(id);
  if (!error) return;
  error.textContent = '';
  error.classList.add('hidden');
  if (error.controlInput) {
    error.controlInput.removeAttribute('aria-invalid');
    error.controlInput.setAttribute('aria-describedby', id);
  }
}

function validateBookingForm() {
  const nameInput = document.getElementById('client-name');
  const phoneInput = document.getElementById('client-phone');
  const name = nameInput.value.trim();
  const phone = phoneInput.value.replace(/\D/g, '');
  let valid = true;

  if (name.length < 2) {
    showFieldError('name-error', 'Informe seu nome com pelo menos 2 caracteres.');
    nameInput.focus();
    valid = false;
  } else {
    clearFieldError('name-error');
  }

  if (!/^\d{2}9\d{8}$/.test(phone)) {
    showFieldError('phone-error', 'Informe um celular com DDD e 9 dígitos, por exemplo (00) 00000-0000.');
    if (valid) phoneInput.focus();
    valid = false;
  } else {
    clearFieldError('phone-error');
  }
  return valid;
}

async function submitAppointment(event) {
  event.preventDefault();
  if (bookingState.isSubmitting) return;
  clearGlobalMessage();

  if (!bookingState.selectedServiceId) {
    showGlobalMessage('Selecione um serviço antes de continuar.', 'error');
    goToStep(1);
    return;
  }
  if (!bookingState.selectedDate || bookingState.selectedDate < BarbershopUtils.getBahiaDateString()) {
    goToStep(2);
    document.querySelector('.day-option[aria-checked="true"]')?.focus();
    return;
  }
  if (!bookingState.selectedTime) {
    showGlobalMessage('Escolha um horário disponível antes de confirmar.', 'error');
    goToStep(3);
    return;
  }
  if (!validateBookingForm()) {
    goToStep(4, false);
    return;
  }

  const submitButton = document.getElementById('btn-submit-booking');
  const originalLabel = 'Confirmar reserva';
  bookingState.isSubmitting = true;
  submitButton.disabled = true;
  submitButton.setAttribute('aria-busy', 'true');
  submitButton.textContent = 'Enviando...';
  try {
    const response = await fetch('/api/public/appointments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        serviceId: bookingState.selectedServiceId,
        date: bookingState.selectedDate,
        startTime: bookingState.selectedTime,
        clientName: document.getElementById('client-name').value.trim(),
        clientPhone: document.getElementById('client-phone').value.replace(/\D/g, '')
      })
    });

    if (response.status === 201) {
      showSuccess(await response.json());
      return;
    }
    if (response.status === 409) {
      clearSelectedTime();
      await loadSlots(bookingState.selectedDate);
      showGlobalMessage('Esse horário acabou de ser reservado. Os horários foram atualizados; escolha outro.', 'error');
      return;
    }
    if (response.status === 429) {
      showGlobalMessage('Muitas tentativas em pouco tempo. Aguarde alguns instantes e tente novamente.', 'error');
      return;
    }

    const errorData = await response.json().catch(() => ({}));
    if (response.status >= 500) {
      showGlobalMessage('O serviço está temporariamente indisponível. Tente novamente em instantes.', 'error');
    } else {
      showGlobalMessage(errorData.detail || 'Não foi possível confirmar o agendamento. Confira os dados e tente novamente.', 'error');
    }
  } catch (error) {
    showGlobalMessage('Não foi possível conectar ao servidor. Verifique sua internet e tente novamente.', 'error');
  } finally {
    bookingState.isSubmitting = false;
    submitButton.disabled = false;
    submitButton.removeAttribute('aria-busy');
    submitButton.textContent = originalLabel;
  }
}

function showSuccess(data) {
  document.getElementById('booking-flow').classList.add('hidden');
  const successScreen = document.getElementById('success-screen');
  const serviceName = data.serviceName || bookingState.selectedServiceName;
  const appointmentDate = data.appointmentDate || bookingState.selectedDate;
  const startTime = data.startTime || bookingState.selectedTime;
  document.getElementById('res-service').textContent = serviceName;
  document.getElementById('res-price').textContent = formatPrice(data.servicePrice || bookingState.selectedServicePrice);
  document.getElementById('res-date').textContent = BarbershopUtils.formatDateDisplay(appointmentDate);
  document.getElementById('res-time').textContent = startTime.substring(0, 5);
  document.getElementById('res-name').textContent = data.clientName || document.getElementById('client-name').value.trim();
  bookingState.confirmedAppointment = {
    id: data.id,
    serviceName,
    appointmentDate,
    startTime
  };
  successScreen.classList.remove('hidden');
  successScreen.scrollIntoView({ behavior: getScrollBehavior(), block: 'start' });
  successScreen.querySelector('h2').focus({ preventScroll: true });
}

function openBarberWhatsApp() {
  const phone = BarbershopUtils.normalizeBrazilianPhone(window.BARBERSHOP_CONTACT?.barberWhatsApp);
  if (!phone) {
    showGlobalMessage('Configure o WhatsApp do barbeiro em js/contact-config.js para enviar a confirmação.', 'error');
    return;
  }
  const appointment = bookingState.confirmedAppointment;
  const message = `Olá! Agendei ${appointment.serviceName} para ${BarbershopUtils.formatDateDisplay(appointment.appointmentDate)} às ${appointment.startTime.substring(0, 5)}.`;
  window.open(`https://wa.me/${phone}?text=${encodeURIComponent(message)}`, '_blank', 'noopener');
}

function saveAppointmentToCalendar() {
  const appointment = bookingState.confirmedAppointment;
  if (!appointment) return;

  const start = new Date(`${appointment.appointmentDate}T${appointment.startTime}`);
  const end = new Date(start.getTime() + 30 * 60 * 1000);
  const toCalendarDate = (date) => [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
    'T',
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
    '00'
  ].join('');
  const escapeCalendarText = (value) => String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\n/g, '\\n')
    .replace(/,/g, '\\,')
    .replace(/;/g, '\\;');
  const calendar = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Barber Braga//Agendamento//PT-BR',
    'BEGIN:VEVENT',
    `UID:${appointment.id || `${appointment.appointmentDate}-${appointment.startTime}`}@barberclassic`,
    `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')}`,
    `DTSTART:${toCalendarDate(start)}`,
    `DTEND:${toCalendarDate(end)}`,
    `SUMMARY:${escapeCalendarText(`${window.BARBERSHOP_CONTENT?.brandName || 'Barber Braga'} - ${appointment.serviceName}`)}`,
    `DESCRIPTION:${escapeCalendarText(`Agendamento: ${appointment.serviceName}`)}`,
    'END:VEVENT',
    'END:VCALENDAR'
  ].join('\r\n');
  const file = new Blob([calendar], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(file);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'agendamento-barbearia.ics';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function resetBookingFlow() {
  document.getElementById('success-screen').classList.add('hidden');
  document.getElementById('booking-flow').classList.remove('hidden');
  document.getElementById('client-name').value = '';
  document.getElementById('client-phone').value = '';
  clearFieldError('name-error');
  clearFieldError('phone-error');
  bookingState.confirmedAppointment = null;
  clearSelectedTime();
  goToStep(1);
}

function goToStep(step, moveFocus = true) {
  bookingState.activeStep = step;
  document.querySelectorAll('.progress-step').forEach((item, index) => {
    const current = index + 1;
    item.classList.toggle('active', current === step);
    item.classList.toggle('complete', current < step);
    if (current === step) item.setAttribute('aria-current', 'step');
    else item.removeAttribute('aria-current');
  });
  const section = document.getElementById(`step-${step}`);
  section.scrollIntoView({ behavior: getScrollBehavior(), block: 'start' });
  if (moveFocus) {
    const heading = section.querySelector('h2, h3');
    heading?.focus({ preventScroll: true });
  }
}

function getScrollBehavior() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
}

function showGlobalMessage(message, type) {
  const element = document.getElementById('global-message');
  element.textContent = message;
  element.className = `feedback-box feedback-${type}`;
  element.setAttribute('role', type === 'error' ? 'alert' : 'status');
  element.scrollIntoView({ behavior: getScrollBehavior(), block: 'nearest' });
}

function clearGlobalMessage() {
  const element = document.getElementById('global-message');
  element.textContent = '';
  element.className = 'hidden';
}
