import { api } from './client'

export const authApi = {
  register: (data) => api.post('/api/v1/auth/register', data),
  login: (data) => api.post('/api/v1/auth/login', data),
  logout: () => api.post('/api/v1/auth/logout', {}),
  me: () => api.get('/api/v1/auth/me'),
}
