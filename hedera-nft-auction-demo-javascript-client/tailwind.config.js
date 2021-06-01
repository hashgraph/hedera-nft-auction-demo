module.exports = {
  mode: 'jit',
  purge: ['./pages/**/*.{js,ts,jsx,tsx}', './components/**/*.{js,ts,jsx,tsx}'],
  darkMode: false, // or 'media' or 'class'
  theme: {
    boxShadow: {
      sm: '0 1px 2px 0 rgba(255, 255, 255, 0.05)',
      DEFAULT:
        '0 1px 3px 0 rgba(255, 255, 255, 0.1), 0 1px 2px 0 rgba(255, 255, 255, 0.06)',
      md: '0 4px 6px -1px rgba(255, 255, 255, 0.1), 0 2px 4px -1px rgba(255, 255, 255, 0.06)',
      lg: '0 10px 15px -3px rgba(255, 255, 255, 0.1), 0 4px 6px -2px rgba(255, 255, 255, 0.05)',
      xl: '0 20px 25px -5px rgba(255, 255, 255, 0.1), 0 10px 10px -5px rgba(255, 255, 255, 0.04)',
      '2xl': '0 25px 50px -12px rgba(255, 255, 255, 0.25)',
      '3xl': '0 35px 60px -15px rgba(255, 255, 255, 0.3)',
      inner: 'inset 0 2px 4px 0 rgba(255, 255, 255, 0.06)',
      card: '1px 6px 18px -5px hsla(0,0%,100%,0.50)',
      'bid-item': '10px 21px 55px -27px rgba(255,255,255.75)',
      'bid-modal': '10px 21px 55px -21px rgba(255,255,255.75)',
    },
    extend: {
      fontFamily: {
        styrena: ['Styerna'],
      },
      fontSize: {
        'card-tokenid': '10px',
        'card-title': '1rem',
        'card-subtitle': '10px',
        'card-units': '11px',
      },
      width: {
        'card-small': '22%',
      },
      height: {
        card: '20rem',
      },
      padding: {
        'section-mobile': '12.5%',
        'section-desktop': '6.5%'
      }
    },
  },
  variants: {
    backgroundColor: ({ after }) => after(['disabled']),
  },
  plugins: [],
}