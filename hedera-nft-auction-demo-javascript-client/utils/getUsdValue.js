const getUsdValue = (bidAmountToShow, currentPrice) =>
  (bidAmountToShow * currentPrice).toLocaleString()

export default getUsdValue
