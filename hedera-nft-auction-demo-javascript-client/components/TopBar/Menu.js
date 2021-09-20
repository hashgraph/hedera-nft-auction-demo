import React from 'react'
import { useRouter } from 'next/router'
import CloseIcon from './assets/close_icon.svg'
import ConnectWalletIcon from './assets/connect_wallet_logo.svg'
import MobileMenuIcon from './assets/mobile_menu_icon.svg'
import NFTLogo from './assets/nft-auction.svg'
import { useWindowSize } from '@react-hook/window-size'
import { isMobile } from 'react-device-detect'

const Link = ({ item, isMobile, closeMenu, externalLink, className }) => {
  const isActive = item.isActive
  const router = useRouter()

  const goToPage = () => {
    if (externalLink) {
      window.open(externalLink, '_blank')
      closeMenu()
      return
    }
    router.push(item.to)
    closeMenu()
  }

  const ActiveBorder = () => (
    <div
      style={{
        width: '36px',
        border: '1px solid #5266F1',
        position: 'relative',
        bottom: '1px',
      }}
    />
  )
  const TransparentBorder = () => (
    <div
      style={{
        width: '36px',
        border: '1px solid transparent',
        position: 'relative',
        bottom: '1px',
      }}
    />
  )

  return (
    <div
      className={
        `sm:mx-10 mx-0 ml-5 ${isMobile ? 'py-3' : ''} ` + (className || '')
      }
    >
      {isActive ? <ActiveBorder /> : <TransparentBorder />}
      <div onClick={goToPage} className={'text-white cursor-pointer'}>
        <span
          style={{
            letterSpacing: '-.025rem !important',
          }}
        >
          {item.name}
        </span>
      </div>
    </div>
  )
}

const TopBarMenu = () => {
  const [isOpen, setOpen] = React.useState(false)
  const handleMenuToggle = () => setOpen(!isOpen)
  const [width, height] = useWindowSize()
  const connectWalletLogoSrc = '/assets/connect-wallet.png'
  const mobileConnectWalletLogoSrc = '/assets/connect-wallet-mobile.png'

  const closeMenu = () => setOpen(false)

  const router = useRouter()
  const location = router.pathname

  const isViewingLiveAuction = location === '/live-auctions'
  const isViewingSold = location.includes('sold')

  const goToHomePage = () => router.push('/')

  const navigation = [
    {
      name: 'Live Auctions',
      to: '/live-auctions',
      isActive: isViewingLiveAuction,
    },
    { name: 'Sold', to: '/sold', isActive: isViewingSold },
    {
      name: 'FAQ',
      externalLink: 'https://help.hedera.com/hc/en-us/articles/4407199123089',
      className: 'sm:hidden block',
    },
  ]

  const goToHederaFAQ = () => window.open('https://help.hedera.com/hc/en-us/articles/4407199123089', '_blank')

  return (
    <div
      as='nav'
      className='sm:py-2 py-4 bg-black text-white border-b border-indigo-500 theme-margin'
    >
      <div className='mx-auto'>
        <div className='relative flex items-center justify-between h-20'>
          <div className='flex items-center justify-between sm:justify-between w-full sm:flex-row flex-row-reverse'>
            <div
              onClick={handleMenuToggle}
              className='cursor-pointer inline-flex items-center justify-center rounded-md text-gray-400 focus:outline-none focus:ring-none sm:hidden'
            >
              {isOpen ? (
                <CloseIcon className='block h-6 w-6' />
              ) : (
                <MobileMenuIcon className='block h-6 w-6' />
              )}
            </div>
            <div className='flex'>
              {/* <NFTLogo
                style={{ top: '2px', width: '8.75rem' }}
                className='relative cursor-pointer'
                onClick={goToHomePage}
              /> */}
              <img
                src='/assets/nft-auction.png'
                onClick={goToHomePage}
                className='cursor-pointer'
              />
              {/* Desktop Nav */}
              <div className='hidden sm:flex items-center sm:ml-10'>
                {navigation.map(item => (
                  <Link
                    key={item.name}
                    item={item}
                    closeMenu={closeMenu}
                    externalLink={item.externalLink}
                    className={item.className}
                  />
                ))}
              </div>
            </div>
            {!(isMobile && width < 640) && (
              <p className='cursor-pointer sm:block hidden' onClick={goToHederaFAQ}>
                FAQ
              </p>
            )}
          </div>
        </div>
      </div>
      {/* Mobile Nav */}
      <div className='sm:hidden flex mt-3'>
        {isOpen &&
          navigation.map(item => (
            <Link
              isMobile
              key={item.name}
              item={item}
              closeMenu={closeMenu}
              externalLink={item.externalLink}
            />
          ))}
      </div>
    </div>
  )
}

export default TopBarMenu
