import { api } from './client'

export const schedulesApi = {
  list: (from, to, opts) =>
    api.get(`/api/v1/schedules?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`, opts),
  getOne: (id) => api.get(`/api/v1/schedules/${id}`),
  create: (data) => api.post('/api/v1/schedules', data),
  update: (id, data) => api.patch(`/api/v1/schedules/${id}`, data),
  delete: (id) => api.delete(`/api/v1/schedules/${id}`),
}
