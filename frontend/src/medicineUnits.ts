const wholeUnits = new Set(['lọ', 'viên', 'chai', 'tuýp', 'gói', 'ống', 'hộp', 'vỉ', 'liều', 'cái']);

export function isWholeMedicineUnit(unit?: string | null) {
  return wholeUnits.has(unit?.trim().toLocaleLowerCase('vi') ?? '');
}