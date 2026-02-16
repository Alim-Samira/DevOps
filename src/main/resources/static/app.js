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
      const isPublic = wp.public === true || wp.isPublic === true;
      const typeLabel = wp.autoConfig ? `auto(${wp.autoConfig.type}${wp.autoConfig.target? ':'+wp.autoConfig.target : ''})` : (isPublic ? 'public' : 'private');
      li.textContent = `${wp.name} — ${typeLabel}` + (wp.creator? ` (creator=${wp.creator.name})` : '');
      list.appendChild(li);
      const opt = document.createElement('option');
      opt.value = wp.name; opt.textContent = wp.name;
      opt.dataset.creator = wp.creator ? wp.creator.name : '';
      sel.appendChild(opt);
    });
    if (sel.options.length > 0) {
      sel.selectedIndex = 0;
      setWpAdminFromSelector();
    }
    log('WatchParties rafraîchis');
  }catch(e){ log('Erreur fetch WP: '+e); }
}

function setWpAdminFromSelector(){
  const sel = document.getElementById('bet-wp');
  if (!sel) return;
  const opt = sel.options[sel.selectedIndex];
  if (!opt) return;
  const creator = opt.dataset && opt.dataset.creator;
  if (creator && creator.length>0) {
    const adminEl = document.getElementById('bet-admin');
    const adminActionEl = document.getElementById('bet-admin-action');
    if (adminEl) adminEl.value = creator;
    if (adminActionEl) adminActionEl.value = creator;
  }
}

async function createWatchParty(){
  const name = document.getElementById('wp-name').value || `WP_${Date.now()}`;
  const user = document.getElementById('wp-user').value || 'alice';
  const mode = document.getElementById('wp-mode').value;

  let res;
  if (mode === 'AUTO') {
    const type = document.getElementById('wp-type').value || 'TEAM';
    const payload = { user, name, type };
    res = await fetch('/api/watchparties',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  } else if (mode === 'PUBLIC') {
    const game = document.getElementById('wp-game').value || 'League of Legends';
    const date = document.getElementById('wp-date').value || '';
    const payload = { name, game, date, user };
    res = await fetch('/api/watchparties/public',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  } else { // PRIVATE
    const game = document.getElementById('wp-game').value || 'League of Legends';
    const date = document.getElementById('wp-date').value || '';
    const payload = { name, game, date, user };
    res = await fetch('/api/watchparties/private',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  }

  const text = await res.text();
  log('Create WP → '+text);
  await refreshWatchParties();
  await refreshWatchPartyRanking();
  updateChatWPSelector();
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

async function joinWatchParty(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('join-user').value || 'bob';
  const res = await fetch(`/api/watchparties/${encodeName(name)}/join`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({user})});
  const text = await res.text();
  log('Join → '+text);
  await refreshWatchParties();
  await refreshWatchPartyRanking();
  await refreshRankings();
  updateChatWPSelector();
}

async function leaveWatchParty(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('join-user').value || 'bob';
  const res = await fetch(`/api/watchparties/${encodeName(name)}/leave`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({user})});
  const text = await res.text();
  log('Leave → '+text);
  await refreshWatchParties();
  await refreshWatchPartyRanking();
  await refreshRankings();
  updateChatWPSelector();
}

async function refreshRankings(){
  // legacy button -> refresh global ranking (force server recompute)
  const r = await fetchJson('/api/rankings/public/points?refresh=true');
  const txt = JSON.stringify(r, null, 2);
  // keep backward-compatible place and new UI element
  const legacyEl = document.getElementById('rankings');
  if (legacyEl) legacyEl.textContent = txt;
  document.getElementById('global-ranking').textContent = txt;
  log('Classement global rafraîchi (points)');
}

async function refreshWatchPartyRanking(){
  const sel = document.getElementById('bet-wp');
  const name = sel && sel.value;
  if (!name) {
    document.getElementById('wp-ranking').textContent = 'Aucune watchparty sélectionnée';
    return;
  }
  try {
    const r = await fetchJson(`/api/watchparties/${encodeName(name)}/rankings/points?refresh=true`);
    document.getElementById('wp-ranking').textContent = JSON.stringify(r, null, 2);
    log(`Classement WP "${name}" rafraîchi`);
  } catch(e) { log('Erreur fetch ranking WP: '+e); }
}

async function lookupUser(){
  const u = document.getElementById('lookup-user').value || 'alice';
  const res = await fetch(`/api/users/${encodeURIComponent(u)}`);
  const json = await res.json();
  document.getElementById('user-result').textContent = JSON.stringify(json, null, 2);
  log(`User ${u} fetched`);
}

function updateChatWPSelector() {
  const betWpSelect = document.getElementById('bet-wp');
  const chatWpSelect = document.getElementById('chat-wp');
  
  // Copier les options
  chatWpSelect.innerHTML = '<option value="">-- Sélectionner une WP --</option>';
  for (const opt of betWpSelect.options) {
    if (opt.value !== '') {
      const newOpt = document.createElement('option');
      newOpt.value = opt.value;
      newOpt.textContent = opt.textContent;
      chatWpSelect.appendChild(newOpt);
    }
  }
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

async function loadWatchPartyChat() {
  const wpName = document.getElementById('chat-wp').value;
  if (!wpName) {
    log('Sélectionnez une watch party');
    return;
  }

  try {
    const messages = await fetchJson(`/api/watchparties/${encodeName(wpName)}/chat`);
    const container = document.getElementById('chat-messages');
    if (!messages || messages.length === 0) {
      container.innerHTML = '<div style="color: #999; font-size: 0.9rem;">Aucun message</div>';
      return;
    }
    container.innerHTML = messages.map(m => 
      `<div style="margin-bottom: 8px; padding: 6px; background: #f5f5f5; border-radius: 3px; border-left: 3px solid #007bff;">
        <strong>${escapeHtml(m.user?.name || 'System')}</strong>: ${escapeHtml(m.content || m.text)}<br/>
        <small style="color: #666;">${m.timestamp || ''}</small>
      </div>`
    ).join('');
    // Auto-scroll to bottom
    container.scrollTop = container.scrollHeight;
    log('Chat chargé');
  } catch(e) {
    log('Erreur chargement chat: ' + e);
  }
}

async function sendChatMessage() {
  const wpName = document.getElementById('chat-wp').value;
  const user = document.getElementById('chat-user').value;
  const text = document.getElementById('chat-text').value;

  if (!wpName) {
    log('Sélectionnez une watch party');
    return;
  }
  if (!user || !text) {
    log('Remplissez user et message');
    return;
  }

  try {
    const res = await fetch(`/api/watchparties/${encodeName(wpName)}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ user, text })
    });
    const result = await res.text();
    log(result);
    document.getElementById('chat-text').value = '';
    await loadWatchPartyChat(); // Rafraîchir
  } catch(e) {
    log('Erreur envoi message: ' + e);
  }
}

function toggleWpMode(){
  const mode = document.getElementById('wp-mode').value;
  const wpType = document.getElementById('wp-type');
  const wpGame = document.getElementById('wp-game');
  const wpDate = document.getElementById('wp-date');
  const createBtn = document.getElementById('btn-create-wp');
  if (mode === 'AUTO') {
    wpType.style.display = '';
    wpGame.style.display = 'none';
    wpDate.style.display = 'none';
    createBtn.textContent = 'Créer (auto)';
  } else {
    wpType.style.display = 'none';
    wpGame.style.display = '';
    wpDate.style.display = '';
    createBtn.textContent = mode === 'PUBLIC' ? 'Créer (public)' : 'Créer (privé)';
  }
}

function bind(){
  document.getElementById('btn-refresh-wp').onclick = refreshWatchParties;
  document.getElementById('btn-create-wp').onclick = createWatchParty;
  document.getElementById('btn-create-bet').onclick = createBet;
  document.getElementById('btn-vote').onclick = vote;
  document.getElementById('btn-end-voting').onclick = endVoting;
  document.getElementById('btn-resolve').onclick = resolveBet;
  document.getElementById('btn-join-wp').onclick = joinWatchParty;
  document.getElementById('btn-leave-wp').onclick = leaveWatchParty;
  document.getElementById('btn-refresh-rank').onclick = refreshRankings;
  document.getElementById('btn-refresh-wp-rank').onclick = refreshWatchPartyRanking;
  document.getElementById('bet-wp').onchange = () => { refreshWatchPartyRanking(); setWpAdminFromSelector(); updateChatWPSelector(); };
  document.getElementById('wp-mode').onchange = toggleWpMode;
  document.getElementById('btn-lookup-user').onclick = lookupUser;
  document.getElementById('btn-load-chat').onclick = loadWatchPartyChat;
  document.getElementById('btn-send-chat').onclick = sendChatMessage;
}

window.addEventListener('DOMContentLoaded', async () => { bind(); toggleWpMode(); await refreshWatchParties(); updateChatWPSelector(); await refreshRankings(); await refreshWatchPartyRanking(); log('UI ready'); });