// Ajuste estes textos somente depois de confirmar as informações com o barbeiro.
window.BARBERSHOP_CONTENT = Object.freeze({
  brandName: 'Barber Braga',
  heroTitle: 'Seu próximo corte começa aqui',
  heroDescription: 'Seu estilo merece atenção aos detalhes. Escolha o serviço, encontre o melhor horário e deixe o resto com a gente.',
  aboutDescription: 'Na Barber Braga, cada atendimento é feito com cuidado, técnica e atenção ao seu estilo. Estamos em Barra do Choça e deixamos o agendamento simples: você escolhe o serviço, encontra um horário e confirma sua visita online.',
  location: 'Barra do Choça / BA',
  businessHours: 'Consulte os horários disponíveis no agendamento.',
  footerNote: 'Serviços e horários sujeitos à disponibilidade.',
  instagramLabel: 'Instagram @barberbraga_',
  instagramUrl: 'https://instagram.com/barberbraga_'
});

document.querySelectorAll('[data-content]').forEach((element) => {
  const key = element.dataset.content;
  const value = window.BARBERSHOP_CONTENT[key];
  if (typeof value === 'string') element.textContent = value;
});

document.querySelectorAll('[data-content-href]').forEach((element) => {
  const key = element.dataset.contentHref;
  const value = window.BARBERSHOP_CONTENT[key];
  if (typeof value === 'string') element.href = value;
});

document.title = `${window.BARBERSHOP_CONTENT.brandName} — Agendamento`;
