const getWalletData = () => {
  return new Promise(async (resolve, reject) => {
    try {
      const network =
        process.env.NEXT_PUBLIC_NETWORK.charAt(0).toUpperCase() +
        process.env.NEXT_PUBLIC_NETWORK.slice(1)
      let wallet = window.wallet
      if (!wallet) {
        document.addEventListener('hederaWalletLoaded', async () => {
          wallet = window.wallet
          const account = await wallet.login(network)
          const provider = wallet.getTransactionSigner()
          resolve({
            account,
            provider,
          })
        })
      } else {
        const account = await wallet.login(network)
        const provider = wallet.getTransactionSigner()
        resolve({
          account,
          provider,
        })
      }
    } catch (error) {
      reject(error)
    }
  })
}

export default getWalletData
