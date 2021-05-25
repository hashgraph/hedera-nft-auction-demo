import React from 'react'
import BidMetaData from './BidMetaData'
import BidButton from './BidButton'
import BidHistory from './BidHistory'
import BidModal from 'components/modals/BidModal'
import ShareModal from 'components/modals/ShareModal'

const RightColumn = ({ auction }) => {
  const [isPlacingBid, setPlacingBidStatus] = React.useState(false)
  const [isSharingAuction, setSharingAuction] = React.useState(false)

  const openBidModal = () => setPlacingBidStatus(true)
  const closeBidModal = () => setPlacingBidStatus(false)

  const openShareModal = () => setSharingAuction(true)
  const closeShareModal = () => setSharingAuction(false)

  const { active } = auction

  return (
    <div>
      <div className='hidden md:block'>
        <BidMetaData auction={auction} />
      </div>
      <div className='hidden md:block'>
        {active && <BidButton openBidModal={openBidModal} />}
      </div>
      <div
        className='sm:visible invisible flex flex-col items-end justify-center cursor-pointer'
        onClick={openShareModal}
      >
        <img
          src='/assets/share-icon.svg'
          className='h-5 w-5 relative'
          style={{ right: '11px' }}
        />
        <p className='font-light'>Share</p>
      </div>
      <BidHistory auction={auction} />
      <BidModal isOpen={isPlacingBid} close={closeBidModal} auction={auction} />
      <ShareModal isOpen={isSharingAuction} close={closeShareModal} />
    </div>
  )
}

export default RightColumn
