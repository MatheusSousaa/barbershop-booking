window.BarbershopUtils = (() => {
  const timeZone = 'America/Bahia';

  function getBahiaDateString(date = new Date()) {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).formatToParts(date);
    const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
    return `${values.year}-${values.month}-${values.day}`;
  }

  function normalizeBrazilianPhone(phone) {
    let digits = String(phone || '').replace(/\D/g, '');
    if ((digits.length === 12 || digits.length === 13) && digits.startsWith('55')) {
      digits = digits.substring(2);
    }
    return digits.length === 10 || digits.length === 11 ? `55${digits}` : '';
  }

  function formatDateDisplay(value) {
    if (!value) return '';
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  return Object.freeze({
    timeZone,
    getBahiaDateString,
    normalizeBrazilianPhone,
    formatDateDisplay
  });
})();
