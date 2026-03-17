import { useState } from 'react'
import { connectGitLab } from '../api/flakespyApi'

export default function GitLabConnect({ onConnected }) {
  const [form, setForm] = useState({
    projectId:           '',
    projectName:         '',
    personalAccessToken: '',
    jobName:             '',
    gitlabUrl:           'https://gitlab.com'
  })
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await connectGitLab(form)
      onConnected(res.data)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to connect. Check your details.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.wrapper}>
      <div style={styles.card}>

        <div style={styles.header}>
          <span style={styles.icon}>🕵️</span>
          <h1 style={styles.title}>FlakeSpy</h1>
          <p style={styles.subtitle}>
            Connect your GitLab project to start detecting flaky tests
          </p>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>

          <div style={styles.field}>
            <label style={styles.label}>GitLab project ID</label>
            <input
              style={styles.input}
              name="projectId"
              value={form.projectId}
              onChange={handleChange}
              placeholder="e.g. 12345678"
              required
            />
            <span style={styles.hint}>
              Found in GitLab → Settings → General
            </span>
          </div>

          <div style={styles.field}>
            <label style={styles.label}>Project name</label>
            <input
              style={styles.input}
              name="projectName"
              value={form.projectName}
              onChange={handleChange}
              placeholder="e.g. my-mobile-app"
              required
            />
          </div>

          <div style={styles.field}>
            <label style={styles.label}>Personal access token</label>
            <input
              style={styles.input}
              name="personalAccessToken"
              type="password"
              value={form.personalAccessToken}
              onChange={handleChange}
              placeholder="glpat-xxxxxxxxxxxxxxxxxxxx"
              required
            />
            <span style={styles.hint}>
              GitLab → Settings → Access Tokens → read_api scope
            </span>
          </div>

          <div style={styles.field}>
            <label style={styles.label}>CI job name</label>
            <input
              style={styles.input}
              name="jobName"
              value={form.jobName}
              onChange={handleChange}
              placeholder="e.g. run-tests"
              required
            />
            <span style={styles.hint}>
              The job name in your .gitlab-ci.yml that produces XML artifacts
            </span>
          </div>

          <div style={styles.field}>
            <label style={styles.label}>GitLab URL</label>
            <input
              style={styles.input}
              name="gitlabUrl"
              value={form.gitlabUrl}
              onChange={handleChange}
              placeholder="https://gitlab.com"
            />
            <span style={styles.hint}>
              Change this if you use a self-hosted GitLab
            </span>
          </div>

          {error && (
            <div style={styles.error}>{error}</div>
          )}

          <button
            type="submit"
            style={loading ? styles.btnDisabled : styles.btn}
            disabled={loading}
          >
            {loading ? 'Connecting & syncing...' : 'Connect project'}
          </button>

        </form>

        <div style={styles.footer}>
          <span style={styles.footerIcon}>🔒</span>
          <span style={styles.footerText}>
            Read-only token. FlakeSpy never pushes to your repository.
          </span>
        </div>

      </div>
    </div>
  )
}

const styles = {
  wrapper: {
    minHeight:       '100vh',
    display:         'flex',
    alignItems:      'center',
    justifyContent:  'center',
    padding:         '2rem',
    backgroundColor: '#0f172a',
  },
  card: {
    backgroundColor: '#1e293b',
    borderRadius:    '16px',
    padding:         '2.5rem',
    width:           '100%',
    maxWidth:        '520px',
    border:          '1px solid #334155',
  },
  header: {
    textAlign:    'center',
    marginBottom: '2rem',
  },
  icon: {
    fontSize: '3rem',
  },
  title: {
    fontSize:   '2rem',
    fontWeight: '700',
    color:      '#f1f5f9',
    margin:     '0.5rem 0 0.25rem',
  },
  subtitle: {
    color:    '#94a3b8',
    fontSize: '0.95rem',
  },
  form: {
    display:       'flex',
    flexDirection: 'column',
    gap:           '1.25rem',
  },
  field: {
    display:       'flex',
    flexDirection: 'column',
    gap:           '0.375rem',
  },
  label: {
    fontSize:   '0.875rem',
    fontWeight: '500',
    color:      '#cbd5e1',
  },
  input: {
    backgroundColor: '#0f172a',
    border:          '1px solid #334155',
    borderRadius:    '8px',
    padding:         '0.625rem 0.875rem',
    color:           '#f1f5f9',
    fontSize:        '0.95rem',
    outline:         'none',
  },
  hint: {
    fontSize: '0.75rem',
    color:    '#64748b',
  },
  error: {
    backgroundColor: '#450a0a',
    border:          '1px solid #991b1b',
    borderRadius:    '8px',
    padding:         '0.75rem',
    color:           '#fca5a5',
    fontSize:        '0.875rem',
  },
  btn: {
    backgroundColor: '#3b82f6',
    color:           '#ffffff',
    border:          'none',
    borderRadius:    '8px',
    padding:         '0.75rem',
    fontSize:        '1rem',
    fontWeight:      '600',
    marginTop:       '0.5rem',
    transition:      'background 0.2s',
  },
  btnDisabled: {
    backgroundColor: '#1e3a5f',
    color:           '#64748b',
    border:          'none',
    borderRadius:    '8px',
    padding:         '0.75rem',
    fontSize:        '1rem',
    fontWeight:      '600',
    marginTop:       '0.5rem',
  },
  footer: {
    display:        'flex',
    alignItems:     'center',
    gap:            '0.5rem',
    marginTop:      '1.5rem',
    justifyContent: 'center',
  },
  footerIcon: {
    fontSize: '0.875rem',
  },
  footerText: {
    fontSize: '0.75rem',
    color:    '#475569',
  },
}
