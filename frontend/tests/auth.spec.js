import { test, expect } from '@playwright/test'

const unique = () => `e2e-${Date.now()}@briefy.test`

test.describe('Auth', () => {
  test('register then see calendar', async ({ page }) => {
    // /register 는 별도 페이지
    await page.goto('/register')

    const email = unique()
    await page.getByLabel(/이메일/i).fill(email)
    await page.getByLabel(/이름/i).fill('E2E Tester')
    // 비밀번호 라벨에 "8자 이상" 힌트가 붙어있으므로 exact:false
    await page.getByLabel(/비밀번호/i).fill('pass1234')
    await page.getByRole('button', { name: /회원가입/i }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText(/\d{4}년/)).toBeVisible()
  })

  test('login with valid credentials', async ({ page }) => {
    const email = unique()

    // 회원가입
    await page.goto('/register')
    await page.getByLabel(/이메일/i).fill(email)
    await page.getByLabel(/이름/i).fill('Login Tester')
    await page.getByLabel(/비밀번호/i).fill('pass1234')
    await page.getByRole('button', { name: /회원가입/i }).click()
    await expect(page).toHaveURL('/')

    // 로그아웃 (nav에 있는 버튼)
    await page.getByRole('button', { name: /로그아웃/i }).click()
    await expect(page).toHaveURL('/login')

    // 로그인
    await page.getByLabel(/이메일/i).fill(email)
    await page.getByLabel(/비밀번호/i).fill('pass1234')
    await page.getByRole('button', { name: /^로그인$/i }).click()
    await expect(page).toHaveURL('/')
  })

  test('wrong password shows error', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel(/이메일/i).fill('nobody@briefy.test')
    await page.getByLabel(/비밀번호/i).fill('wrongpass')
    await page.getByRole('button', { name: /^로그인$/i }).click()

    // 에러는 <p class="auth-error"> 로 표시됨
    await expect(page.locator('.auth-error')).toBeVisible()
  })

  test('unauthenticated redirect to login', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL('/login')
  })

  test('register link from login page', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /회원가입/i }).click()
    await expect(page).toHaveURL('/register')
    await expect(page.locator('.auth-title')).toBeVisible()
  })
})
