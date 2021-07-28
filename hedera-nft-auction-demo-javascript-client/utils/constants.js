const isDev = process.env.NODE_ENV === 'development'
const isProd = process.env.NODE_ENV === 'production'
export const API_BASE_URL = isProd
  ? 'https://104.198.67.87:8081/v1'
  : 'https://104.198.67.87:8081/v1'