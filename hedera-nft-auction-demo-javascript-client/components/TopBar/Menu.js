/* This example requires Tailwind CSS v2.0+ */
import React from 'react'
import { useRouter } from 'next/router'
import CloseIcon from './assets/close_icon.svg'
import ConnectWalletIcon from './assets/connect_wallet_logo.svg'
import MobileMenuIcon from './assets/mobile_menu_icon.svg'
import NFTLogo from './assets/nft_logo.svg'

const Link = ({ item, isMobile, closeMenu }) => {
  const isActive = item.isActive
  const router = useRouter()

  const goToPage = () => {
    router.push(item.to)
    closeMenu()
  }

  const ActiveBorder = () => (
    <div style={{ width: '75%', border: '1px solid #5266F1' }} />
  )
  const TransparentBorder = () => (
    <div style={{ width: '75%', border: '1px solid transparent' }} />
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
      className='sm:py-2 py-4 mx-7 bg-black text-white border-b border-indigo-500'
    >
      <div className='mx-auto'>
        <div className='relative flex items-center justify-between h-16'>
          <div className='flex items-center justify-between sm:items-stretch sm:justify-between w-full'>
            <div
              onClick={handleMenuToggle}
              className='cursor-pointer inline-flex items-center justify-center p-2 rounded-md text-gray-400 focus:outline-none focus:ring-none sm:hidden'
            >
              {isOpen ? (
                <CloseIcon className='block h-6 w-6' />
              ) : (
                <MobileMenuIcon className='block h-6 w-6' />
              )}
            </div>
            <div className='flex'>
              <NFTLogo
                style={{ top: '2px', width: '5.75rem' }}
                className='relative'
              />
              {/* Desktop Nav */}
              <div className='hidden sm:flex items-center sm:ml-10'>
                {navigation.map(item => (
                  <Link key={item.name} item={item} closeMenu={closeMenu} />
                ))}
              </div>
            </div>
            <ConnectWalletIcon style={{ width: '6.5rem' }} />
          </div>
        </div>
      </div>
      {/* Mobile Nav */}
      <div className='sm:hidden flex mt-3'>
        {isOpen &&
          navigation.map(item => (
            <Link isMobile key={item.name} item={item} closeMenu={closeMenu} />
          ))}
      </div>
    </div>
  )
}
