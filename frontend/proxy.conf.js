// Dev-server proxy: forwards /api to the Spring Boot backend so the SPA and API share an origin
// (the refresh-token cookie then works without CORS). Override the port with FITTRACK_API_PORT.
const apiPort = process.env.FITTRACK_API_PORT || 8081;

module.exports = {
  '/api': {
    target: `http://localhost:${apiPort}`,
    secure: false,
    changeOrigin: false,
    logLevel: 'warn',
  },
};
