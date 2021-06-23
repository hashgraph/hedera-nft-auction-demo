import React from 'react'
import {
  FacebookShareButton,
  LinkedinShareButton,
  RedditShareButton,
  TelegramShareButton,
  TwitterShareButton,
  FacebookIcon,
  LinkedinIcon,
  TelegramIcon,
  TwitterIcon,
  RedditIcon,
} from 'react-share'
import { CopyToClipboard } from 'react-copy-to-clipboard'
import { FaCopy } from 'react-icons/fa'

const ShareIcons = ({ url }) => {
  const ICON_SIZE = 32
  const iconBgColor = '#4B68F1'

  const iconBgStyle = {
    fill: iconBgColor,
  }

  const TITLE = 'Check out this NFT Auction! '

  return (
    <div className='flex justify-between px-2'>
      <FacebookShareButton
        style={{ paddingLeft: '.5rem', paddingRight: '.5rem' }}
        url={url}
        title={TITLE}
      >
        <FacebookIcon round size={ICON_SIZE} bgStyle={iconBgStyle} />
      </FacebookShareButton>
      <LinkedinShareButton
        style={{ paddingLeft: '.5rem', paddingRight: '.5rem' }}
        url={url}
        title={TITLE}
      >
        <LinkedinIcon round size={ICON_SIZE} bgStyle={iconBgStyle} />
      </LinkedinShareButton>
      <TelegramShareButton
        style={{ paddingLeft: '.5rem', paddingRight: '.5rem' }}
        url={url}
        title={TITLE}
      >
        <TelegramIcon round size={ICON_SIZE} bgStyle={iconBgStyle} />
      </TelegramShareButton>
      <TwitterShareButton
        style={{ paddingLeft: '.5rem', paddingRight: '.5rem' }}
        url={url}
        title={TITLE}
      >
        <TwitterIcon round size={ICON_SIZE} bgStyle={iconBgStyle} />
      </TwitterShareButton>
      <RedditShareButton
        style={{ paddingLeft: '.5rem', paddingRight: '.5rem' }}
        url={url}
        title={TITLE}
      >
        <RedditIcon round size={ICON_SIZE} bgStyle={iconBgStyle} />
      </RedditShareButton>
    </div>
  )
}

const Modal = ({ isOpen, close, auction }) => {
  const [hasCopiedText, setCopiedTextStatus] = React.useState(false)
  if (!isOpen) return null

  const shareUrl = window.location.href

  const handleCopy = () => {
    setCopiedTextStatus(true)
    setTimeout(() => {
      setCopiedTextStatus(false)
    }, 1500)
  }

  return (
    <div className='fixed z-50 inset-0 overflow-y-none flex justify-center items-center'>
      <div className='pb-alot flex items-end justify-center pt-4 text-center'>
        <div
          className='fixed inset-0 transition-opacity'
          aria-hidden='true'
          style={{ touchAction: 'none' }}
          onClick={close}
        >
          <div className='absolute inset-0 bg-black opacity-75'></div>
        </div>
        <div
          className='inline-block align-bottom text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-md sm:w-full'
          role='dialog'
          aria-modal='true'
          aria-labelledby='modal-headline'
        >
          <div
            className='bg-black font-thin px-4 pt-5 pb-4 sm:pb-4 border-l-8 min-w-full'
            style={{
              borderColor: '#4B68F1',
            }}
          >
            <div className='pt-5 pb-4 sm:p-6 sm:pb-4'>
              {hasCopiedText && (
                <p className='text-white mb-2 absolute left-2 top-2'>Copied!</p>
              )}
              <div className='text-center'>
                <CopyToClipboard onCopy={handleCopy} text={shareUrl}>
                  <FaCopy
                    className='absolute right-2 top-2 cursor-pointer'
                    style={{ color: '#4B68F1' }}
                  />
                </CopyToClipboard>
                <p className='text-white m-3 py-3'>{shareUrl}</p>
              </div>
              <ShareIcons url={shareUrl} />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Modal