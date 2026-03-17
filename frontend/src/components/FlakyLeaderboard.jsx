import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getFlakyTests } from '../api/flakespyApi'

export default function FlakyLeaderboard({ project }) {
  const [tests,   setTests]   = useState([])
  const [loading, setLoading] = useState(true)
  const [filter,  setFilter]  = useState('ALL')
  const navigate = useNavigate()

  useEffect(() => {
    getFlakyTests(project.id)
      .then(res => setTests(res.data))
      .catch(err => console.error('Failed to load flaky tests', err))
      .finally(() => setLoading(false))
  }, [project])

  const filtered = tests.filter(t => {
    if (filter === 'ALL')     return true
    if (filter === 'BROKEN')  return t.flakinessScore > 0.8
    if (filter === 'FLAKY')   return t.flakinessScore > 0.2 && t.flakinessScore <= 0.8
    if (filter === 'STABLE')  return t.flakinessScore <= 0.2
    return true
  })

  if (loading) return (
    <div style={styles.loading}>Analysing your test suite...</div>
  )

  return (
    <div style={styles.wrapper}>

      {/* Header */}
      <div style={styles.header}>
        <div>
          <h2 style={styles.title}>Flakiness leaderboard</h2>
          <p style={styles.subtitle}>
            {tests.length} tests tracked — click any row for full analysis
          </p>
        </div>

        {/* Filter pills */}
        <div style={styles.filters}>
          {['ALL', 'BROKEN', 'FLAKY', 'STABLE'].map(f => (
            <button
              key={f}
              style={filter === f ? styles.filterActive : styles.filter}
              onClick={() => setFilter(f)}
            >
              {filterEmoji(f)} {f}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      {filtered.length === 0 ? (
        <div style={styles.empty}>
          No tests in this category. 🎉
        </div>
      ) : (
        <div style={styles.table}>

          {/* Table header */}
          <div style={styles.tableHeader}>
            <span style={{ flex: 3 }}>Test name</span>
            <span style={{ flex: 2 }}>Class</span>
            <span style={{ flex: 1, textAlign: 'center' }}>Runs</span>
            <span style={{ flex: 1, textAlign: 'center' }}>Failures</span>
            <span style={{ flex: 1, textAlign: 'center' }}>Score</span>
            <span style={{ flex: 1, textAlign: 'center' }}>Status</span>
            <span style={{ flex: 1.5 }}>Root cause</span>
          </div>

          {/* Table rows */}
          {filtered.map((test, i) => (
            <div
              key={i}
              style={styles.row}
              onClick={() => navigate(
                `/test/${encodeURIComponent(test.testName)}?projectId=${project.id}`
              )}
            >
              <span style={{ ...styles.cell, flex: 3 }}>
                <span style={styles.testName}>{shortName(test.testName)}</span>
              </span>
              <span style={{ ...styles.cell, flex: 2 }}>
                <span style={styles.className}>{shortClass(test.className)}</span>
              </span>
              <span style={{ ...styles.cellCenter, flex: 1 }}>
                {test.totalRuns}
              </span>
              <span style={{ ...styles.cellCenter, flex: 1 }}>
                <span style={{ color: test.failures > 0 ? '#ef4444' : '#22c55e' }}>
                  {test.failures}
                </span>
              </span>
              <span style={{ ...styles.cellCenter, flex: 1 }}>
                <ScoreBar score={test.flakinessScore} />
              </span>
              <span style={{ ...styles.cellCenter, flex: 1 }}>
                <StatusBadge label={test.flakinessLabel} />
              </span>
              <span style={{ ...styles.cell, flex: 1.5 }}>
                <RootCauseBadge cause={test.rootCause} />
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function ScoreBar({ score }) {
  const pct   = Math.round(score * 100)
  const color = score > 0.8 ? '#ef4444'
              : score > 0.5 ? '#f59e0b'
              : score > 0.2 ? '#eab308'
              : '#22c55e'
  return (
    <div style={styles.scoreWrap}>
      <div style={styles.scoreTrack}>
        <div style={{ ...styles.scoreFill, width: `${pct}%`, backgroundColor: color }} />
      </div>
      <span style={{ ...styles.scorePct, color }}>{pct}%</span>
    </div>
  )
}

function StatusBadge({ label }) {
  const color = label === 'Broken'   ? { bg: '#450a0a', text: '#fca5a5' }
              : label === 'Flaky'    ? { bg: '#431407', text: '#fdba74' }
              : label === 'Unstable' ? { bg: '#422006', text: '#fde68a' }
              : { bg: '#052e16', text: '#86efac' }
  return (
    <span style={{ ...styles.badge, backgroundColor: color.bg, color: color.text }}>
      {label || 'Stable'}
    </span>
  )
}

function RootCauseBadge({ cause }) {
  if (!cause) return <span style={styles.noCause}>Pending AI analysis</span>
  const colors = {
    TIMING:          { bg: '#1e1b4b', text: '#a5b4fc' },
    LOCATOR:         { bg: '#1c1917', text: '#d6d3d1' },
    DATA_DEPENDENCY: { bg: '#042f2e', text: '#99f6e4' },
    NETWORK:         { bg: '#0c1a2e', text: '#93c5fd' },
    CONCURRENCY:     { bg: '#2d1b69', text: '#c4b5fd' },
    ENVIRONMENT:     { bg: '#1a2e05', text: '#86efac' },
  }
  const c = colors[cause] || { bg: '#1e293b', text: '#94a3b8' }
  return (
    <span style={{ ...styles.badge, backgroundColor: c.bg, color: c.text }}>
      {cause.replace('_', ' ')}
    </span>
  )
}

function filterEmoji(f) {
  if (f === 'BROKEN') return '🔴'
  if (f === 'FLAKY')  return '🟡'
  if (f === 'STABLE') return '🟢'
  return '📋'
}

function shortName(name) {
  if (!name) return ''
  return name.length > 35 ? name.substring(0, 35) + '...' : name
}

function shortClass(cls) {
  if (!cls) return ''
  const parts = cls.split('.')
  return parts[parts.length - 1]
}

const styles = {
  wrapper: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '16px',
    overflow:        'hidden',
  },
  header: {
    display:         'flex',
    justifyContent:  'space-between',
    alignItems:      'flex-start',
    padding:         '1.5rem 1.75rem',
    borderBottom:    '1px solid #334155',
    flexWrap:        'wrap',
    gap:             '1rem',
  },
  title: {
    fontSize:   '1.125rem',
    fontWeight: '600',
    color:      '#f1f5f9',
    margin:     0,
  },
  subtitle: {
    fontSize:   '0.8rem',
    color:      '#64748b',
    marginTop:  '0.25rem',
  },
  filters: {
    display: 'flex',
    gap:     '0.5rem',
  },
  filter: {
    backgroundColor: '#0f172a',
    border:          '1px solid #334155',
    borderRadius:    '20px',
    padding:         '0.375rem 0.875rem',
    color:           '#94a3b8',
    fontSize:        '0.8rem',
    fontWeight:      '500',
  },
  filterActive: {
    backgroundColor: '#1d4ed8',
    border:          '1px solid #2563eb',
    borderRadius:    '20px',
    padding:         '0.375rem 0.875rem',
    color:           '#ffffff',
    fontSize:        '0.8rem',
    fontWeight:      '500',
  },
  table: {
    display:       'flex',
    flexDirection: 'column',
  },
  tableHeader: {
    display:         'flex',
    padding:         '0.75rem 1.75rem',
    backgroundColor: '#0f172a',
    fontSize:        '0.75rem',
    fontWeight:      '600',
    color:           '#475569',
    textTransform:   'uppercase',
    letterSpacing:   '0.05em',
    gap:             '1rem',
  },
  row: {
    display:      'flex',
    padding:      '1rem 1.75rem',
    borderBottom: '1px solid #1e293b',
    cursor:       'pointer',
    transition:   'background 0.15s',
    gap:          '1rem',
    alignItems:   'center',
    backgroundColor: '#1e293b',
  },
  cell: {
    display:    'flex',
    alignItems: 'center',
    overflow:   'hidden',
  },
  cellCenter: {
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
  },
  testName: {
    color:     '#e2e8f0',
    fontSize:  '0.875rem',
    fontWeight:'500',
    overflow:  'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  className: {
    color:    '#64748b',
    fontSize: '0.8rem',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  scoreWrap: {
    display:    'flex',
    alignItems: 'center',
    gap:        '0.375rem',
    width:      '100%',
  },
  scoreTrack: {
    flex:            1,
    height:          '6px',
    backgroundColor: '#0f172a',
    borderRadius:    '3px',
    overflow:        'hidden',
  },
  scoreFill: {
    height:       '6px',
    borderRadius: '3px',
    transition:   'width 0.3s',
  },
  scorePct: {
    fontSize:  '0.75rem',
    fontWeight:'600',
    minWidth:  '32px',
  },
  badge: {
    padding:      '0.25rem 0.625rem',
    borderRadius: '20px',
    fontSize:     '0.75rem',
    fontWeight:   '500',
    whiteSpace:   'nowrap',
  },
  noCause: {
    color:    '#475569',
    fontSize: '0.75rem',
    fontStyle:'italic',
  },
  loading: {
    padding: '2rem',
    color:   '#64748b',
    textAlign: 'center',
  },
  empty: {
    padding:   '3rem',
    textAlign: 'center',
    color:     '#64748b',
    fontSize:  '0.9rem',
  },
}
