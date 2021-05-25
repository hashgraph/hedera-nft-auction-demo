/* This example requires Tailwind CSS v2.0+ */
import React from 'react'
import { useRouter } from 'next/router'

const Link = ({ item, isMobile, closeMenu }) => {
  const isActive = item.isActive
  const router = useRouter()
  
  const goToPage = () => {
    router.push(item.to)
    closeMenu()
  }

  const ActiveBorder = () => (
    <div style={{ width: '75%', border: '2px solid #5266F1' }} />
  )
  const TransparentBorder = () => (
    <div style={{ width: '75%', border: '2px solid transparent' }} />
  )

  return (
    <div className={`sm:mx-10 mx-0 ml-5 ${isMobile ? 'py-3' : ''}`}>
      {isActive ? <ActiveBorder /> : <TransparentBorder />}
      <div onClick={goToPage} className='text-white font-light cursor-pointer'>
        <span>{item.name}</span>
      </div>
    </div>
  )
}

export default function Example() {
  const [isOpen, setOpen] = React.useState(false)
  const handleMenuToggle = () => setOpen(!isOpen)

  const closeMenu = () => setOpen(false)

  const router = useRouter()
  const location = router.pathname

  const isViewingLiveAuction = location === '/'
  const isViewingSold = location.includes('sold')

  const navigation = [
    { name: 'Live Auctions', to: '/', isActive: isViewingLiveAuction },
    { name: 'Sold', to: '/sold', isActive: isViewingSold },
  ]

  return (
    <div
      as='nav'
      className='sm:px-3 py-10 bg-black text-white border-b border-indigo-500 mb-10'
    >
      <div className='mx-auto px-2 sm:px-6 lg:px-8'>
        <div className='relative flex items-center justify-between h-16'>
          <div className='flex items-center justify-between sm:items-stretch sm:justify-between w-full'>
            <div
              onClick={handleMenuToggle}
              className='cursor-pointer inline-flex items-center justify-center p-2 rounded-md text-gray-400 focus:outline-none focus:ring-none sm:hidden'
            >
              {isOpen ? (
                <img
                  src='/assets/close_icon.svg'
                  className='block h-6 w-6'
                  aria-hidden='true'
                />
              ) : (
                <img
                  src='/assets/mobile_menu_icon.svg'
                  className='block h-6 w-6'
                  aria-hidden='true'
                />
              )}
            </div>
            <div className='flex'>
              <img
                style={{ top: '2px' }}
                className=' w-28 sm:w-36 relative'
                src='/assets/nft_logo.svg'
              />
              {/* Desktop Nav */}
              <div className='hidden sm:flex items-center sm:ml-10'>
                {navigation.map(item => (
                  <Link key={item.name} item={item} closeMenu={closeMenu} />
                ))}
              </div>
            </div>
            <img
              className=' w-28 sm:w-36'
              src='/assets/connect_wallet_logo.svg'
            />
          </div>
        </div>
      </div>
      {/* Mobile Nav */}
      <div className='sm:hidden'>
        {isOpen &&
          navigation.map(item => (
            <Link isMobile key={item.name} item={item} closeMenu={closeMenu} />
          ))}
      </div>
    </div>
  )
}
