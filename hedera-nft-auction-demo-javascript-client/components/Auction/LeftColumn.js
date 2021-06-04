import BidMetaData from './RightColumn//BidMetaData'
import ShareModal from 'components/modals/ShareModal'
import React from 'react'

const LeftColumn = ({ auction }) => {
  const [isSharingAuction, setSharingAuction] = React.useState(false)

  const openShareModal = () => setSharingAuction(true)
  const closeShareModal = () => setSharingAuction(false)

  const { tokenid, tokenimage, title, description, auctionaccountid } = auction
  const sidebarImage = tokenimage || '/assets/default-token-image.jpg'

  const titleToRender = title || 'Title'
  const descriptionToRender =
    description ||
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed doeiusmod tempor incididunt ut labore et dolore magna aliqua.'

  return (
    <div className='p-2'>
      <div className='flex justify-between'>
        <p className='hidden sm:flex font-light mb-5 text-sm whitespace-nowrap'>
          Token ID: <span className='ml-1 font-bold'>{tokenid}</span>
        </p>
        <p className='visible sm:invisible font-light mb-5'>
          Acct ID: <span className='font-bold'>{auctionaccountid}</span>
        </p>
        {/* <img src='/assets/expand-icon.svg' className='w-5 h-5' /> */}
        <div
          className='sm:invisible visible flex flex-col items-end justify-center cursor-pointer'
          onClick={openShareModal}
        >
          <img
            src='/assets/share-icon.svg'
            className='h-5 w-5 relative'
            style={{ right: '11px' }}
          />
          <p className='font-light'>Share</p>
        </div>
      </div>
      <div className='flex justify-between sm:flex-row flex-col'>
        <img
          className='sm:w-full pr-3 py-3'
          src={sidebarImage}
          alt='bid-item'
        />
        <div className='sm:hidden visible'>
          <BidMetaData auction={auction} />
        </div>
      </div>
      <p className='font-bold text-2xl my-5'>{titleToRender}</p>
      <div className='font-thin'>
        <p className='mb-4 text-sm'>{descriptionToRender}</p>
        <p className='mb-4'>
          The highest bigger on the HNFT will receive a signed print of the
          painting by the artist
        </p>
      </div>
      <p className='font-light'>
        Edition: <span className='font-bold'>1</span>
      </p>
      <p className='visible sm:invisible font-light mb-5'>
        Token ID: <span className='font-bold'>{tokenid}</span>
      </p>
      <ShareModal isOpen={isSharingAuction} close={closeShareModal} />
    </div>
  )
}

export default LeftColumn
