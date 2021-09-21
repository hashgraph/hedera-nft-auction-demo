const HbarUnit = ({
  className,
  amount,
  denomination,
  italic,
  bold,
  amountBold,
  large,
  medium,
  card,
  description,
}) => {
  const getFontSize = () => {
    if (medium) return '28px'
    if (large) return '34px'
    if (card) return '22px'
    return ''
  }
  const fontSize = getFontSize()
  let classNames = ''
  if (bold) classNames += ' font-bold '
  if (!amount && amount !== 0)
    return <span className={classNames + className}>ℏ</span>
  if(denomination === 'tinybar')
    return (
      <span className={className} style={{ fontSize }}>
        <span className={amountBold ? 'font-bold' : ''}>{Math.round((amount / 100000000 + Number.EPSILON) * 100) / 100}</span>
        <span className={classNames + 'font-light'} style={{ marginLeft: '3px' }}>
          ℏ
        </span>
      </span>
    ) 
  if (amount % 1000000 === 0)
    return (
      <span className={className} style={{ fontSize }}>
        <span className={amountBold ? 'font-bold' : ''}>
          {amount / 1000000}
        </span>
        <span
          className={classNames + 'font-light'}
          style={{ marginLeft: '3px' }}
        >
          ℏ
        </span>
      </span>
    )
  return (
    <span className={className} style={{ fontSize }}>
      <span className={amountBold ? 'font-bold' : ''}>{amount}</span>
      <span className={classNames + 'font-light'} style={{ marginLeft: '3px' }}>
        ℏ
      </span>
    </span>
  )
}

export default HbarUnit
