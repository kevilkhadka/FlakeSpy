import { useState, useEffect } from 'react'
import { useParams, useSearchParams, useNavigate } from 'react-router-dom'
import { getTestAnalysis } from '../api/flakespyApi'
import TrendChart from '../components/TrendChart'

export default function TestDetail() {
  const { testName }          = useParams()
  const [searchParams]        = useSearchParams()
  const projectId             = searchParams.get('projectId')
  const navigate              = useNavigate()
  const [analysis, setAnalysis] = useState(null)
  const [loading,  setLoading]  = useState(true)
  const [error,    setError]    = useState(null)

  useEffect(() => {
    getTestAnalysis(decodeURIComponent(testName), projectId)
      .then(res => setAnalysis(res.data))
      .catch(() => setError('Failed to load test analysis.'))
      .finally(() => setLoading(false))
  }, [testName, projectId])

  if (loading) return (
    <div style={styles.centered}>
      <span style={styles.loadingText}>Running AI analysis...</span>
    </div>
  )

  if (error) return (
    <div style={styles.centered}>
      <span style={styles.errorText}>{error}</span>
    </div>
  )

  return (
    <div style={styles.page}>

      {/* Nav */}
      <nav style={styles.nav}>
        <button style={styles.backBtn} onClick={() => navigate('/')}>
          ← Back to dashboard
        </button>
        <span style={styles.navTitle}>🕵️ FlakeSpy</span>
      </nav>

      <main style={styles.main}>

        {/* Test header */}
        <div style={styles.testHeader}>
          <div>
            <h1 style={styles.testName}>{analysis.testName}</h1>
            <p style={styles.className}>{analysis.className}</p>
          </div>
          <StatusBadge label={analysis.flakinessLabel} score={analysis.flakinessScore} />
        </div>

        {/* Stats row */}
        <div style={styles.statsRow}>
          <StatCard label="Flakiness score" value={`${Math.round(analysis.flakinessScore * 100)}%`} color={scoreColor(analysis.flakinessScore)} />
          <StatCard label="Total runs"      value={analysis.totalRuns}  color="#94a3b8" />
          <StatCard label="Failures"        value={analysis.failures}   color="#ef4444" />
          <StatCard label="Passes"          value={analysis.passes}     color="#22c55e" />
        </div>

        {/* AI Analysis */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>🤖 AI root cause analysis</h2>

          <div style={styles.rootCauseRow}>
            <div style={styles.rootCauseBadge}>
              {analysis.rootCause || 'UNKNOWN'}
            </div>
            <p style={styles.rootCauseExplanation}>
              {analysis.rootCauseExplanation || 'Analysis pending — sync to trigger AI classification.'}
            </p>
          </div>

          {analysis.failurePattern && (
            <div style={styles.patternBox}>
              <span style={styles.patternLabel}>Failure pattern</span>
              <p style={styles.patternText}>{analysis.failurePattern}</p>
            </div>
          )}

          {analysis.fixSuggestion && (
            <div style={styles.fixBox}>
              <span style={styles.fixLabel}>🔧 Suggested fix</span>
              <p style={styles.fixText}>{analysis.fixSuggestion}</p>
            </div>
          )}
        </div>

        {/* Trend chart */}
        <TrendChart
          testName={decodeURIComponent(testName)}
          projectId={projectId}
        />

        {/* Most common failure */}
        {analysis.mostCommonFailureMessage && (
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>Most common failure message</h2>
            <pre style={styles.failurePre}>
              {analysis.mostCommonFailureMessage}
            </pre>
          </div>
        )}

      </main>
    </div>
  )
}

function StatCard({ label, value, color }) {
  return (
    <div style={styles.statCard}>
      <span style={{ ...styles.statValue, color }}>{value}</span>
      <span style={styles.statLabel}>{label}</span>
    </div>
  )
}

function StatusBadge({ label, score }) {
  const color = scoreColor(score)
  return (
    <div style={{ ...styles.statusBadge, borderColor: color }}>
      <span style={{ ...styles.statusScore, color }}>
        {Math.round(score * 100)}%
      </span>
      <span style={{ ...styles.statusLabel, color }}>
        {label || 'Stable'}
      </span>
    </div>
  )
}

function scoreColor(score) {
  if (score > 0.8) return '#ef4444'
  if (score > 0.5) return '#f59e0b'
  if (score > 0.2) return '#eab308'
  return '#22c55e'
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
  backBtn: {
    backgroundColor: 'transparent',
    border:          '1px solid #334155',
    borderRadius:    '8px',
    color:           '#94a3b8',
    padding:         '0.375rem 0.875rem',
    fontSize:        '0.875rem',
  },
  navTitle: {
    fontSize:   '1.25rem',
    fontWeight: '700',
    color:      '#f1f5f9',
  },
  main: {
    maxWidth:      '1000px',
    margin:        '0 auto',
    padding:       '2rem',
    display:       'flex',
    flexDirection: 'column',
    gap:           '1.5rem',
  },
  testHeader: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'flex-start',
    flexWrap:       'wrap',
    gap:            '1rem',
  },
  testName: {
    fontSize:   '1.5rem',
    fontWeight: '700',
    color:      '#f1f5f9',
    margin:     0,
    wordBreak:  'break-all',
  },
  className: {
    fontSize:  '0.875rem',
    color:     '#64748b',
    marginTop: '0.25rem',
  },
  statusBadge: {
    border:        '2px solid',
    borderRadius:  '12px',
    padding:       '0.75rem 1.25rem',
    display:       'flex',
    flexDirection: 'column',
    alignItems:    'center',
    gap:           '0.125rem',
    minWidth:      '80px',
  },
  statusScore: {
    fontSize:   '1.5rem',
    fontWeight: '700',
  },
  statusLabel: {
    fontSize:   '0.75rem',
    fontWeight: '500',
  },
  statsRow: {
    display:  'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap:      '1rem',
  },
  statCard: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '12px',
    padding:         '1.25rem',
    display:         'flex',
    flexDirection:   'column',
    alignItems:      'center',
    gap:             '0.375rem',
  },
  statValue: {
    fontSize:   '2rem',
    fontWeight: '700',
  },
  statLabel: {
    fontSize: '0.75rem',
    color:    '#64748b',
  },
  card: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '16px',
    padding:         '1.5rem 1.75rem',
    display:         'flex',
    flexDirection:   'column',
    gap:             '1rem',
  },
  cardTitle: {
    fontSize:   '1rem',
    fontWeight: '600',
    color:      '#f1f5f9',
    margin:     0,
  },
  rootCauseRow: {
    display:    'flex',
    alignItems: 'flex-start',
    gap:        '1rem',
    flexWrap:   'wrap',
  },
  rootCauseBadge: {
    backgroundColor: '#1d4ed8',
    color:           '#bfdbfe',
    padding:         '0.375rem 0.875rem',
    borderRadius:    '20px',
    fontSize:        '0.8rem',
    fontWeight:      '600',
    whiteSpace:      'nowrap',
  },
  rootCauseExplanation: {
    color:     '#94a3b8',
    fontSize:  '0.9rem',
    lineHeight:'1.6',
    margin:    0,
    flex:      1,
  },
  patternBox: {
    backgroundColor: '#0f172a',
    borderRadius:    '8px',
    padding:         '1rem',
    display:         'flex',
    flexDirection:   'column',
    gap:             '0.375rem',
  },
  patternLabel: {
    fontSize:   '0.75rem',
    fontWeight: '600',
    color:      '#475569',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  patternText: {
    color:     '#94a3b8',
    fontSize:  '0.875rem',
    lineHeight:'1.6',
    margin:    0,
  },
  fixBox: {
    backgroundColor: '#052e16',
    border:          '1px solid #166534',
    borderRadius:    '8px',
    padding:         '1rem',
    display:         'flex',
    flexDirection:   'column',
    gap:             '0.375rem',
  },
  fixLabel: {
    fontSize:   '0.75rem',
    fontWeight: '600',
    color:      '#16a34a',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  fixText: {
    color:     '#86efac',
    fontSize:  '0.875rem',
    lineHeight:'1.6',
    margin:    0,
  },
  failurePre: {
    backgroundColor: '#0f172a',
    borderRadius:    '8px',
    padding:         '1rem',
    color:           '#fca5a5',
    fontSize:        '0.8rem',
    overflowX:       'auto',
    whiteSpace:      'pre-wrap',
    wordBreak:       'break-all',
    margin:          0,
    lineHeight:      '1.6',
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
  errorText: {
    color:    '#ef4444',
    fontSize: '1rem',
  },
}
