import { useState, useEffect } from 'react'
import { getProjects, disconnectProject } from '../api/flakespyApi'
import GitLabConnect from '../components/GitLabConnect'
import SuiteHealthBadge from '../components/SuiteHealthBadge'
import FlakyLeaderboard from '../components/FlakyLeaderboard'

export default function Dashboard() {
  const [project,  setProject]  = useState(null)
  const [loading,  setLoading]  = useState(true)

  useEffect(() => {
    getProjects()
      .then(res => {
        if (res.data.length > 0) setProject(res.data[0])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  async function handleDisconnect() {
    if (!window.confirm('Disconnect this project?')) return
    try {
      await disconnectProject(project.id)
      setProject(null)
    } catch (err) {
      console.error('Failed to disconnect', err)
    }
  }

  if (loading) return (
    <div style={styles.centered}>
      <span style={styles.loadingText}>Loading FlakeSpy...</span>
    </div>
  )

  if (!project) return (
    <GitLabConnect onConnected={setProject} />
  )

  return (
    <div style={styles.page}>

      {/* Top nav */}
      <nav style={styles.nav}>
        <div style={styles.navBrand}>
          <span style={styles.navIcon}>🕵️</span>
          <span style={styles.navTitle}>FlakeSpy</span>
        </div>
        <div style={styles.navRight}>
          <span style={styles.navProject}>
            {project.projectName}
          </span>
          <button
            style={styles.disconnectBtn}
            onClick={handleDisconnect}
          >
            Disconnect
          </button>
        </div>
      </nav>

      {/* Main content */}
      <main style={styles.main}>

        {/* Suite health */}
        <section style={styles.section}>
          <SuiteHealthBadge project={project} />
        </section>

        {/* Leaderboard */}
        <section style={styles.section}>
          <FlakyLeaderboard project={project} />
        </section>

      </main>

    </div>
  )
}

const styles = {
  page: {
    minHeight:       '100vh',
    backgroundColor: '#0f172a',
  },
  nav: {
    display:         'flex',
    justifyContent:  'space-between',
    alignItems:      'center',
    padding:         '1rem 2rem',
    backgroundColor: '#1e293b',
    borderBottom:    '1px solid #334155',
    position:        'sticky',
    top:             0,
    zIndex:          10,
  },
  navBrand: {
    display:    'flex',
    alignItems: 'center',
    gap:        '0.625rem',
  },
  navIcon: {
    fontSize: '1.5rem',
  },
  navTitle: {
    fontSize:   '1.25rem',
    fontWeight: '700',
    color:      '#f1f5f9',
  },
  navRight: {
    display:    'flex',
    alignItems: 'center',
    gap:        '1rem',
  },
  navProject: {
    fontSize:        '0.875rem',
    color:           '#94a3b8',
    backgroundColor: '#0f172a',
    padding:         '0.375rem 0.875rem',
    borderRadius:    '20px',
    border:          '1px solid #334155',
  },
  disconnectBtn: {
    backgroundColor: 'transparent',
    border:          '1px solid #334155',
    borderRadius:    '8px',
    color:           '#64748b',
    padding:         '0.375rem 0.875rem',
    fontSize:        '0.8rem',
  },
  main: {
    maxWidth: '1200px',
    margin:   '0 auto',
    padding:  '2rem',
    display:  'flex',
    flexDirection: 'column',
    gap:      '1.5rem',
  },
  section: {
    width: '100%',
  },
  centered: {
    minHeight:      '100vh',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
  },
  loadingText: {
    color:    '#64748b',
    fontSize: '1rem',
  },
}
