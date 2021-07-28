const isDev = process.env.NODE_ENV === 'development'
const isProd = process.env.NODE_ENV === 'production'
console.log('IS DEV', isDev)
console.log('IS Prod', isProd)
export const API_BASE_URL = 'https://localhost:8081/v1'
