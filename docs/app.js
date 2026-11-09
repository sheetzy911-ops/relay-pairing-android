const $ = (id) => document.getElementById(id);
const state = { relayUrl: sessionStorage.getItem('relayUrl') || '', browserToken: sessionStorage.getItem('browserToken') || '' };

$('relayUrl').value = state.relayUrl;
$('browserToken').value = state.browserToken;

function setStatus(text, kind = '') { $('connectionStatus').textContent = text; $('connectionStatus').className = `form-status ${kind}`; }
function setMessageStatus(text, kind = '') { $('messageStatus').textContent = text; $('messageStatus').className = `form-status ${kind}`; }
function apiUrl(path) { return `${state.relayUrl.replace(/\/$/, '')}${path}`; }
function headers() { return { 'content-type': 'application/json', ...(state.browserToken ? { 'x-api-key': state.browserToken } : {}) }; }
function connected(ok) { $('connectionState').className = `status-pill ${ok ? 'online' : 'offline'}`; $('connectionState').innerHTML = `<i></i> ${ok ? 'Relay connected' : 'Not connected'}`; }
function addActivity(label, detail, kind = '') { const item = document.createElement('div'); item.className = `activity-item ${kind}`; item.innerHTML = `<span class="activity-dot"></span><div><b>${label}</b><small>${detail}</small></div>`; const empty = $('activity').querySelector('.empty'); if (empty) empty.remove(); $('activity').prepend(item); }

async function checkRelay() {
  if (!state.relayUrl) throw new Error('Enter the relay HTTPS URL first.');
  if (!state.relayUrl.startsWith('https://') && !state.relayUrl.includes('127.0.0.1')) throw new Error('Use an HTTPS relay URL.');
  const response = await fetch(apiUrl('/health'), { headers: headers() });
  if (!response.ok) throw new Error(`Health check failed with HTTP ${response.status}.`);
  const body = await response.json(); connected(true); addActivity('Relay online', `${body.outstanding ?? 0} outstanding message(s)`, 'success'); return body;
}

$('connectionForm').addEventListener('submit', async (event) => {
  event.preventDefault(); state.relayUrl = $('relayUrl').value.trim(); state.browserToken = $('browserToken').value.trim();
  sessionStorage.setItem('relayUrl', state.relayUrl); sessionStorage.setItem('browserToken', state.browserToken);
  setStatus('Checking relay…');
  try { await checkRelay(); setStatus('Connected for this browser session.', 'success'); } catch (error) { connected(false); setStatus(error.message, 'error'); }
});

$('messageBody').addEventListener('input', () => { $('counter').textContent = `${$('messageBody').value.length} / 1600`; });
$('messageForm').addEventListener('submit', async (event) => {
  event.preventDefault(); setMessageStatus('Queueing…');
  try {
    if (!state.relayUrl) throw new Error('Connect the relay first.');
    const to = $('recipient').value.trim(); const body = $('messageBody').value.trim(); const sim = $('sim').value;
    if (!/^\+[1-9]\d{7,14}$/.test(to)) throw new Error('Use an E.164 recipient number.');
    const response = await fetch(apiUrl('/bridge/messages'), { method: 'POST', headers: headers(), body: JSON.stringify({ to, body, sim }) });
    const result = await response.json().catch(() => ({})); if (!response.ok) throw new Error(result.error || `Queue failed with HTTP ${response.status}.`);
    setMessageStatus(`Queued ${result.id}. The paired Android device will fetch it.`, 'success'); addActivity('SMS queued', `${sim} → ${to}`, 'success'); $('messageForm').reset(); $('counter').textContent = '0 / 1600';
  } catch (error) { setMessageStatus(error.message, 'error'); }
});
