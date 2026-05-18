const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

function getToken() {
  return localStorage.getItem('jwt')
}

export function setToken(token) {
  if (token) localStorage.setItem('jwt', token)
  else localStorage.removeItem('jwt')
}

async function request(path, options = {}) {
  const { headers: extraHeaders, ...rest } = options
  const hasBody = rest.body !== undefined
  const token = getToken()
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      ...(hasBody && { 'Content-Type': 'application/json' }),
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...extraHeaders,
    },
    ...rest,
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw Object.assign(new Error(err.message ?? res.statusText), { status: res.status, body: err })
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  get: (path, opts) => request(path, opts),
  post: (path, body, opts) => request(path, { method: 'POST', body: JSON.stringify(body), ...opts }),
  patch: (path, body, opts) => request(path, { method: 'PATCH', body: JSON.stringify(body), ...opts }),
  delete: (path, opts) => request(path, { method: 'DELETE', ...opts }),
}
