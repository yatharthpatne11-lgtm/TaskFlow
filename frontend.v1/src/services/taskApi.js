import apiClient from './apiClient';

export const fetchBoardTasks = async (boardId) => {
  const response = await apiClient.get(`/tasks?boardId=${boardId}`);
  return response.data;
};

export const createNewTask = async (taskData) => {
  const response = await apiClient.post('/tasks', taskData);
  return response.data;
};

export const updateTaskPosition = async (taskData) => {
  const response = await apiClient.put('/tasks', taskData);
  return response.data;
};

export const deleteTaskById = async (taskId) => {
  const response = await apiClient.delete(`/tasks/${taskId}`);
  return response.data;
};