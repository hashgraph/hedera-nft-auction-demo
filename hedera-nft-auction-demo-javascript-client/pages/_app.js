import '../styles/index.css'
import Topbar from 'components/TopBar'
import Footer from 'components/Footer'
import { QueryClient, QueryClientProvider } from 'react-query'

const queryClient = new QueryClient()

function HederaAuctionApp({ Component, pageProps }) {
  return (
    <QueryClientProvider client={queryClient}>
      <Topbar />
      <div
        style={{ minHeight: 'calc(100vh - 12rem)' }}
        className='sm:p-section-desktop p-section-mobile'
      >
        <Component {...pageProps} />
      </div>
      <Footer />
    </QueryClientProvider>
  )
}

export default HederaAuctionApp
