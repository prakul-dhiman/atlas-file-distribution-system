/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Cartography + distributed-systems palette
        canvas: '#F6F5F2',      // light page background (map-paper warm white)
        surface: '#FFFFFF',      // cards, modals, elevated panels
        ink: '#1C1E22',          // primary text (deep charcoal-ink)
        slate: '#565C66',        // secondary text, muted labels
        line: '#E5E2DB',         // borders, dividers (light)
        brass: {
          DEFAULT: '#B0763B',    // accent — primary buttons, links, focus
          hover: '#94602E',      // accent hover/active
          soft: '#F3E9DC',       // accent tint backgrounds
        },
        navy: {
          DEFAULT: '#1D2939',    // deep chart-navy — hero/diagram canvas, dark surface
          deep: '#161A21',       // dark-mode page background
          surface: '#1F242D',    // dark-mode card surface
        },
        pine: {
          DEFAULT: '#2E7D5B',    // success — node healthy, verified
          soft: '#E4F0EA',       // success tint
        },
        brick: {
          DEFAULT: '#BC4A3C',    // danger — delete, purge, node down
          soft: '#F9E9E7',       // danger tint
        },
        // Alias so existing `primary-*` utility classes resolve to the brass accent
        primary: {
          50: '#F3E9DC',
          100: '#EAD9C4',
          200: '#D9BFA0',
          300: '#C9A47C',
          400: '#BC8A58',
          500: '#B0763B',
          600: '#94602E',
          700: '#7A4F27',
          800: '#5F3E1F',
          900: '#452D17',
        },
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['IBM Plex Sans', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 3px 0 rgb(0 0 0 / 0.08), 0 1px 2px -1px rgb(0 0 0 / 0.08)',
        'card-hover': '0 6px 12px -2px rgb(0 0 0 / 0.12), 0 3px 6px -3px rgb(0 0 0 / 0.1)',
        'brass-glow': '0 0 0 3px rgb(176 118 59 / 0.25)',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-in': 'fadeIn 0.15s ease-out',
        'slide-in': 'slideIn 0.2s ease-out',
      },
    },
  },
  plugins: [],
}