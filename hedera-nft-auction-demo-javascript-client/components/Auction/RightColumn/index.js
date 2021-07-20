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
        className='hidden md:flex flex-col items-end justify-center cursor-pointer'
        onClick={openShareModal}
      >
        <div style={{ marginRight: '14px' }}>
          <img
            src='/assets/share-icon.svg'
            className='h-7 w-7 relative'
            style={{ left: '3px', marginBottom: '2px' }}
          />
          <p
            className='font-light'
            style={{ fontSize: '12px', paddingBottom: '6px' }}
          >
            Share
          </p>
        </div>
      </div>
      <BidHistory auction={auction} />
      <BidModal isOpen={isPlacingBid} close={closeBidModal} auction={auction} />
      <ShareModal isOpen={isSharingAuction} close={closeShareModal} />
    </div>
  )
}

export default RightColumn
