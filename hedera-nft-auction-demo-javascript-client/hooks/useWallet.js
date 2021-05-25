import { useState, useEffect } from 'react'
import getHederaWallet from 'utils/getHederaWallet'

function useWallet() {
  const [isGettingWallet, setGettingWalletStatus] = useState(false)
  const [wallet, setWallet] = useState(null)

  useEffect(() => {
    setGettingWalletStatus(true)
    const asyncGetWallet = async () => {
      const wallet = await getHederaWallet()
      setGettingWalletStatus(false)
      setWallet(wallet)
    }
    asyncGetWallet()
  }, [])

  return { isFetching: isGettingWallet, wallet }
}

export default useWallet
