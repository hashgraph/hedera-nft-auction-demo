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
      card: '1px 7px 23px -3px hsla(0,0%,100%,0.5)',
      'bid-item': '10px 5px 40px -19px rgba(255,255,255.75)',
      'bid-modal': '10px 21px 55px -21px rgba(255,255,255.75)',
      'share-modal': '0 10px 56px -2px rgba(255, 255, 255, 0.25)',
    },
    extend: {
      fontFamily: {
        styrena: ['Styerna'],
      },
      fontSize: {
        'card-tokenid': '10px',
        'card-title': '1rem',
        'card-subtitle': '12px',
        'card-units': '14px',
        xxs: '10px',
        20: '20px',
        22: '22px',
        32: '32px',
        30: '30px',
        34: '34px',
        36: '36px',
        17: '17px',
        16: '16px',
        10: '10px',
      },
      width: {
        'card-small': '22%',
        'card-md': '44%',
      },
      maxWidth: {
        'card': '33%',
      },
      height: {
        card: '20rem',
      },
      padding: {
        'section-mobile': '12.5%',
        'section-desktop': '6.5%',
        10: '10px',
        'mobile-bid-button': '.5rem 2rem',
      },
      lineHeight: {
        tight: '-1rem',
      },
      margin: {
        '25': '25px'
      },
      height: {
        '90': '90%'
      }
    },
  },
  variants: {
    backgroundColor: ({ after }) => after(['disabled']),
  },
  plugins: [require('@tailwindcss/line-clamp')],
}
