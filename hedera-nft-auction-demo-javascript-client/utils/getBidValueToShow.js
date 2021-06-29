import { HBAR_DECIMALS } from '../constants'

const getBidValue = bidamount => bidamount / HBAR_DECIMALS

export default getBidValue
