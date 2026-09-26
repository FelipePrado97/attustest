module.exports = {
  '/api': {
    target: process.env.API_URL || 'http://localhost:8080',
    secure: false,
  },
};
