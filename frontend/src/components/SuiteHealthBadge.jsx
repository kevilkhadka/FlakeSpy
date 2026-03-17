import { useState, useEffect } from 'react'
import { getDashboardSummary, syncNow } from '../api/flakespyApi'

export default function SuiteHealthBadge({ project }) {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)

  useEffect(() => {
    fetchSummary()
  }, [project])

  async function fetchSummary() {
    try {
      const res = await getDashboardSummary(project.id)
      setSummary(res.data)
    } catch (err) {
      console.error('Failed to load summary', err)
    } finally {
      setLoading(false)
    }
  }

  async function handleSyncNow() {
    setSyncing(true)
    try {
      await syncNow(project.id)
      await fetchSummary()
    } catch (err) {
      console.error('Sync failed', err)
    } finally {
      setSyncing(false)
    }
  }

  if (loading) return <div style={styles.loading}>Loading suite health...</div>

  if (!summary || summary.totalTests === 0) return (
    <div style={styles.empty}>
      <p style={styles.emptyText}>
        No test runs found yet. FlakeSpy syncs automatically at 6am —
        or click Sync Now to pull data immediately.
      </p>
      <button
        style={syncing ? styles.btnDisabled : styles.btn}
        onClick={handleSyncNow}
        disabled={syncing}
      >
        {syncing ? 'Syncing...' : '🔄 Sync Now'}
      </button>
    </div>
  )

  const { healthScore, healthLabel } = summary
  const color  = healthColor(healthScore)
  const emoji  = healthEmoji(healthScore)

  return (
    <div style={styles.wrapper}>

      {/* Left — score ring */}
      <div style={styles.scoreSection}>
        <div style={{ ...styles.scoreBadge, borderColor: color }}>
          <span style={{ ...styles.scoreNumber, color }}>
            {healthScore}
          </span>
          <span style={styles.scoreMax}>/100</span>
        </div>
        <div style={styles.scoreLabel}>
          <span style={{ fontSize: '1.25rem' }}>{emoji}</span>
          <span style={{ ...styles.healthLabel, color }}>
            {healthLabel}
          </span>
        </div>
      </div>

      {/* Middle — breakdown */}
      <div style={styles.breakdown}>
        <div style={styles.breakdownTitle}>
          {project.projectName}
        </div>
        <div style={styles.stats}>
          <Stat value={summary.totalStableTests} label="stable"  color="#22c55e" />
          <Stat value={summary.totalFlakyTests}  label="flaky"   color="#f59e0b" />
          <Stat value={summary.totalBrokenTests} label="broken"  color="#ef4444" />
        </div>
        <div style={styles.runStats}>
          <span style={styles.runStat}>
            {summary.totalTests} tests
          </span>
          <span style={styles.dot}>·</span>
          <span style={styles.runStat}>
            {summary.passed} passed
          </span>
          <span style={styles.dot}>·</span>
          <span style={styles.runStat}>
            {summary.failed} failed
          </span>
        </div>
      </div>

      {/* Right — sync info */}
      <div style={styles.syncSection}>
        <div style={styles.syncTime}>
          Last sync
          <span style={styles.syncDate}>
            {formatDate(summary.lastSyncedAt)}
          </span>
        </div>
        <div style={styles.syncTime}>
          Last run
          <span style={styles.syncDate}>
            {formatDate(summary.lastRunDate)}
          </span>
        </div>
        <button
          style={syncing ? styles.btnDisabled : styles.btn}
          onClick={handleSyncNow}
          disabled={syncing}
        >
          {syncing ? 'Syncing...' : '🔄 Sync Now'}
        </button>
      </div>

    </div>
  )
}

function Stat({ value, label, color }) {
  return (
    <div style={styles.stat}>
      <span style={{ ...styles.statValue, color }}>{value}</span>
      <span style={styles.statLabel}>{label}</span>
    </div>
  )
}

function healthColor(score) {
  if (score >= 80) return '#22c55e'
  if (score >= 50) return '#f59e0b'
  return '#ef4444'
}

function healthEmoji(score) {
  if (score >= 80) return '🟢'
  if (score >= 50) return '🟡'
  return '🔴'
}

function formatDate(dateStr) {
  if (!dateStr) return 'Never'
  const d = new Date(dateStr)
  return d.toLocaleString('en-US', {
    month:  'short',
    day:    'numeric',
    hour:   '2-digit',
    minute: '2-digit'
  })
}

const styles = {
  wrapper: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '16px',
    padding:         '1.75rem 2rem',
    display:         'flex',
    alignItems:      'center',
    gap:             '2rem',
    flexWrap:        'wrap',
  },
  scoreSection: {
    display:        'flex',
    flexDirection:  'column',
    alignItems:     'center',
    gap:            '0.5rem',
  },
  scoreBadge: {
    width:        '90px',
    height:       '90px',
    borderRadius: '50%',
    border:       '4px solid',
    display:      'flex',
    alignItems:   'center',
    justifyContent:'center',
    flexDirection: 'column',
  },
  scoreNumber: {
    fontSize:   '2rem',
    fontWeight: '700',
    lineHeight: '1',
  },
  scoreMax: {
    fontSize: '0.75rem',
    color:    '#64748b',
  },
  scoreLabel: {
    display:    'flex',
    alignItems: 'center',
    gap:        '0.375rem',
  },
  healthLabel: {
    fontWeight: '600',
    fontSize:   '1rem',
  },
  breakdown: {
    flex:          '1',
    display:       'flex',
    flexDirection: 'column',
    gap:           '0.75rem',
  },
  breakdownTitle: {
    fontSize:   '1.125rem',
    fontWeight: '600',
    color:      '#f1f5f9',
  },
  stats: {
    display: 'flex',
    gap:     '1.5rem',
  },
  stat: {
    display:       'flex',
    flexDirection: 'column',
    alignItems:    'center',
    gap:           '0.125rem',
  },
  statValue: {
    fontSize:   '1.5rem',
    fontWeight: '700',
  },
  statLabel: {
    fontSize: '0.75rem',
    color:    '#64748b',
  },
  runStats: {
    display:    'flex',
    alignItems: 'center',
    gap:        '0.5rem',
  },
  runStat: {
    fontSize: '0.8rem',
    color:    '#94a3b8',
  },
  dot: {
    color: '#475569',
  },
  syncSection: {
    display:       'flex',
    flexDirection: 'column',
    alignItems:    'flex-end',
    gap:           '0.5rem',
  },
  syncTime: {
    display:       'flex',
    flexDirection: 'column',
    alignItems:    'flex-end',
    fontSize:      '0.75rem',
    color:         '#64748b',
    gap:           '0.125rem',
  },
  syncDate: {
    color:      '#94a3b8',
    fontWeight: '500',
  },
  btn: {
    backgroundColor: '#1d4ed8',
    color:           '#ffffff',
    border:          'none',
    borderRadius:    '8px',
    padding:         '0.5rem 1rem',
    fontSize:        '0.875rem',
    fontWeight:      '500',
    marginTop:       '0.25rem',
  },
  btnDisabled: {
    backgroundColor: '#1e3a5f',
    color:           '#475569',
    border:          'none',
    borderRadius:    '8px',
    padding:         '0.5rem 1rem',
    fontSize:        '0.875rem',
    fontWeight:      '500',
    marginTop:       '0.25rem',
  },
  loading: {
    color:   '#64748b',
    padding: '1rem',
  },
  empty: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '16px',
    padding:         '1.75rem 2rem',
    display:         'flex',
    alignItems:      'center',
    justifyContent:  'space-between',
    gap:             '1.5rem',
    flexWrap:        'wrap',
  },
  emptyText: {
    color:    '#94a3b8',
    fontSize: '0.9rem',
    maxWidth: '500px',
  },
}
