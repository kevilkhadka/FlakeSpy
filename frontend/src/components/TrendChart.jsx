import { useState, useEffect } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine
} from 'recharts'
import { getTestHistory } from '../api/flakespyApi'

export default function TrendChart({ testName, projectId }) {
  const [data,    setData]    = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!testName || !projectId) return
    getTestHistory(testName, projectId)
      .then(res => setData(formatData(res.data)))
      .catch(err => console.error('Failed to load history', err))
      .finally(() => setLoading(false))
  }, [testName, projectId])

  if (loading) return (
    <div style={styles.loading}>Loading 30-night history...</div>
  )

  if (data.length === 0) return (
    <div style={styles.empty}>No history data available yet.</div>
  )

  return (
    <div style={styles.wrapper}>
      <div style={styles.header}>
        <h3 style={styles.title}>30-night pass/fail history</h3>
        <div style={styles.legend}>
          <span style={styles.legendItem}>
            <span style={{ ...styles.dot, backgroundColor: '#22c55e' }} />
            Passed
          </span>
          <span style={styles.legendItem}>
            <span style={{ ...styles.dot, backgroundColor: '#ef4444' }} />
            Failed
          </span>
          <span style={styles.legendItem}>
            <span style={{ ...styles.dot, backgroundColor: '#64748b' }} />
            Skipped
          </span>
        </div>
      </div>

      {/* Status timeline — coloured dots per night */}
      <div style={styles.timeline}>
        {data.map((d, i) => (
          <div key={i} style={styles.nightWrap}>
            <div
              style={{
                ...styles.nightDot,
                backgroundColor: dotColor(d.status)
              }}
              title={`${d.date} — ${d.status}`}
            />
            <span style={styles.nightLabel}>{d.shortDate}</span>
          </div>
        ))}
      </div>

      {/* Duration trend line chart */}
      <div style={styles.chartWrap}>
        <p style={styles.chartLabel}>Test duration over time (seconds)</p>
        <ResponsiveContainer width="100%" height={180}>
          <LineChart data={data} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis
              dataKey="shortDate"
              tick={{ fill: '#475569', fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fill: '#475569', fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#1e293b',
                border:          '1px solid #334155',
                borderRadius:    '8px',
                color:           '#e2e8f0',
                fontSize:        '0.8rem',
              }}
              formatter={(value) => [`${value}s`, 'Duration']}
            />
            <ReferenceLine
              y={avgDuration(data)}
              stroke="#334155"
              strokeDasharray="4 4"
              label={{ value: 'avg', fill: '#475569', fontSize: 10 }}
            />
            <Line
              type="monotone"
              dataKey="duration"
              stroke="#3b82f6"
              strokeWidth={2}
              dot={(props) => <CustomDot {...props} />}
              activeDot={{ r: 6, fill: '#3b82f6' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

// Custom dot — red if failed, green if passed
function CustomDot({ cx, cy, payload }) {
  const color = dotColor(payload.status)
  return (
    <circle
      cx={cx}
      cy={cy}
      r={4}
      fill={color}
      stroke="#0f172a"
      strokeWidth={1.5}
    />
  )
}

function dotColor(status) {
  if (status === 'PASSED')  return '#22c55e'
  if (status === 'FAILED')  return '#ef4444'
  return '#64748b'
}

function formatData(raw) {
  return raw.map(r => ({
    date:      r.date,
    shortDate: formatShortDate(r.date),
    status:    r.status,
    duration:  Math.round(r.duration * 10) / 10,
    failure:   r.failureMessage,
  }))
}

function formatShortDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function avgDuration(data) {
  if (!data.length) return 0
  const sum = data.reduce((acc, d) => acc + (d.duration || 0), 0)
  return Math.round((sum / data.length) * 10) / 10
}

const styles = {
  wrapper: {
    backgroundColor: '#1e293b',
    border:          '1px solid #334155',
    borderRadius:    '16px',
    padding:         '1.5rem 1.75rem',
    display:         'flex',
    flexDirection:   'column',
    gap:             '1.25rem',
  },
  header: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    flexWrap:       'wrap',
    gap:            '0.75rem',
  },
  title: {
    fontSize:   '1rem',
    fontWeight: '600',
    color:      '#f1f5f9',
    margin:     0,
  },
  legend: {
    display: 'flex',
    gap:     '1rem',
  },
  legendItem: {
    display:    'flex',
    alignItems: 'center',
    gap:        '0.375rem',
    fontSize:   '0.75rem',
    color:      '#94a3b8',
  },
  dot: {
    width:        '8px',
    height:       '8px',
    borderRadius: '50%',
    display:      'inline-block',
  },
  timeline: {
    display:  'flex',
    gap:      '0.375rem',
    flexWrap: 'wrap',
  },
  nightWrap: {
    display:       'flex',
    flexDirection: 'column',
    alignItems:    'center',
    gap:           '0.25rem',
  },
  nightDot: {
    width:        '14px',
    height:       '14px',
    borderRadius: '50%',
  },
  nightLabel: {
    fontSize: '0.6rem',
    color:    '#475569',
  },
  chartWrap: {
    display:       'flex',
    flexDirection: 'column',
    gap:           '0.5rem',
  },
  chartLabel: {
    fontSize: '0.75rem',
    color:    '#64748b',
    margin:   0,
  },
  loading: {
    padding:   '2rem',
    color:     '#64748b',
    textAlign: 'center',
  },
  empty: {
    padding:   '2rem',
    color:     '#64748b',
    textAlign: 'center',
    fontSize:  '0.9rem',
  },
}
