import { test, expect } from '@playwright/test'

const unique = () => `cal-${Date.now()}@briefy.test`

async function registerAndLogin(page) {
  const email = unique()
  await page.goto('/register')
  await page.getByLabel(/이메일/i).fill(email)
  await page.getByLabel(/이름/i).fill('Cal Tester')
  await page.getByLabel(/비밀번호/i).fill('pass1234')
  await page.getByRole('button', { name: /회원가입/i }).click()
  await expect(page).toHaveURL('/')
  return email
}

async function openNewScheduleModal(page) {
  await page.getByRole('button', { name: /일정 추가/i }).click()
  await expect(page.locator('.modal-box')).toBeVisible()
}

async function fillScheduleForm(page, { title, dateStr, startHour = '10', endHour = '11' }) {
  await page.getByPlaceholder(/일정 제목/i).fill(title)
  await page.locator('input[type="datetime-local"]').first().fill(`${dateStr}T${startHour}:00`)
  await page.locator('input[type="datetime-local"]').last().fill(`${dateStr}T${endHour}:00`)
}

function todayStr() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  // 내일 날짜 사용 (UTC 범위 경계 이슈 방지)
  d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

test.describe('Calendar', () => {
  test('calendar page loads with month header', async ({ page }) => {
    await registerAndLogin(page)
    await expect(page.locator('.cal-page')).toBeVisible()
    // "2026년 5월" 형식의 헤더
    await expect(page.locator('h1')).toContainText(/년/)
  })

  test('create a schedule and see it in day panel', async ({ page }) => {
    await registerAndLogin(page)
    await openNewScheduleModal(page)

    const dateStr = todayStr()
    await fillScheduleForm(page, { title: 'E2E 생성 테스트', dateStr })
    await page.getByRole('button', { name: /^생성$/i }).click()
    await expect(page.locator('.modal-box')).not.toBeVisible()

    // 해당 날짜 셀을 클릭해 day-panel 열기
    const day = new Date(dateStr).getDate()
    await page.locator('.cal-cell').filter({ hasText: String(day) }).first().click()
    await expect(page.locator('.day-panel')).toBeVisible()
    await expect(page.locator('.day-event__title').filter({ hasText: 'E2E 생성 테스트' })).toBeVisible()
  })

  test('edit a schedule', async ({ page }) => {
    await registerAndLogin(page)
    await openNewScheduleModal(page)

    const dateStr = todayStr()
    await fillScheduleForm(page, { title: '수정 전 제목', dateStr, startHour: '14', endHour: '15' })
    await page.getByRole('button', { name: /^생성$/i }).click()
    await expect(page.locator('.modal-box')).not.toBeVisible()

    // 날짜 클릭해 day-panel 열기
    const day = new Date(dateStr).getDate()
    await page.locator('.cal-cell').filter({ hasText: String(day) }).first().click()

    // 이벤트 클릭해 수정 모달 열기
    await page.locator('.day-event').filter({ hasText: '수정 전 제목' }).click()
    await expect(page.locator('.modal-box h2')).toContainText('일정 수정')

    await page.getByPlaceholder(/일정 제목/i).clear()
    await page.getByPlaceholder(/일정 제목/i).fill('수정 후 제목')
    await page.getByRole('button', { name: /^수정$/i }).click()
    await expect(page.locator('.modal-box')).not.toBeVisible()

    await expect(page.locator('.day-event__title').filter({ hasText: '수정 후 제목' })).toBeVisible()
  })

  test('delete a schedule', async ({ page }) => {
    await registerAndLogin(page)
    await openNewScheduleModal(page)

    const dateStr = todayStr()
    await fillScheduleForm(page, { title: '삭제할 일정', dateStr, startHour: '16', endHour: '17' })
    await page.getByRole('button', { name: /^생성$/i }).click()
    await expect(page.locator('.modal-box')).not.toBeVisible()

    const day = new Date(dateStr).getDate()
    await page.locator('.cal-cell').filter({ hasText: String(day) }).first().click()
    await page.locator('.day-event').filter({ hasText: '삭제할 일정' }).click()

    // confirm 다이얼로그 수락 후 삭제
    page.on('dialog', d => d.accept())
    await page.getByRole('button', { name: /^삭제$/i }).click()
    await expect(page.locator('.modal-box')).not.toBeVisible()

    await expect(page.locator('.day-event__title').filter({ hasText: '삭제할 일정' })).not.toBeVisible()
  })

  test('navigate months forward and back', async ({ page }) => {
    await registerAndLogin(page)

    // .cal-page__nav h1 로 특정 (AppNav 등 다른 h1과 구별)
    const monthHeader = page.locator('.cal-page__nav h1')
    const initialText = await monthHeader.textContent()
    await page.locator('.nav-btn').last().click() // 다음달 (›)
    const nextText = await monthHeader.textContent()
    expect(nextText).not.toEqual(initialText)

    await page.locator('.nav-btn').first().click() // 이전달 (‹)
    const backText = await monthHeader.textContent()
    expect(backText).toEqual(initialText)
  })

  test('settings page - update name', async ({ page }) => {
    await registerAndLogin(page)

    // 설정 페이지로 이동
    await page.locator('.app-nav__name').click()
    await expect(page).toHaveURL('/settings')
    await expect(page.locator('.settings-title')).toBeVisible()

    // 이름 수정
    await page.locator('#name-input').clear()
    await page.locator('#name-input').fill('Updated Name')
    await page.getByRole('button', { name: /^저장$/i }).click()

    // "저장됨" 상태 확인
    await expect(page.getByRole('button', { name: /저장됨/i })).toBeVisible()
  })

  test('settings page - delete account', async ({ page }) => {
    await registerAndLogin(page)

    await page.locator('.app-nav__name').click()
    await expect(page).toHaveURL('/settings')

    // 계정 삭제 flow: idle → confirm → deleting → login
    await page.getByRole('button', { name: /^계정 삭제$/i }).click()
    await expect(page.locator('.settings-confirm-box')).toBeVisible()

    await page.getByRole('button', { name: /삭제 확인/i }).click()
    await expect(page).toHaveURL('/login')
  })
})
