
const Footer = () => {
  const currentYear = new Date().getFullYear()
  return (
    <div className='flex justify-between p-5 mt-4 items-center font-thin'>
      <img style={{ width: '12rem' }} src='assets/footer_logo.svg' />
      <p>® {currentYear} All Rights Reserved</p>
    </div>
  )
}

export default Footer
