import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const config = resolve(scriptDir, '..', 'application-local.properties')
const properties = Object.fromEntries(
  readFileSync(config, 'utf8')
    .split(/\r?\n/)
    .filter((line) => line.trim() && !line.trim().startsWith('#') && line.includes('='))
    .map((line) => {
      const splitAt = line.indexOf('=')
      return [line.slice(0, splitAt).trim(), line.slice(splitAt + 1).trim()]
    }),
)

const username = process.env.FIXFLOW_ADMIN_USERNAME || properties['fixflow.admin.username']
const password = process.env.FIXFLOW_ADMIN_PASSWORD || properties['fixflow.admin.password']
if (!username || !password) throw new Error('Admin credentials are missing from local configuration.')

const baseUrl = (process.env.FIXFLOW_API_URL || 'http://localhost:8080').replace(/\/$/, '')
let token

async function api(path, { method = 'GET', body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (!response.ok) {
    const message = await response.text()
    throw new Error(`FixFlow API ${method} ${path} returned ${response.status}: ${message}`)
  }
  return response.status === 204 ? null : response.json()
}

function dateFromToday(offset) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

try {
  const auth = await api('/api/auth/login', {
    method: 'POST',
    body: { username, password },
  })
  if (auth.user.role !== 'ADMIN') throw new Error('The configured login must have the ADMIN role.')
  token = auth.token

  const tickets = await api('/api/tickets')
  if (tickets.some((ticket) => ticket.customerName?.startsWith('DEMO-PHASE2-'))) {
    console.log('Phase 2 demo tickets already exist; no records were added.')
  } else {
    let technicians = await api('/api/technicians')
    let technician = technicians.find((item) => item.name === 'DEMO-PHASE2 Technician')
    if (!technician) {
      technician = await api('/api/technicians', {
        method: 'POST',
        body: {
          name: 'DEMO-PHASE2 Technician',
          email: 'demo-technician@example.test',
          phone: '9000000100',
          specialty: 'General repair',
          skills: 'Diagnostics, screen replacement, battery replacement',
          availability: 'AVAILABLE',
          active: true,
        },
      })
    }

    const samples = [
      ['Asha Moreno', 'Pixel 8', 'RECEIVED', 'MEDIUM', 5, 0],
      ['Ravi Shah', 'MacBook Air', 'DIAGNOSED', 'HIGH', 2, 85],
      ['Mina Patel', 'Galaxy S23', 'IN_REPAIR', 'HIGH', -1, 120],
      ['Dev Kumar', 'iPad Air', 'IN_REPAIR', 'MEDIUM', 1, 65],
      ['Lina Joseph', 'ThinkPad T14', 'QUALITY_CHECK', 'LOW', 4, 45],
      ['Omar Lee', 'Nintendo Switch', 'READY', 'MEDIUM', 0, 70],
      ['Sara Nair', 'iPhone 14', 'DELIVERED', 'LOW', -2, 95],
      ['Noah Das', 'Surface Laptop', 'CANCELLED', 'LOW', -3, 0],
    ]

    const createdTickets = []
    for (const [name, device, status, priority, dueOffset, cost] of samples) {
      const ticket = await api('/api/tickets', {
        method: 'POST',
        body: {
          customerName: `DEMO-PHASE2-${name}`,
          customerPhone: '9000000000',
          customerEmail: 'phase2-demo@example.test',
          deviceName: `DEMO-PHASE2 ${device}`,
          problemDescription: 'DEMO DATA: fictional repair record for the Phase 2 software demonstration.',
          status,
          priority,
          targetCompletionDate: dateFromToday(dueOffset),
          estimatedCost: cost,
        },
      })
      await api(`/api/tickets/${ticket.id}/technician?technicianId=${technician.id}`, { method: 'PATCH' })
      createdTickets.push(ticket)
    }

    const parts = [
      { name: 'DEMO-PHASE2 Display Assembly', sku: 'DEMO-P2-DISPLAY', quantity: 2, reorderThreshold: 5, description: 'Fictional demo display assembly', unit: 'each' },
      { name: 'DEMO-PHASE2 Battery Pack', sku: 'DEMO-P2-BATTERY', quantity: 12, reorderThreshold: 3, description: 'Fictional demo battery', unit: 'each' },
      { name: 'DEMO-PHASE2 USB-C Port', sku: 'DEMO-P2-USBC', quantity: 4, reorderThreshold: 4, description: 'Fictional demo charging port', unit: 'each' },
    ]
    for (const part of parts) await api('/api/inventory', { method: 'POST', body: part })

    const stats = await api('/api/tickets/stats')
    const lowStock = await api('/api/inventory/low-stock')
    console.log(`Added ${createdTickets.length} fictional demo tickets and ${parts.length} parts.`)
    console.log(`Current ticket total: ${stats.total}; in repair: ${stats.byStatus.IN_REPAIR}; low-stock parts: ${lowStock.length}.`)
  }
} finally {
  if (token) {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch {
      // A failed logout does not undo the sample records; the access token expires.
    }
  }
}
