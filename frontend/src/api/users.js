import { api } from './client'

export const usersApi = {
  getMe: () => api.get('/api/v1/users/me'),
  updateMe: (data) => api.patch('/api/v1/users/me', data),
  deleteMe: () => api.delete('/api/v1/users/me'),
}
