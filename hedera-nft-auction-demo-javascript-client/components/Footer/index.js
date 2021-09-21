import FooterLogo from './assets/footer_logo.svg'

const Footer = () => {
  const currentYear = new Date().getFullYear()
  return (
    <div className='flex justify-between py-5 mt-4 items-center font-thin mx-7 border-t border-indigo-500 theme-margin text-xs'>
      <FooterLogo style={{ width: '9.5rem' }} />
      <p className='text-right sm:text-md text-10 leading-none mt-0 sm:mt-2.5'>® {currentYear} All Rights Reserved | <a target='_blank' href='https://help.hedera.com/hc/en-us/articles/4407713650321' className='whitespace-nowrap'>
      Terms and Conditions
            </a></p>
    </div>
  )
}

export default Footer
