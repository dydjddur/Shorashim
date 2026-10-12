module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Cache-Control', 's-maxage=300, stale-while-revalidate=600');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    const ghRes = await fetch('https://api.github.com/repos/dydjddur/Shorashim/releases/latest', {
      headers: {
        'User-Agent': 'Shorashim-App',
        'Accept': 'application/vnd.github.v3+json'
      }
    });

    if (!ghRes.ok) {
      return res.status(200).json({
        tagName: 'v1.0.0',
        version: '1.0.0',
        name: 'שורשים v1.0.0',
        downloadUrl: 'https://github.com/dydjddur/Shorashim/releases/download/v1.0.0/shorashim-v1.0.0.apk',
        body: ''
      });
    }

    const data = await ghRes.json();
    const apkAsset = (data.assets || []).find(a => (a.name || '').endsWith('.apk'));
    const downloadUrl = apkAsset ? apkAsset.browser_download_url : `https://github.com/dydjddur/Shorashim/releases/download/${data.tag_name}/shorashim-${data.tag_name}.apk`;
    const cleanVersion = (data.tag_name || '').replace(/^[vV]/, '');

    return res.status(200).json({
      tagName: data.tag_name,
      version: cleanVersion,
      name: data.name || `שורשים ${data.tag_name}`,
      downloadUrl: downloadUrl,
      body: data.body || '',
      publishedAt: data.published_at
    });
  } catch (err) {
    return res.status(200).json({
      tagName: 'v1.0.0',
      version: '1.0.0',
      downloadUrl: 'https://github.com/dydjddur/Shorashim/releases/download/v1.0.0/shorashim-v1.0.0.apk'
    });
  }
};
