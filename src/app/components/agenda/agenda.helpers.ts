export function formatAgendaStatusLabel(status: string): string {
  return status
    .toLowerCase()
    .split('_')
    .map(fragment => fragment.charAt(0).toUpperCase() + fragment.slice(1))
    .join(' ');
}

export function getInitials(value: string): string {
  return (
    value
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(fragment => fragment.charAt(0).toUpperCase())
      .join(' ') || 'NA'
  );
}

export function getDurationMinutes(startIso: string, endIso: string): number {
  const start = new Date(startIso).getTime();
  const end = new Date(endIso).getTime();
  return Math.max(30, Math.round((end - start) / 60000));
}

export function buildWhatsAppUrl(phone?: string | null): string | null {
  const normalized = phone?.replace(/[^\d]/g, '') ?? '';
  if (!normalized) {
    return null;
  }

  return `https://wa.me/${normalized}`;
}
