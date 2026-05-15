import { api } from './client'

// Phase 3 인증 구현 전 임시 사용자 ID
const TEST_USER_ID = '11111111-1111-1111-1111-111111111111'

const headers = { 'X-User-Id': TEST_USER_ID }

export const schedulesApi = {
  /** 날짜 범위 내 이벤트 목록 (반복 일정 확장 포함) */
  list: (from, to) =>
    api.get(`/api/v1/schedules?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`, { headers }),

  /** 단일 일정 상세 */
  getOne: (id) =>
    api.get(`/api/v1/schedules/${id}`, { headers }),

  /** 일정 생성 */
  create: (data) =>
    api.post('/api/v1/schedules', data, { headers }),

  /** 일정 수정 */
  update: (id, data) =>
    api.patch(`/api/v1/schedules/${id}`, data, { headers }),

  /** 일정 삭제 */
  delete: (id) =>
    api.delete(`/api/v1/schedules/${id}`, { headers }),
}
