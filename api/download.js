module.exports = (req, res) => {
  const apkUrl = process.env.APK_DOWNLOAD_URL || 'https://github.com/dydjddur/Shorashim/releases/download/v1.0.0/shorashim-v1.0.0.apk';
  res.writeHead(302, {
    'Location': apkUrl,
    'Cache-Control': 'no-cache'
  });
  res.end();
};
