const animalIcons: Record<string, string> = {
  cho: '🐶', dog: '🐶', canine: '🐶',
  meo: '🐱', cat: '🐱', feline: '🐱',
  chim: '🐦', bird: '🐦', vet: '🦜', parrot: '🦜',
  tho: '🐰', rabbit: '🐰', hamster: '🐹',
  chuot: '🐭', mouse: '🐭', rat: '🐭',
  rua: '🐢', turtle: '🐢', ca: '🐟', fish: '🐟',
};

export default function PatientIdentity({name, species, detail}: {
  name: string; species?: string | null; detail?: string | null;
}) {
  const key = (species || '').toLowerCase().normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').trim();
  return <span className="patient-identity">
    <span className="animal-avatar" aria-hidden="true">{animalIcons[key] || '🐾'}</span>
    <span className="patient-text"><strong>{name}</strong>
      {(detail || species) && <small>{detail || species}</small>}
    </span>
  </span>;
}
