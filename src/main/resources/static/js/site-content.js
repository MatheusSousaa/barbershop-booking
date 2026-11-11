// Ajuste estes textos somente depois de confirmar as informações com o barbeiro.
window.BARBERSHOP_CONTENT = Object.freeze({
  brandName: 'Barber Braga',
  heroTitle: 'Seu próximo corte começa aqui',
  heroDescription: 'Consulte os serviços, veja as imagens disponíveis e escolha um horário para seu atendimento.',
  aboutDescription: 'Na Barber Braga, em Barra do Choça, seu atendimento começa com praticidade: escolha o serviço, confira os horários disponíveis e faça seu agendamento online. Um cuidado de cada vez, no horário que funciona para você.',
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
