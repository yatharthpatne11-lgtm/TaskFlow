import React from 'react';
import { ClipboardList, Clock } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { parseDate } from '../utils/dateUtils';
import QuickNoteEditor from './QuickNoteEditor';

export default function MyActionsList({ tasks, columns, onQuickNoteSaved }) {
  const { user } = useAuth();

  const myTasks = tasks
    .filter((t) => user && t.assigneeId != null && t.assigneeId === user.id)
    .sort((a, b) => {
      const da = parseDate(a.dueDate);
      const db = parseDate(b.dueDate);
      if (da && db) return da - db;
      if (da) return -1;
      if (db) return 1;
      return 0;
    });

  const statusOf = (task) => columns.find((c) => c.id === task.columnId)?.name || task.columnName;

  return (
    <section className="my-actions">
      <div className="my-actions-header">
        <h3>
          <ClipboardList size={20} /> My Actions
        </h3>
        <span className="task-count">{myTasks.length}</span>
      </div>
      <p className="text-muted my-actions-subtitle">Tasks assigned to you across this board.</p>

      {myTasks.length === 0 ? (
        <div className="empty-state">
          Nothing on your plate right now 🎉 — tasks assigned to you will show up here.
        </div>
      ) : (
        <div className="table-card">
          <table className="actions-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Task</th>
                <th>Priority</th>
                <th>Due Date</th>
                <th>Quick Note</th>
              </tr>
            </thead>
            <tbody>
              {myTasks.map((task) => {
                const due = parseDate(task.dueDate);
                const isOverdue = due && due < new Date() && task.columnName !== 'DONE';
                return (
                  <tr key={task.id}>
                    <td>
                      <span className="column-pill">{statusOf(task)}</span>
                    </td>
                    <td className="actions-title">{task.title}</td>
                    <td>
                      <span className={`priority-tag priority-${task.priority.toLowerCase()}`}>
                        {task.priority}
                      </span>
                    </td>
                    <td>
                      {due ? (
                        <span className={isOverdue ? 'due-date overdue' : 'due-date'}>
                          <Clock size={12} />
                          <span>{due.toLocaleDateString()}</span>
                        </span>
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </td>
                    <td className="actions-note-cell">
                      <QuickNoteEditor task={task} canEdit compact onSaved={onQuickNoteSaved} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
