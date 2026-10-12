module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Cache-Control', 's-maxage=120, stale-while-revalidate=300');

  try {
    const ghRes = await fetch('https://api.github.com/repos/dydjddur/Shorashim/releases/latest', {
      headers: {
        'User-Agent': 'Shorashim-App',
        'Accept': 'application/vnd.github.v3+json'
      }
    });
    if (ghRes.ok) {
      const data = await ghRes.json();
      const apkAsset = (data.assets || []).find(a => (a.name || '').endsWith('.apk'));
      if (apkAsset && apkAsset.browser_download_url) {
        res.writeHead(302, {
          'Location': apkAsset.browser_download_url,
          'Cache-Control': 'no-cache'
        });
        return res.end();
      }
      if (data.tag_name) {
        const directUrl = `https://github.com/dydjddur/Shorashim/releases/download/${data.tag_name}/shorashim-${data.tag_name}.apk`;
        res.writeHead(302, { 'Location': directUrl, 'Cache-Control': 'no-cache' });
        return res.end();
      }
    }
  } catch (e) {
    // Fall through to fallback
  }

  const fallbackUrl = 'https://github.com/dydjddur/Shorashim/releases/download/v1.0.0/shorashim-v1.0.0.apk';
  res.writeHead(302, {
    'Location': fallbackUrl,
    'Cache-Control': 'no-cache'
  });
  res.end();
};
