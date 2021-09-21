import React from 'react'
import { CopyToClipboard } from 'react-copy-to-clipboard'
import { FaCopy } from 'react-icons/fa'
import CloseIcon from './assets/close_icon.svg'

const Modal = ({ isOpen, close, auction }) => {
  const { auctionaccountid, title } = auction
  const [hasCopiedText, setCopiedTextStatus] = React.useState(false)
  if (!isOpen) return null

  const handleClose = () => {
    const body = window.document.getElementsByTagName('body')[0]
    body.style.overflow = 'inherit'
    close()
  }

  const handleCopy = () => {
    setCopiedTextStatus(true)
    setTimeout(() => {
      setCopiedTextStatus(false)
    }, 1500)
  }

  const Content = () => {
    return (
      <div className='mt-5 text-left sm:mt-0 w-full'>
        <div className=' pb-8'>
          <h1 className='font-light text-2xl pb-1'>Bid on {title}</h1>
          <p className='font-thin text-sm'>
            The {title} auction accepts bids in HBAR.
            <br />
            Non-winning bids are automatically refunded.
          </p>
        </div>
        <div className='pb-8 relative'>
          <p className='uppercase font-light text-xxs pb-2'>Auction Account</p>
          {hasCopiedText && (
                <p className='text-white text-sm absolute -top-1 right-0'>
                  Copied!
                </p>
              )}
          <div className='relative'>  
            <p className='bg-white bg-opacity-10 text-center py-2 px-4 mb-4 text-lg font-bold'>
              {auctionaccountid}
            </p>
            <CopyToClipboard onCopy={handleCopy} text={auctionaccountid} className='absolute top-3.5 right-2'>
                    <FaCopy
                      className='cursor-pointer relative'
                      style={{ color: '#4B68F1' }}
                    />
              </CopyToClipboard>
          </div>        
          <p className='font-thin text-sm'>
            Send bids to the {title} auction account from any hbar support
            wallet.
          </p>
        </div>
        <div>
          <h1 className='text-2xl font-light pb-1'>Need hbars?</h1>
          <p className='text-sm font-thin pb-3'>
            A list of hbar supported wallets and exchanges can be found at{' '}
            <a target='_blank' href='https://hedera.com/buying-guide'>
              hedera.com/buying-guide
            </a>
            .
          </p>
          <p className='text-sm font-thin'>
            For common questions about the auction and Hedera Hashgraph visit{' '}
            <a target='_blank' href='https://help.hedera.com/hc/en-us/articles/4407199123089'>
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
          className=' border-l-4 w-full inline-block align-bottom bg-black text-left overflow-hidden shadow-card transform transition-all sm:align-middle sm:max-w-md sm:w-full'
          role='dialog'
          aria-modal='true'
          aria-labelledby='modal-headline'
          style={{
            borderColor: '#4B68F1',
          }}
        >
          <div className='px-8 pb-8 pt-12 w-full relative'>
            <CloseIcon
              className='h-8 w-8 text-white absolute cursor-pointer'
              onClick={handleClose}
              style={{
                top: '10px',
                right: '8px',
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
