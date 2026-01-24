export function percent(rate: number | null | undefined): string {
  if (rate == null) return '–';
  return `${Math.round(rate * 100)}%`;
}

export function plural(count: number, singular: string, pluralForm = `${singular}s`) {
  return `${count} ${count === 1 ? singular : pluralForm}`;
}
