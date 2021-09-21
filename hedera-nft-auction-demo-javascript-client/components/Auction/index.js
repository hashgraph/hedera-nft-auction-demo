import React from 'react'
import LeftColumn from './LeftColumn'
import RightColumn from './RightColumn'
import fetchAuction from 'utils/getAuction'
import fetchAuctionImage from 'utils/getAuctionImage'
import { useRouter } from 'next/router'
import Lightbox from 'components/common/Lightbox'

const LEFT_COLUMN_WIDTH = '25%'
const RIGHT_COLUMN_WIDTH = '75%'

const AuctionView = () => {
  const router = useRouter()
  const { auctionId } = router.query
  const [auction, setAuction] = React.useState(null)
  const [auctionImage, setAuctionImage] = React.useState(null)
  const [isViewingLightbox, setLightboxStatus] = React.useState(false)

  const openLightbox = () => {
    const body = window.document.getElementsByTagName('body')[0]
    body.style.overflow = 'hidden'
    setLightboxStatus(true)
  }
  const closeLightbox = () => {
    const body = window.document.getElementsByTagName('body')[0]
    body.style.overflow = 'inherit'
    setLightboxStatus(false)
  } 

  React.useEffect(() => {
    const asyncFetchAuction = async () => {
      const auction = await fetchAuction(auctionId)
      setAuction(auction)
      const auctionImage = await fetchAuctionImage(auction.tokenmetadata)  
      setAuctionImage(auctionImage.image.description)
    }
    if (auctionId) asyncFetchAuction()
  }, [auctionId])

  if (!auction) return null



  return (
    <div className='flex sm:flex-row flex-col'>
      {isViewingLightbox && (
        <Lightbox
          close={closeLightbox}
          image={auctionImage}
        />
      )}
      <div className='sm:mr-24 mr-0' style={{ flexBasis: LEFT_COLUMN_WIDTH }}>
        <LeftColumn auction={auction} auctionImage={auctionImage} openLightbox={openLightbox} />
      </div>
      <div className='' style={{ flexBasis: RIGHT_COLUMN_WIDTH }}>
        <RightColumn auction={auction} />
      </div>
    </div>
  )
}

export default AuctionView
