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
      card: '2px 5px 27px -6px rgba(255,255,255,0.75)',
      'bid-item': '10px 21px 55px -21px rgba(255,255,255.75)',
      'bid-modal': '10px 21px 55px -21px rgba(255,255,255.75)',
    },
    extend: {
      fontFamily: {
        styrena: ['Styerna'],
      },
      fontSize: {
        'card-tokenid': '12px',
        'card-title': '1.25rem',
        'card-subtitle': '12px',
        'card-units': '13px',
      },
      width: {
        'card-small': '22%',
      },
      height: {
        card: '30rem',
      },
    },
  },
  variants: {
    backgroundColor: ({ after }) => after(['disabled']),
  },
  plugins: [],
}
