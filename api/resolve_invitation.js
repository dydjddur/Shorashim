module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const supabaseUrl = (
    process.env.SUPABASE_URL ||
    process.env.NEXT_PUBLIC_SUPABASE_URL ||
    process.env.VITE_SUPABASE_URL ||
    process.env.REACT_APP_SUPABASE_URL ||
    process.env.supabase_url ||
    'https://drbhaydqdlupzpyngkdl.supabase.co'
  ).trim();

  const supabaseAnonKey = (
    process.env.SUPABASE_ANON_KEY ||
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
    process.env.VITE_SUPABASE_ANON_KEY ||
    process.env.REACT_APP_SUPABASE_ANON_KEY ||
    process.env.supabase_anon_key ||
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRyYmhheWRxZGx1cHpweW5na2RsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE2NTAwNTMsImV4cCI6MjEwNzIyNjA1M30.ITyIGFcp5JpLU5sz_TVFQZOG8KoBe8KeQ8ydKZjVBG4'
  ).trim();

  if (!supabaseUrl || !supabaseAnonKey) {
    return res.status(500).json({ ok: false, error: 'missing_configuration' });
  }

  const token = req.body && req.body.p_token;
  if (!token) {
    return res.status(400).json({ ok: false, error: 'missing_token' });
  }

  try {
    const cleanUrl = supabaseUrl.replace(/\/+$/, '');
    const rpcUrl = `${cleanUrl}/rest/v1/rpc/resolve_invitation`;
    const response = await fetch(rpcUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'apikey': supabaseAnonKey,
        'Authorization': `Bearer ${supabaseAnonKey}`
      },
      body: JSON.stringify({ p_token: token })
    });
    const data = await response.json();
    return res.status(response.status).json(data);
  } catch (err) {
    return res.status(500).json({ ok: false, error: err.message || 'server_error' });
  }
};
