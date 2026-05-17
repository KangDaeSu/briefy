import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { authApi } from '../api/auth'
import { usersApi } from '../api/users'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const checkAuth = useCallback(async () => {
    try {
      const res = await authApi.me()
      setUser(res.data)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { checkAuth() }, [checkAuth])

  const login = useCallback(async (email, password) => {
    const res = await authApi.login({ email, password })
    setUser(res.data)
    return res.data
  }, [])

  const register = useCallback(async (email, name, password) => {
    const res = await authApi.register({ email, name, password })
    setUser(res.data)
    return res.data
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  const updateUser = useCallback(async (name) => {
    const res = await usersApi.updateMe({ name })
    setUser(res.data)
    return res.data
  }, [])

  const deleteAccount = useCallback(async () => {
    await usersApi.deleteMe()
    await authApi.logout()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser, deleteAccount }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
