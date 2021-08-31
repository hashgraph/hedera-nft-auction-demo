import { Client, Status, TransferTransaction } from '@hashgraph/sdk'
import getWallet from 'utils/getHederaWallet'

const sendBid = ({ auctionAccountId, bid }) => {
  return new Promise(async (resolve, reject) => {
    const wallet = await getWallet()
    const { account, provider } = wallet
    try {
      let message = 'Preparing Bid Transaction'
      let client = Client.forTestnet()
      if (process.env.NEXT_PUBLIC_NETWORK.toUpperCase() === 'MAINNET') {
        client = Client.forMainnet()
      } else if (
        process.env.NEXT_PUBLIC_NETWORK.toUpperCase() === 'PREVIEWNET'
      ) {
        client = Client.forPreviewnet()
      }
      if (account) {
        client.setOperatorWith(account.id, account.publicKey, provider)
        message = 'Signing Bid Transaction'
        const toExecute = await new TransferTransaction()
          .addHbarTransfer(account.id, -bid)
          .addHbarTransfer(auctionAccountId, bid)
          .freezeWith(client)
          .signWithOperator(client)
          .catch(err => {
            message = err.message
            return null
          })

        if (toExecute) {
          message = 'Sending Bid Transaction to Hedera'
          const executed = await toExecute.execute(client)

          // reset client so receipt request doesn't prompt for signature
          message = 'Fetching Receipt'
          client = Client.forTestnet()
          if (process.env.NEXT_PUBLIC_NETWORK.toUpperCase() === 'MAINNET') {
            client = Client.forMainnet()
          } else if (
            process.env.NEXT_PUBLIC_NETWORK.toUpperCase() === 'PREVIEWNET'
          ) {
            client = Client.forPreviewnet()
          }
          const receipt = await executed.getReceipt(client)
          message = ''
          if (receipt.status == Status.Success) {
            resolve('Bid Placed Successfully')
          } else {
            reject(receipt.status.toString())
          }
        }
      } else {
        message = 'Unable to login with extension - bid aborted'
      }
    } catch (error) {
      reject(error)
    }
  })
}

export default sendBid
