import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { NoteEditor } from './NoteEditor';

describe('NoteEditor', () => {
  it('adds a note', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<NoteEditor note={null} onSave={onSave} />);
    await userEvent.click(screen.getByRole('button', { name: 'Add note' }));
    await userEvent.type(screen.getByLabelText('Note'), '  Felt great ');
    await userEvent.click(screen.getByRole('button', { name: 'Save note' }));
    expect(onSave).toHaveBeenCalledWith('Felt great');
  });

  it('keeps the draft when the note changes underneath it', async () => {
    const { rerender } = render(<NoteEditor note="First" onSave={vi.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Edit note' }));
    await userEvent.type(screen.getByLabelText('Note'), ' and more');
    rerender(<NoteEditor note="Refetched" onSave={vi.fn()} />);
    expect(screen.getByLabelText('Note')).toHaveValue('First and more');
  });
});
