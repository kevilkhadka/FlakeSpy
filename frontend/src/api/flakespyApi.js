import axios from 'axios'

const BASE_URL = 'http://localhost:8080/api'

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' }
})

// GitLab
export const connectGitLab    = (data)      => api.post('/gitlab/connect', data)
export const syncNow          = (id)        => api.post(`/gitlab/sync/${id}`)
export const getProjects      = ()          => api.get('/gitlab/projects')
export const disconnectProject= (id)        => api.delete(`/gitlab/projects/${id}`)

// Tests
export const getFlakyTests    = (projectId) => api.get('/tests/flaky', { params: { projectId } })
export const getTestHistory   = (testName, projectId) =>
  api.get(`/tests/${encodeURIComponent(testName)}/history`, { params: { projectId } })
export const getTestAnalysis  = (testName, projectId) =>
  api.get(`/tests/${encodeURIComponent(testName)}/analysis`, { params: { projectId } })

// Dashboard
export const getDashboardSummary = (projectId) =>
  api.get('/dashboard/summary', { params: { projectId } })
