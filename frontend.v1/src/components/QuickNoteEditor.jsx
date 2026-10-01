import React, { useState, useEffect } from 'react';
import { Lock, Pencil, StickyNote } from 'lucide-react';
import { updateQuickNote } from '../services/taskApi';

const MAX_LENGTH = 1000;

/**
 * Shared Quick Note UI used by TaskCard and MyActionsList.
 * - canEdit === true  -> assignee: "Edit Note" button + textarea (saved via PATCH /api/tasks/:id/quick-note)
 * - canEdit === false -> everyone else: read-only note with a lock icon
 */
export default function QuickNoteEditor({ task, canEdit, onSaved, onEditingChange, compact = false }) {
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(task.quickNote || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Keep the draft in sync when the note changes elsewhere (e.g. edited from the other view)
  useEffect(() => {
    if (!isEditing) setDraft(task.quickNote || '');
  }, [task.quickNote, isEditing]);

  const changeEditing = (value) => {
    setIsEditing(value);
    if (onEditingChange) onEditingChange(value);
  };

  const startEditing = () => {
    setDraft(task.quickNote || '');
    setError('');
    changeEditing(true);
  };

  const cancelEditing = () => {
    setDraft(task.quickNote || '');
    setError('');
    changeEditing(false);
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      const res = await updateQuickNote(task.id, draft);
      if (res.success) {
        if (onSaved) onSaved(res.data);
        changeEditing(false);
      } else {
        setError(res.message || 'Failed to save note');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save note');
    } finally {
      setSaving(false);
    }
  };

  // Keep clicks / drags inside the note from opening the task modal or dragging the card
  const isolate = (e) => e.stopPropagation();
  const blockDrag = (e) => { e.preventDefault(); e.stopPropagation(); };

  const hasNote = !!(task.quickNote && task.quickNote.trim());

  return (
    <div
      className={compact ? 'quick-note quick-note-compact' : 'quick-note'}
      onClick={isolate}
      onDragStart={blockDrag}
      draggable={false}
    >
      {!compact && (
        <div className="quick-note-header">
          <span className="quick-note-label">
            <StickyNote size={12} /> Quick Note
          </span>
          {!canEdit && (
            <span className="quick-note-lock" title="Only the assignee can modify this note">
              <Lock size={12} />
            </span>
          )}
        </div>
      )}

      {canEdit && isEditing ? (
        <>
          <textarea
            className="quick-note-textarea"
            rows={compact ? 2 : 3}
            maxLength={MAX_LENGTH}
            value={draft}
            autoFocus
            placeholder="Write a private update for this task..."
            onChange={(e) => setDraft(e.target.value)}
            disabled={saving}
          />
          {error && <p className="quick-note-error">{error}</p>}
          <div className="quick-note-actions">
            <button type="button" className="btn-secondary btn-sm" onClick={cancelEditing} disabled={saving}>
              Cancel
            </button>
            <button type="button" className="btn-primary btn-sm" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </>
      ) : (
        <>
          <p className={hasNote ? 'quick-note-text' : 'quick-note-text quick-note-empty'}>
            {hasNote ? task.quickNote : canEdit ? 'No note yet.' : 'No note from the assignee yet.'}
          </p>
          {canEdit ? (
            <button type="button" className="quick-note-edit-btn" onClick={startEditing}>
              <Pencil size={12} /> Edit Note
            </button>
          ) : (
            <p className="quick-note-hint">
              <Lock size={11} /> Only the assignee can modify this note
            </p>
          )}
          {error && <p className="quick-note-error">{error}</p>}
        </>
      )}
    </div>
  );
}