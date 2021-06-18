const HbarUnit = ({ className, amount, italic }) => {
  const classNames = italic ? `italic ` : ''
  if (!amount && amount !== 0) return <span className={classNames + className}>ℏ</span>
  if (amount % 1000000 === 0)
    return (
      <p className={className}>
        {amount / 1000000}
        <span className={classNames}>ℏ</span>
      </p>
    )
  return (
    <p className={className}>
      {amount}
      <span className={classNames}>ℏ</span>
    </p>
  )
}

export default HbarUnit
