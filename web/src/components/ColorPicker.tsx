export const HABIT_COLORS = [
  '#4f7cff',
  '#3fa7d6',
  '#26a69a',
  '#2bb673',
  '#9ccc65',
  '#f2a541',
  '#ef6c57',
  '#d4598e',
  '#8e6cef',
  '#5c6bc0',
];

export function ColorPicker({ value, onChange }: { value: string; onChange: (color: string) => void }) {
  return (
    <div className="color-picker" role="radiogroup" aria-label="Colour">
      {HABIT_COLORS.map((color) => (
        <button
          key={color}
          type="button"
          role="radio"
          aria-checked={value.toLowerCase() === color}
          aria-label={color}
          className="swatch"
          style={{ background: color }}
          onClick={() => onChange(color)}
        />
      ))}
    </div>
  );
}
