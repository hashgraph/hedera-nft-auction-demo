import { useRouter } from 'next/router'

const ViewButton = ({ auctionId }) => {
  const router = useRouter()
  const goToAuctionDetailPage = () => router.push(`/auction/${auctionId}`)
  return (
    <button
      onClick={goToAuctionDetailPage}
      className='cursor-pointer border-gradient border-gradient-purple px-3 uppercase ml-5 font-light text-sm'
    >
      View
    </button>
  )
}

export default ViewButton
