import type { PageKey } from './permissions';

const symbols: Record<PageKey, string> = {
  services: '₫', dashboard: '▦', appointments: '▤', owners: '♙', pets: '🐾', vets: '✚',
  vaccinations: '✦', 'vaccine-types': '⊕', medicines: '▣', invoices: '▧',
  search: '⌕', notifications: '◷', knowledge: '▥', assistant: '✧',
  support: '?', users: '♧', audit: '≡',
};

export default function NavIcon({page}: {page: PageKey}) {
  return <span className="nav-icon" aria-hidden="true">{symbols[page]}</span>;
}
