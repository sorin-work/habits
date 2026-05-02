import { useState } from 'react';

interface NoteEditorProps {
  note: string | null;
  disabled?: boolean;
  onSave: (note: string | null) => Promise<unknown>;
}

/** Shows a day's note and lets the user add or change it. */
export function NoteEditor({ note, disabled, onSave }: NoteEditorProps) {
  const [editing, setEditing] = useState(false);
  // The draft is only taken from props when editing starts, so a background refetch
  // can't wipe out what the user is typing.
  const [draft, setDraft] = useState('');
  const [saving, setSaving] = useState(false);

  if (!editing) {
    return (
      <div className="note">
        {note && <p className="note-text">{note}</p>}
        <button
          type="button"
          className="link-button"
          disabled={disabled}
          onClick={() => {
            setDraft(note ?? '');
            setEditing(true);
          }}
        >
          {note ? 'Edit note' : 'Add note'}
        </button>
      </div>
    );
  }

  const save = async () => {
    setSaving(true);
    try {
      await onSave(draft.trim() || null);
      setEditing(false);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="note editing">
      <textarea
        aria-label="Note"
        value={draft}
        maxLength={500}
        rows={2}
        autoFocus
        onChange={(e) => setDraft(e.target.value)}
      />
      <div className="note-actions">
        <button type="button" className="button small primary" disabled={saving} onClick={() => void save()}>
          Save note
        </button>
        <button type="button" className="button small" onClick={() => setEditing(false)}>
          Cancel
        </button>
      </div>
    </div>
  );
}
