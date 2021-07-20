import React from 'react'
import CloseIcon from './assets/close_icon.svg'

const Modal = ({ isOpen, close, auction }) => {
  const { auctionaccountid, title } = auction
  if (!isOpen) return null

  const handleClose = () => {
    const body = window.document.getElementsByTagName('body')[0]
    body.style.overflow = 'inherit'
    close()
  }

  const Content = () => {
    return (
      <div className='mt-5 text-left sm:mt-0 w-full'>
        <div className=' pb-8'>
          <h1 className='font-light text-xl pb-1'>Bid on {title}</h1>
          <p className='font-thin text-xs'>
            The {title} auction accepts bids in HBAR.
            <br />
            Non-winning bids are automatically refunded.
          </p>
        </div>
        <div className='pb-8'>
          <p className='uppercase font-light text-xxs pb-2'>Auction Account</p>
          <p className='bg-gray-800 rounded text-center py-2 px-4 mb-2'>
            {auctionaccountid}
          </p>
          <p className='font-thin text-xs'>
            Send bids to the {title} auction account from any HBAR support
            wallet.
          </p>
        </div>
        <div>
          <h1 className='font-light pb-1'>Need HBAR?</h1>
          <p className='text-xs font-thin pb-3'>
            A list of supported wallets and exchanges can be found at{' '}
            <a target='_blank' href='https://hedera.com/buying-guide'>
              hedera.com/buying-guide
            </a>
          </p>
          <p className='text-xs font-thin'>
            For common questions about the auction and Hedera Hashgraph visit{' '}
            <a target='_blank' href='https://help.hedera.com'>
              help.hedera.com
            </a>
            .
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className='fixed z-50 inset-0 overflow-y-none flex justify-center items-center'>
      <div
        className='flex items-end justify-center pt-4 px-4 text-center absolute'
        style={{
          top: '12.5rem',
        }}
      >
        <div
          className='fixed inset-0 transition-opacity'
          aria-hidden='true'
          style={{ touchAction: 'none' }}
          onClick={handleClose}
        >
          <div className='absolute inset-0 opacity-75 bg-black'></div>
        </div>
        {/* Modal Content */}
        <div
          className=' border-l-4 w-full inline-block align-bottom bg-black text-left overflow-hidden shadow-2xl transform transition-all sm:align-middle sm:max-w-md sm:w-full'
          role='dialog'
          aria-modal='true'
          aria-labelledby='modal-headline'
          style={{
            borderColor: '#4B68F1',
          }}
        >
          <div
            className='pl-8 pr-10 w-full'
            style={{
              position: 'relative',
              paddingTop: '2.5rem',
              bottom: '7px',
              paddingBottom: '1rem',
            }}
          >
            <CloseIcon
              className='h-8 w-8 text-white absolute cursor-pointer'
              onClick={handleClose}
              style={{
                top: '.75rem',
                right: '.25rem',
              }}
            />
            <Content />
          </div>
        </div>
      </div>
    </div>
  )
}

export default Modal
