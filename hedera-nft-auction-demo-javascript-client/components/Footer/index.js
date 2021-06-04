import FooterLogo from './assets/footer_logo.svg'

const Footer = () => {
  const currentYear = new Date().getFullYear()
  return (
    <div className='flex justify-between py-5 mt-4 items-center font-thin mx-7 border-t border-indigo-500 theme-margin text-xs'>
      <FooterLogo style={{ width: '10.5rem' }} />
      <p>® {currentYear} All Rights Reserved</p>
    </div>
  )
}

export default Footer
