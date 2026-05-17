const KEY = 'briefy_accounts'

export function getSavedAccounts() {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? '[]')
  } catch {
    return []
  }
}

export function saveAccount({ email, name }) {
  const accounts = getSavedAccounts().filter(a => a.email !== email)
  accounts.unshift({ email, name })
  localStorage.setItem(KEY, JSON.stringify(accounts.slice(0, 5)))
}

export function removeAccount(email) {
  const accounts = getSavedAccounts().filter(a => a.email !== email)
  localStorage.setItem(KEY, JSON.stringify(accounts))
}
