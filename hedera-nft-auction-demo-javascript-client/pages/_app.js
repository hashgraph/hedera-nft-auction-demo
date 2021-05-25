import '../styles/index.css'
import Topbar from 'components/TopBar'
import { QueryClient, QueryClientProvider } from 'react-query'

const queryClient = new QueryClient()

function HederaAuctionApp({ Component, pageProps }) {
  return (
    <QueryClientProvider client={queryClient}>
      <Topbar />
      <Component {...pageProps} />
    </QueryClientProvider>
  )
}

export default HederaAuctionApp
