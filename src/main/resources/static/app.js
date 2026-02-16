const apiBase = '';

function log(msg){
  const el = document.getElementById('log');
  const p = document.createElement('div');
  p.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
  el.prepend(p);
}

async function fetchJson(path){
  const res = await fetch(apiBase + path);
  return res.json();
}

function encodeName(name){ return encodeURIComponent(name); }

async function refreshWatchParties(){
  try{
    const wps = await fetchJson('/api/watchparties');
    const list = document.getElementById('wp-list');
    const sel = document.getElementById('bet-wp');
    list.innerHTML = '';
    sel.innerHTML = '';
    wps.forEach(wp => {
      const li = document.createElement('li');
      li.textContent = wp.name + (wp.creator? ` (creator=${wp.creator.name})` : '');
      list.appendChild(li);
      const opt = document.createElement('option');
      opt.value = wp.name; opt.textContent = wp.name;
      sel.appendChild(opt);
    });
    log('WatchParties rafraîchis');
  }catch(e){ log('Erreur fetch WP: '+e); }
}

async function createWatchParty(){
  const name = document.getElementById('wp-name').value || `WP_${Date.now()}`;
  const user = document.getElementById('wp-user').value || 'alice';
  const type = document.getElementById('wp-type').value || 'TEAM';
  const payload = { user, name, type };
  const r = await fetch('/api/watchparties',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await r.text();
  log('Create WP → '+text);
  await refreshWatchParties();
}

async function createBet(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin').value || 'alice';
  const question = document.getElementById('bet-question').value || 'Who wins?';
  const choices = (document.getElementById('bet-choices').value || 'Team A,Team B').split(',').map(s=>s.trim());
  const minutes = parseInt(document.getElementById('bet-minutes').value || '5',10);
  const payload = { admin, question, choices, votingMinutes: minutes };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/discrete`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Create Bet → '+text);
}

async function vote(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('vote-user').value || 'bob';
  const value = document.getElementById('vote-value').value || 'Team A';
  const points = parseInt(document.getElementById('vote-points').value || '10',10);
  const payload = { user, value, points };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/vote`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Vote → '+text);
}

async function endVoting(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin-action').value || 'alice';
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/end-voting`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({admin})});
  const text = await res.text();
  log('End voting → '+text);
}

async function resolveBet(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin-action').value || 'alice';
  const correctValue = document.getElementById('resolve-value').value || 'Team A';
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/resolve`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({admin, correctValue})});
  const text = await res.text();
  log('Resolve → '+text);
}

async function refreshRankings(){
  const r = await fetchJson('/api/rankings/public/points');
  document.getElementById('rankings').textContent = JSON.stringify(r, null, 2);
  log('Rankings rafraîchis');
}

async function lookupUser(){
  const u = document.getElementById('lookup-user').value || 'alice';
  const res = await fetch(`/api/users/${encodeURIComponent(u)}`);
  const json = await res.json();
  document.getElementById('user-result').textContent = JSON.stringify(json, null, 2);
  log(`User ${u} fetched`);
}

function bind(){
  document.getElementById('btn-refresh-wp').onclick = refreshWatchParties;
  document.getElementById('btn-create-wp').onclick = createWatchParty;
  document.getElementById('btn-create-bet').onclick = createBet;
  document.getElementById('btn-vote').onclick = vote;
  document.getElementById('btn-end-voting').onclick = endVoting;
  document.getElementById('btn-resolve').onclick = resolveBet;
  document.getElementById('btn-refresh-rank').onclick = refreshRankings;
  document.getElementById('btn-lookup-user').onclick = lookupUser;
}

window.addEventListener('DOMContentLoaded', async () => { bind(); await refreshWatchParties(); await refreshRankings(); log('UI ready'); });