import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import { useAuth } from '../context/AuthContext'

// ===== Icons (inline, cartographic style) =====
const IconChunk = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 5a2 2 0 012-2h12a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm4 2h8M8 11h8M8 15h5" />
  </svg>
)
const IconReplicate = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
  </svg>
)
const IconShield = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
  </svg>
)
const IconHistory = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
)
const IconTrash = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
  </svg>
)
const IconKey = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
  </svg>
)
const IconUser = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
  </svg>
)
const IconLogout = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
  </svg>
)
const IconSettings = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
  </svg>
)
const IconGauge = ({ className = 'h-5 w-5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
  </svg>
)

// ===== Hero network diagram =====
const NODES = [
  { id: 1, x: 12, y: 62, label: 'Node-1', chunks: 4 },
  { id: 2, x: 50, y: 18, label: 'Node-2', chunks: 3 },
  { id: 3, x: 88, y: 40, label: 'Node-3', chunks: 5 },
  { id: 4, x: 34, y: 30, label: 'Node-4', chunks: 2 },
  { id: 5, x: 70, y: 72, label: 'Node-5', chunks: 6 },
]

const EDGES = [
  [0, 1], [1, 2], [2, 3], [3, 4], [4, 0], [1, 4], [0, 3],
]

function HeroNetwork() {
  const reduceMotion = useReducedMotion()
  const [hovered, setHovered] = useState(null)
  const [packets, setPackets] = useState([])
  const svgRef = useRef(null)

  // Generate packet positions along edges, animated via CSS
  useEffect(() => {
    if (reduceMotion) return
    const generated = EDGES.map(([a, b], i) => {
      const from = NODES[a]
      const to = NODES[b]
      return {
        id: i,
        from,
        to,
        delay: (i * 0.7) % 4,
        duration: 3 + (i % 3),
      }
    })
    setPackets(generated)
  }, [reduceMotion])

  return (
    <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-navy p-2 shadow-2xl">
      {/* Surveyor grid backdrop */}
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.07]"
        style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,0.6) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.6) 1px, transparent 1px)',
          backgroundSize: '28px 28px',
        }}
      />
      <svg
        ref={svgRef}
        viewBox="0 0 100 100"
        className="relative h-auto w-full"
        role="img"
        aria-label="Animated diagram of storage nodes connected in a network, with file chunks flowing between them"
      >
        {/* Edges */}
        {EDGES.map(([a, b], i) => {
          const from = NODES[a]
          const to = NODES[b]
          return (
            <line
              key={`edge-${i}`}
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
              stroke="rgba(176,118,59,0.35)"
              strokeWidth="0.4"
              strokeDasharray="1.5 1.5"
            />
          )
        })}

        {/* Packets (file chunks) flowing along edges */}
        {!reduceMotion &&
          packets.map((p) => (
            <motion.circle
              key={`packet-${p.id}`}
              r="0.9"
              fill="#B0763B"
              initial={{ cx: p.from.x, cy: p.from.y, opacity: 0 }}
              animate={{
                cx: [p.from.x, p.to.x],
                cy: [p.from.y, p.to.y],
                opacity: [0, 1, 1, 0],
              }}
              transition={{
                duration: p.duration,
                delay: p.delay,
                repeat: Infinity,
                repeatType: 'loop',
                ease: 'easeInOut',
              }}
            />
          ))}

        {/* Nodes */}
        {NODES.map((n) => {
          const isHovered = hovered === n.id
          return (
            <g
              key={n.id}
              onMouseEnter={() => setHovered(n.id)}
              onMouseLeave={() => setHovered(null)}
              className="cursor-pointer"
            >
              <circle
                cx={n.x}
                cy={n.y}
                r={isHovered ? 4.5 : 3.5}
                fill={isHovered ? '#B0763B' : '#1D2939'}
                stroke="#B0763B"
                strokeWidth="0.8"
                className="transition-all duration-200"
              />
              <circle
                cx={n.x}
                cy={n.y}
                r="1.2"
                fill="#B0763B"
                className={isHovered ? 'opacity-100' : 'opacity-60'}
              />
            </g>
          )
        })}
      </svg>

      {/* Tooltip */}
      <AnimatePresence>
        {hovered && (
          <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            className="pointer-events-none absolute z-10 rounded-lg border border-white/10 bg-navy-deep px-3 py-2 text-xs shadow-xl"
            style={{
              left: `${NODES.find((n) => n.id === hovered).x}%`,
              top: `${NODES.find((n) => n.id === hovered).y}%`,
              transform: 'translate(-50%, -130%)',
            }}
          >
            <p className="font-medium text-white">
              {NODES.find((n) => n.id === hovered).label}
            </p>
            <p className="mt-0.5 flex items-center gap-1.5 text-gray-300">
              <span className="h-1.5 w-1.5 rounded-full bg-pine" />
              Healthy · {NODES.find((n) => n.id === hovered).chunks} chunks
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Legend */}
      <div className="relative flex items-center gap-4 border-t border-white/10 px-4 py-2.5 text-[11px] text-gray-400">
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-brass" /> Storage node
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-1 w-3 rounded-full bg-brass/50" /> Replication link
        </span>
        <span className="ml-auto hidden sm:inline">Live · chunks in transit</span>
      </div>
    </div>
  )
}

// ===== Feature card =====
const FEATURES = [
  {
    icon: IconChunk,
    title: 'Chunked Storage',
    desc: 'Every file is split into 8 MB chunks before it touches disk. Large files stream in pieces instead of one fragile blob.',
  },
  {
    icon: IconReplicate,
    title: 'Replication & Fault Tolerance',
    desc: 'Each chunk is copied to two separate nodes. If one node goes down, your data is still served from the other.',
  },
  {
    icon: IconShield,
    title: 'Secure Sharing',
    desc: 'Share files and folders with password-protected links that expire. Downloads are signed and time-limited.',
  },
  {
    icon: IconHistory,
    title: 'Version History',
    desc: 'Upload a file with the same name and the previous version is kept. Restore any earlier copy at any time.',
  },
  {
    icon: IconTrash,
    title: 'Recycle Bin',
    desc: 'Deleted files sit in a recycle bin for 30 days before permanent removal — accidental deletes are recoverable.',
  },
  {
    icon: IconKey,
    title: 'JWT Authentication',
    desc: 'Short-lived access tokens with rotating refresh tokens. Passwords are hashed with BCrypt, never stored in plain text.',
  },
]

// ===== How it works steps =====
const STEPS = [
  {
    title: 'Upload',
    desc: 'You upload a file. Atlas records its metadata and prepares it for distribution.',
  },
  {
    title: 'Split into 8 MB chunks',
    desc: 'The file is divided into fixed 8 MB chunks, each with its own SHA-256 checksum for integrity.',
  },
  {
    title: 'Replicate ×2 across nodes',
    desc: 'Every chunk is written to two different storage nodes, chosen by available capacity.',
  },
  {
    title: 'Health sweeps',
    desc: 'A background scheduler continuously checks each node. A node that stops responding is marked DOWN.',
  },
  {
    title: 'Automatic recovery',
    desc: 'When a node recovers, the scheduler re-replicates any missing chunks so the cluster returns to full redundancy.',
  },
]

// ===== Node status teaser =====
const TEASER_NODES = [
  { name: 'Node-1', status: 'healthy', chunks: 4, used: 62 },
  { name: 'Node-2', status: 'healthy', chunks: 3, used: 48 },
  { name: 'Node-3', status: 'healthy', chunks: 5, used: 71 },
]

// ===== Demo modal (staged animation) =====
function DemoModal({ open, onClose }) {
  const reduceMotion = useReducedMotion()
  const [step, setStep] = useState(0)
  const modalRef = useRef(null)
  const closeBtnRef = useRef(null)

  const STAGES = [
    { label: 'Uploading file', detail: 'report.pdf · 24 MB', icon: 'upload' },
    { label: 'Splitting into chunks', detail: '3 × 8 MB chunks', icon: 'chunk' },
    { label: 'Replicating across nodes', detail: 'Chunk 1 → Node-1 & Node-3', icon: 'replicate' },
    { label: 'Node-2 goes DOWN', detail: 'Health sweep detects failure', icon: 'down' },
    { label: 'Recovery in progress', detail: 'Re-replicating to Node-4', icon: 'recover' },
    { label: 'Cluster healthy again', detail: 'All chunks at ×2 redundancy', icon: 'ok' },
  ]

  // Focus trap + Escape
  useEffect(() => {
    if (!open) return
    const prevFocus = document.activeElement
    closeBtnRef.current?.focus()

    const handleKey = (e) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'Tab') {
        const focusables = modalRef.current?.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        )
        if (!focusables || focusables.length === 0) return
        const first = focusables[0]
        const last = focusables[focusables.length - 1]
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault()
          last.focus()
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault()
          first.focus()
        }
      }
    }
    document.addEventListener('keydown', handleKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleKey)
      document.body.style.overflow = ''
      prevFocus?.focus()
    }
  }, [open, onClose])

  // Auto-advance through stages
  useEffect(() => {
    if (!open || reduceMotion) return
    if (step >= STAGES.length - 1) return
    const t = setTimeout(() => setStep((s) => s + 1), 1400)
    return () => clearTimeout(t)
  }, [open, step, reduceMotion, STAGES.length])

  if (!open) return null

  const current = STAGES[step]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-label="Live demo: upload and recovery flow"
        className="relative w-full max-w-lg rounded-2xl border border-line bg-surface p-6 shadow-2xl dark:border-gray-700 dark:bg-navy-surface"
      >
        <div className="flex items-center justify-between">
          <h3 className="font-display text-xl font-semibold text-ink dark:text-gray-100">
            Live demo
          </h3>
          <button
            ref={closeBtnRef}
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate hover:bg-canvas dark:hover:bg-navy-deep"
            aria-label="Close demo"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <p className="mt-1 text-sm text-slate dark:text-gray-400">
          A staged walkthrough of upload, failure, and recovery.
        </p>

        {/* Stage visual */}
        <div className="mt-6 flex h-40 items-center justify-center rounded-xl border border-line bg-canvas dark:border-gray-700 dark:bg-navy-deep">
          <AnimatePresence mode="wait">
            <motion.div
              key={step}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.25 }}
              className="text-center"
            >
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-brass/15 text-brass">
                {current.icon === 'upload' && <IconChunk className="h-6 w-6" />}
                {current.icon === 'chunk' && <IconReplicate className="h-6 w-6" />}
                {current.icon === 'replicate' && <IconReplicate className="h-6 w-6" />}
                {current.icon === 'down' && <IconTrash className="h-6 w-6 text-brick" />}
                {current.icon === 'recover' && <IconHistory className="h-6 w-6" />}
                {current.icon === 'ok' && (
                  <svg className="h-6 w-6 text-pine" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                )}
              </div>
              <p className="font-medium text-ink dark:text-gray-100">{current.label}</p>
              <p className="mt-0.5 text-sm text-slate dark:text-gray-400">{current.detail}</p>
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Progress dots */}
        <div className="mt-5 flex items-center justify-center gap-1.5">
          {STAGES.map((_, i) => (
            <button
              key={i}
              onClick={() => setStep(i)}
              aria-label={`Go to stage ${i + 1}`}
              className={`h-2 rounded-full transition-all ${
                i === step ? 'w-6 bg-brass' : i < step ? 'w-2 bg-brass/50' : 'w-2 bg-line dark:bg-gray-700'
              }`}
            />
          ))}
        </div>

        <div className="mt-6 flex items-center justify-between">
          <button
            onClick={() => setStep((s) => Math.max(0, s - 1))}
            disabled={step === 0}
            className="btn-secondary disabled:opacity-40"
          >
            Back
          </button>
          <button
            onClick={() => setStep((s) => Math.min(STAGES.length - 1, s + 1))}
            disabled={step === STAGES.length - 1}
            className="btn-primary disabled:opacity-40"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  )
}

// ===== Avatar dropdown (static preview) =====
function AvatarDropdown() {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    const handleClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    const handleEsc = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    document.addEventListener('keydown', handleEsc)
    return () => {
      document.removeEventListener('mousedown', handleClick)
      document.removeEventListener('keydown', handleEsc)
    }
  }, [])

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex h-9 w-9 items-center justify-center rounded-full bg-brass/15 text-sm font-semibold text-brass transition-colors hover:bg-brass/25"
        aria-label="Account menu (preview)"
        aria-expanded={open}
      >
        JD
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 6 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 top-12 z-20 w-64 rounded-xl border border-line bg-surface p-2 shadow-xl dark:border-gray-700 dark:bg-navy-surface"
          >
            <div className="flex items-center gap-3 border-b border-line px-3 py-2.5 dark:border-gray-700">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brass/15 text-sm font-semibold text-brass">
                JD
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-ink dark:text-gray-100">John Doe</p>
                <p className="truncate text-xs text-slate dark:text-gray-400">@johndoe92</p>
              </div>
            </div>

            {/* Storage usage preview */}
            <div className="px-3 py-3">
              <div className="mb-1 flex items-center justify-between text-xs">
                <span className="text-slate dark:text-gray-400">Storage</span>
                <span className="font-medium text-ink dark:text-gray-100">2.4 GB / 10 GB</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-line dark:bg-gray-700">
                <div className="h-full rounded-full bg-brass" style={{ width: '24%' }} />
              </div>
            </div>

            <div className="border-t border-line pt-1 dark:border-gray-700">
              {[
                { label: 'Profile', icon: IconUser },
                { label: 'Settings', icon: IconSettings },
              ].map((item) => (
                <button
                  key={item.label}
                  className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm text-ink transition-colors hover:bg-canvas dark:text-gray-200 dark:hover:bg-navy-deep"
                >
                  <item.icon className="h-4 w-4 text-slate dark:text-gray-400" />
                  {item.label}
                </button>
              ))}
              <button className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm text-brick transition-colors hover:bg-brick/10">
                <IconLogout className="h-4 w-4" />
                Log out
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

// ===== Scroll reveal wrapper =====
function Reveal({ children, delay = 0, className = '' }) {
  const reduceMotion = useReducedMotion()
  return (
    <motion.div
      initial={reduceMotion ? false : { opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.5, delay }}
      className={className}
    >
      {children}
    </motion.div>
  )
}

// ===== Main Landing page =====
export default function Landing() {
  const { isAuthenticated } = useAuth()
  const [showDemo, setShowDemo] = useState(false)

  return (
    <div className="min-h-screen bg-canvas text-ink dark:bg-navy-deep dark:text-gray-100">
      {/* ===== Nav ===== */}
      <header className="sticky top-0 z-30 border-b border-line bg-canvas/90 backdrop-blur dark:border-gray-800 dark:bg-navy-deep/90">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brass text-white">
              <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
            </span>
            <span className="font-display text-lg font-semibold tracking-tight">Atlas</span>
          </Link>

          <div className="hidden items-center gap-6 text-sm text-slate dark:text-gray-400 md:flex">
            <a href="#features" className="transition-colors hover:text-ink dark:hover:text-gray-100">Features</a>
            <a href="#how-it-works" className="transition-colors hover:text-ink dark:hover:text-gray-100">Architecture</a>
            <a href="#nodes" className="transition-colors hover:text-ink dark:hover:text-gray-100">Docs</a>
          </div>

          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <AvatarDropdown />
            ) : (
              <>
                <Link to="/login" className="btn-secondary">Log in</Link>
                <Link to="/register" className="btn-primary">Get started</Link>
              </>
            )}
          </div>
        </nav>
      </header>

      {/* ===== Hero ===== */}
      <section className="relative overflow-hidden">
        {/* Subtle same-hue gradient backdrop */}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-brass/5 via-transparent to-transparent" />
        <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
          <div>
            <p className="mb-4 inline-flex items-center gap-2 rounded-full border border-brass/30 bg-brass/10 px-3 py-1 text-xs font-medium text-brass">
              <span className="h-1.5 w-1.5 rounded-full bg-brass" />
              Distributed file storage
            </p>
            <h1 className="font-display text-4xl font-semibold leading-tight tracking-tight sm:text-5xl lg:text-6xl">
              Your files, charted across a network of nodes.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-slate dark:text-gray-400">
              Atlas splits your files into chunks and distributes them across a monitored
              network of storage nodes. Lose one node, and your data is still safe on another.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register" className="btn-primary px-6 py-3 text-base">
                Get started
              </Link>
              <button onClick={() => setShowDemo(true)} className="btn-secondary px-6 py-3 text-base">
                View live demo
              </button>
            </div>
          </div>

          <Reveal>
            <HeroNetwork />
          </Reveal>
        </div>
      </section>

      {/* ===== Features ===== */}
      <section id="features" className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24">
        <Reveal>
          <div className="mb-12 max-w-2xl">
            <h2 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              Built like a real distributed system
            </h2>
            <p className="mt-3 text-lg text-slate dark:text-gray-400">
              Not a thin wrapper over a single server. These are the mechanisms that keep
              your data available and intact.
            </p>
          </div>
        </Reveal>

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f, i) => (
            <Reveal key={f.title} delay={i * 0.05}>
              <div className="group h-full rounded-xl border border-line bg-surface p-6 shadow-card transition-all hover:-translate-y-0.5 hover:shadow-card-hover dark:border-gray-700 dark:bg-navy-surface">
                <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-brass/10 text-brass transition-colors group-hover:bg-brass group-hover:text-white">
                  <f.icon className="h-5 w-5" />
                </div>
                <h3 className="font-display text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate dark:text-gray-400">{f.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ===== How it works ===== */}
      <section id="how-it-works" className="border-y border-line bg-surface dark:border-gray-800 dark:bg-navy-surface">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24">
          <Reveal>
            <div className="mb-12 max-w-2xl">
              <h2 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
                How a file travels through Atlas
              </h2>
              <p className="mt-3 text-lg text-slate dark:text-gray-400">
                A genuine sequence — from your upload to full redundancy.
              </p>
            </div>
          </Reveal>

          <div className="grid gap-4 lg:grid-cols-5">
            {STEPS.map((s, i) => (
              <Reveal key={s.title} delay={i * 0.08}>
                <div className="relative h-full rounded-xl border border-line bg-canvas p-5 dark:border-gray-700 dark:bg-navy-deep">
                  <div className="mb-3 flex items-center gap-2">
                    <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brass text-sm font-semibold text-white">
                      {i + 1}
                    </span>
                    {i < STEPS.length - 1 && (
                      <span className="hidden h-px flex-1 bg-line dark:bg-gray-700 lg:block" />
                    )}
                  </div>
                  <h3 className="font-display text-base font-semibold">{s.title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-slate dark:text-gray-400">{s.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ===== Live node status teaser ===== */}
      <section id="nodes" className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24">
        <Reveal>
          <div className="mb-10 max-w-2xl">
            <h2 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              The cluster, at a glance
            </h2>
            <p className="mt-3 text-lg text-slate dark:text-gray-400">
              Every node is monitored. A background sweep marks failures and triggers
              automatic re-replication.
            </p>
          </div>
        </Reveal>

        <Reveal>
          <div className="grid gap-4 sm:grid-cols-3">
            {TEASER_NODES.map((n) => (
              <div
                key={n.name}
                className="group rounded-xl border border-line bg-surface p-5 shadow-card transition-all hover:-translate-y-0.5 hover:shadow-card-hover dark:border-gray-700 dark:bg-navy-surface"
              >
                <div className="flex items-center justify-between">
                  <span className="font-display text-base font-semibold">{n.name}</span>
                  <span className="flex items-center gap-1.5 text-xs font-medium text-pine">
                    <span className="h-2 w-2 animate-pulse rounded-full bg-pine" />
                    Healthy
                  </span>
                </div>
                <p className="mt-2 text-sm text-slate dark:text-gray-400">{n.chunks} chunks stored</p>
                <div className="mt-3">
                  <div className="mb-1 flex items-center justify-between text-xs text-slate dark:text-gray-400">
                    <span>Capacity</span>
                    <span>{n.used}%</span>
                  </div>
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-line dark:bg-gray-700">
                    <div className="h-full rounded-full bg-brass" style={{ width: `${n.used}%` }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Reveal>

        <Reveal delay={0.1}>
          <p className="mt-6 text-sm text-slate dark:text-gray-400">
            Simulate a node failure and watch the system recover — try it after{' '}
            <Link to="/register" className="font-medium text-brass hover:text-brass-hover">
              signing up
            </Link>
            .
          </p>
        </Reveal>
      </section>

      {/* ===== CTA ===== */}
      <section className="border-t border-line bg-surface dark:border-gray-800 dark:bg-navy-surface">
        <div className="mx-auto max-w-6xl px-4 py-16 text-center sm:px-6">
          <Reveal>
            <h2 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              Ready to chart your storage?
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-lg text-slate dark:text-gray-400">
              Create an account and start uploading. Your files will be distributed and
              protected from the first byte.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Link to="/register" className="btn-primary px-6 py-3 text-base">
                Get started
              </Link>
              <button onClick={() => setShowDemo(true)} className="btn-secondary px-6 py-3 text-base">
                View live demo
              </button>
            </div>
          </Reveal>
        </div>
      </section>

      {/* ===== Footer ===== */}
      <footer className="border-t border-line bg-canvas dark:border-gray-800 dark:bg-navy-deep">
        <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
          <div className="flex flex-col items-start justify-between gap-8 sm:flex-row">
            <div>
              <div className="flex items-center gap-2.5">
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brass text-white">
                  <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                  </svg>
                </span>
                <span className="font-display text-lg font-semibold tracking-tight">Atlas</span>
              </div>
              <p className="mt-3 max-w-xs text-sm text-slate dark:text-gray-400">
                A distributed file storage system. A systems-design project.
              </p>
            </div>

            <div className="flex flex-col gap-2 text-sm text-slate dark:text-gray-400">
              <span className="mb-1 text-xs font-medium uppercase tracking-wide text-slate/70 dark:text-gray-500">
                Tech stack
              </span>
              {['Spring Boot', 'PostgreSQL', 'Redis', 'React'].map((t) => (
                <span key={t} className="flex items-center gap-2">
                  <span className="h-1 w-1 rounded-full bg-brass" />
                  {t}
                </span>
              ))}
            </div>
          </div>

          <div className="mt-8 border-t border-line pt-6 text-xs text-slate/70 dark:border-gray-800 dark:text-gray-500">
            © {new Date().getFullYear()} Project Atlas. Built as a systems-design project.
          </div>
        </div>
      </footer>

      {/* ===== Demo modal ===== */}
      <DemoModal open={showDemo} onClose={() => setShowDemo(false)} />
    </div>
  )
}