import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api', // Vite proxies this directly to http://localhost:8080/api
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;