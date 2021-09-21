import BidMetaData from './RightColumn//BidMetaData'
import ShareModal from 'components/modals/ShareModal'
import React from 'react'

const LeftColumn = ({ auction, auctionImage, openLightbox }) => {
  const [isSharingAuction, setSharingAuction] = React.useState(false)

  const openShareModal = () => setSharingAuction(true)
  const closeShareModal = () => setSharingAuction(false)

  const { tokenid, title, description, auctionaccountid } = auction
  const sidebarImage = auctionImage

  const titleToRender = title || 'Title'
  const descriptionToRender =
    description ||
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed doeiusmod tempor incididunt ut labore et dolore magna aliqua.'

  return (
    <div className='tracking-tighter'>
      <div className='flex justify-between'>
        {/* Mobile View */}
        <div className='sm:hidden block'>
        <p className='font-bold my-5' style={{ fontSize: '26px' }}>
          {titleToRender}
        </p>
          <div className='font-thin text-sm'>
            <p className='mb-4 line-clamp-5'>{descriptionToRender}</p>
          </div>
          <p
            className='font-light text-sm whitespace-nowrap relative mb-2'
            style={{ top: '1px' }}
          >
            Token I.D.: <span className='ml-1 font-bold'>{tokenid}</span>
          </p>
          <p className='font-light sm:mb-0 mb-5 text-sm'>
            Acct I.D.: <span className='font-bold'>{auctionaccountid}</span>
          </p>
        </div>
        <img
          src='/assets/expand-icon.svg'
          className='w-5 h-5 cursor-pointer sm:hidden block'
          onClick={openLightbox}
        />
      </div>
      <div className='flex justify-between sm:flex-row flex-col'>
        <img
          className='sm:w-full sm:pt-10 pt-7 sm:pb-0 pb-11 object-contain h-full'
          src={sidebarImage}
          alt='bid-item'
        />
        {/* Desktop Expand */}
        <img
          src='/assets/expand-icon.svg'
          className='w-5 h-5 cursor-pointer sm:block hidden'
          onClick={openLightbox}
        />
        <div className='sm:hidden visible  sm:pb-0 sm:pb-16 pb-12'>
          <BidMetaData auction={auction} />
        </div>
      </div>
      {/* Desktop View */}
      <div className='sm:block hidden'>
        <p className='font-bold my-5' style={{ fontSize: '26px' }}>
          {titleToRender}
        </p>
        <div className='font-thin text-sm'>
          <p className='mb-4 line-clamp-5'>{descriptionToRender}</p>
        </div>
        <p className='font-light mb-5 text-sm mt-1'>
          Token I.D.: <span className='font-bold'>{tokenid}</span>
        </p>
      </div>
      <ShareModal isOpen={isSharingAuction} close={closeShareModal} />
    </div>
  )
}

export default LeftColumn
