import React, { useState } from 'react';
import { X } from 'lucide-react';
import { createNewTask } from '../services/taskApi';

export default function TaskModal({ boardId, task, onClose, onTaskSaved }) {
  const [title, setTitle] = useState(task ? task.title : '');
  const [description, setDescription] = useState(task ? task.description : '');
  const [priority, setPriority] = useState(task ? task.priority : 'MEDIUM');
  const [columnId, setColumnId] = useState(task ? task.columnId : 1);
  const [estimatedEffort, setEstimatedEffort] = useState(task ? task.estimatedEffortHours : 2.0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await createNewTask({
        title,
        description,
        boardId: parseInt(boardId),
        columnId: parseInt(columnId),
        priority,
        estimatedEffortHours: parseFloat(estimatedEffort),
        position: 0
      });
      onTaskSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create task');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-card">
        <div className="modal-header">
          <h3>{task ? 'Task Details' : 'Create New Task'}</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Title</label>
            <input type="text" required value={title} onChange={(e) => setTitle(e.target.value)} disabled={!!task} />
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea rows="3" value={description} onChange={(e) => setDescription(e.target.value)} disabled={!!task} />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value)} disabled={!!task}>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div className="form-group">
              <label>Column</label>
              <select value={columnId} onChange={(e) => setColumnId(e.target.value)} disabled={!!task}>
                <option value={1}>BACKLOG</option>
                <option value={2}>TO DO</option>
                <option value={3}>IN PROGRESS</option>
                <option value={4}>IN REVIEW</option>
                <option value={5}>DONE</option>
              </select>
            </div>
          </div>

          {!task && (
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={loading}>{loading ? 'Saving...' : 'Save Task'}</button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
