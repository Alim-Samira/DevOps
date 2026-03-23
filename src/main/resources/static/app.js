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

function parseCsv(input) {
  return (input || '')
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0);
}

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
      let typeLabel = 'private';
      if (wp.autoConfig) {
        const autoTarget = wp.autoConfig.target ? `:${wp.autoConfig.target}` : '';
        typeLabel = `auto(${wp.autoConfig.type}${autoTarget})`;
      } else if (isPublic) {
        typeLabel = 'public';
      }
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
  const creator = opt.dataset?.creator;
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
    const addToCalendar = document.getElementById('wp-add-calendar').checked === true;
    const calendarConnectionId = document.getElementById('wp-calendar-connection-id').value || '';
    const payload = { name, game, date, user, addToCalendar, calendarConnectionId };
    res = await fetch('/api/watchparties/public',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  } else { // PRIVATE
    const game = document.getElementById('wp-game').value || 'League of Legends';
    const date = document.getElementById('wp-date').value || '';
    const addToCalendar = document.getElementById('wp-add-calendar').checked === true;
    const calendarConnectionId = document.getElementById('wp-calendar-connection-id').value || '';
    const payload = { name, game, date, user, addToCalendar, calendarConnectionId };
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
  const minutes = Number.parseInt(document.getElementById('bet-minutes').value || '5',10);
  const payload = { admin, question, choices, votingMinutes: minutes };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/discrete`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Create Bet → '+text);
}

async function createNumericBet(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin').value || 'alice';
  const question = document.getElementById('bet-question-num').value || 'Numeric question?';
  const isInteger = document.getElementById('bet-num-int').checked === true;
  const minStr = document.getElementById('bet-num-min').value;
  const maxStr = document.getElementById('bet-num-max').value;
  const minutes = Number.parseInt(document.getElementById('bet-minutes-num').value || '5',10);
  const payload = { admin, question, isInteger, votingMinutes: minutes };
  if (minStr !== '') {
    const minVal = Number.parseFloat(minStr);
    if (Number.isNaN(minVal)) {
      log('Valeur min invalide');
      return;
    }
    payload.minValue = minVal;
  }
  if (maxStr !== '') {
    const maxVal = Number.parseFloat(maxStr);
    if (Number.isNaN(maxVal)) {
      log('Valeur max invalide');
      return;
    }
    payload.maxValue = maxVal;
  }
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/numeric`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Create Numeric Bet → '+text);
}

async function createRankingBet(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin').value || 'alice';
  const question = document.getElementById('bet-question-rank').value || 'Ranking question?';
  const items = parseCsv(document.getElementById('bet-items').value || 'A,B,C');
  const minutes = Number.parseInt(document.getElementById('bet-minutes-rank').value || '5',10);
  const payload = { admin, question, items, votingMinutes: minutes };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/ranking`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Create Ranking Bet → '+text);
}

async function vote(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('vote-user').value || 'bob';
  const value = document.getElementById('vote-value').value || 'Team A';
  const points = Number.parseInt(document.getElementById('vote-points').value || '10',10);
  const payload = { user, value, points };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/vote`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Vote → '+text);
}

async function voteNumeric(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('vote-user').value || 'bob';
  const valStr = document.getElementById('vote-value-num').value;
  const value = Number.parseFloat(valStr);
  const points = Number.parseInt(document.getElementById('vote-points').value || '10',10);
  if (Number.isNaN(value)) {
    log('Valeur numerique invalide');
    return;
  }
  const payload = { user, value, points };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/vote`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Vote Numeric → '+text);
}

async function voteRanking(){
  const name = document.getElementById('bet-wp').value;
  const user = document.getElementById('vote-user').value || 'bob';
  const value = parseCsv(document.getElementById('vote-value-rank').value);
  const points = Number.parseInt(document.getElementById('vote-points').value || '10',10);
  if (value.length === 0) {
    log('Classement invalide');
    return;
  }
  const payload = { user, value, points };
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/vote`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
  const text = await res.text();
  log('Vote Ranking → '+text);
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

async function resolveNumeric(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin-action').value || 'alice';
  const valStr = document.getElementById('resolve-value-num').value;
  const correctValue = Number.parseFloat(valStr);
  if (Number.isNaN(correctValue)) {
    log('Valeur numerique invalide');
    return;
  }
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/resolve`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({admin, correctValue})});
  const text = await res.text();
  log('Resolve Numeric → '+text);
}

async function resolveRanking(){
  const name = document.getElementById('bet-wp').value;
  const admin = document.getElementById('bet-admin-action').value || 'alice';
  const correctValue = parseCsv(document.getElementById('resolve-value-rank').value);
  if (correctValue.length === 0) {
    log('Classement invalide');
    return;
  }
  const res = await fetch(`/api/watchparties/${encodeName(name)}/bets/resolve`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({admin, correctValue})});
  const text = await res.text();
  log('Resolve Ranking → '+text);
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
  const name = sel?.value;
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
    .replaceAll(/&/g, '&amp;')
    .replaceAll(/</g, '&lt;')
    .replaceAll(/>/g, '&gt;')
    .replaceAll(/"/g, '&quot;')
    .replaceAll(/'/g, '&#039;');
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
      `<div style="margin-bottom: 8px; padding: 6px; background: #e8f0ff; border-radius: 3px; border-left: 3px solid #007bff; color: #000;">
        <strong style="color: #0056b3;">${escapeHtml(m.senderName || m.sender?.name || 'System')}</strong>: <span style="color: #222;">${escapeHtml(m.content || m.text)}</span><br/>
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
  const wpCalendarLabel = document.getElementById('wp-calendar-label');
  const wpCalendarConnectionId = document.getElementById('wp-calendar-connection-id');
  const createBtn = document.getElementById('btn-create-wp');
  if (mode === 'AUTO') {
    wpType.style.display = '';
    wpGame.style.display = 'none';
    wpDate.style.display = 'none';
    wpCalendarLabel.style.display = 'none';
    wpCalendarConnectionId.style.display = 'none';
    createBtn.textContent = 'Créer (auto)';
  } else {
    wpType.style.display = 'none';
    wpGame.style.display = '';
    wpDate.style.display = '';
    wpCalendarLabel.style.display = 'inline-flex';
    wpCalendarConnectionId.style.display = '';
    createBtn.textContent = mode === 'PUBLIC' ? 'Créer (public)' : 'Créer (privé)';
  }
}

function toggleCalendarProviderFields() {
  const provider = document.getElementById('cal-provider').value;
  const url = document.getElementById('cal-url');
  const token = document.getElementById('cal-token');
  const externalId = document.getElementById('cal-external-id');

  if (provider === 'GOOGLE') {
    url.style.display = 'none';
    token.style.display = '';
    externalId.style.display = '';
  } else {
    url.style.display = '';
    token.style.display = 'none';
    externalId.style.display = 'none';
  }
}

// === Calendar Functions ===

async function connectCalendar() {
  const user = document.getElementById('cal-user').value.trim();
  const provider = document.getElementById('cal-provider').value;
  const url = document.getElementById('cal-url').value.trim();
  const token = document.getElementById('cal-token').value.trim();
  const externalCalendarId = document.getElementById('cal-external-id').value.trim();

  if (!user) {
    log('Remplissez username');
    return;
  }

  if (provider === 'GOOGLE' && !token) {
    log('Remplissez le token OAuth Google');
    return;
  }

  if (provider === 'ICAL' && !url) {
    log('Remplissez l URL ICAL');
    return;
  }

  try {
    let payload;
    if (provider === 'GOOGLE') {
      payload = { provider, oauthAccessToken: token, externalCalendarId };
    } else {
      payload = { provider, sourceUrl: url };
    }
    const res = await fetch(`/api/users/${encodeURIComponent(user)}/calendars`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    log('Connect Calendar → ' + JSON.stringify(result));
    if (result.success) {
      await listCalendars();
    }
  } catch(e) {
    log('Erreur connexion calendar: ' + e);
  }
}

async function listCalendars() {
  const user = document.getElementById('cal-list-user').value.trim();
  if (!user) {
    log('Remplissez username');
    return;
  }

  try {
    const calendars = await fetchJson(`/api/users/${encodeURIComponent(user)}/calendars`);
    const list = document.getElementById('cal-list');
    list.innerHTML = '';
    if (!calendars || calendars.length === 0) {
      list.innerHTML = '<li style="color: #999;">Aucun calendrier connecté</li>';
      return;
    }
    calendars.forEach(cal => {
      const li = document.createElement('li');
      let details = cal.sourceUrl;
      if (cal.type === 'GOOGLE') {
        details = cal.calendarId || 'primary';
      }
      li.textContent = `${cal.id} - ${cal.type} (${details})`;
      list.appendChild(li);
    });
    log(`Calendriers de ${user} listés`);
  } catch(e) {
    log('Erreur list calendars: ' + e);
  }
}

async function deleteCalendar() {
  const user = document.getElementById('cal-delete-user').value.trim();
  const connectionId = document.getElementById('cal-connection-id').value.trim();

  if (!user || !connectionId) {
    log('Remplissez username et connection ID');
    return;
  }

  try {
    const res = await fetch(`/api/users/${encodeURIComponent(user)}/calendars/${encodeURIComponent(connectionId)}`, {
      method: 'DELETE'
    });
    const result = await res.json();
    log('Delete Calendar → ' + JSON.stringify(result));
    if (result.success) {
      await listCalendars();
    }
  } catch(e) {
    log('Erreur suppression calendar: ' + e);
  }
}

async function getCalendarEvents() {
  const user = document.getElementById('cal-events-user').value.trim();
  const connectionId = document.getElementById('cal-events-connection-id').value.trim();
  const start = document.getElementById('cal-events-start').value;
  const end = document.getElementById('cal-events-end').value;

  if (!user || !connectionId || !start || !end) {
    log('Remplissez tous les champs');
    return;
  }

  try {
    const result = await fetchJson(`/api/users/${encodeURIComponent(user)}/calendars/${encodeURIComponent(connectionId)}/events?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
    const resultEl = document.getElementById('cal-events-result');
    if (result.success) {
      resultEl.textContent = JSON.stringify(result.events, null, 2);
      log(`Événements récupérés: ${result.count}`);
    } else {
      resultEl.textContent = 'Erreur: ' + JSON.stringify(result);
    }
  } catch(e) {
    log('Erreur récupération événements: ' + e);
    document.getElementById('cal-events-result').textContent = 'Erreur: ' + e;
  }
}

async function loadNotifications() {
  const user = document.getElementById('notif-user').value.trim();
  if (!user) {
    log('Remplissez username');
    return;
  }

  try {
    const notifications = await fetchJson(`/api/users/${encodeURIComponent(user)}/notifications`);
    const list = document.getElementById('notif-list');
    list.innerHTML = '';
    if (!notifications || notifications.length === 0) {
      list.innerHTML = '<li style="color: #999;">Aucune notification</li>';
      return;
    }
    notifications.forEach(notif => {
      const li = document.createElement('li');
      li.style.cssText = 'margin-bottom: 10px; padding: 8px; background: #f0f8ff; border-left: 3px solid #007bff; border-radius: 3px;';
      li.innerHTML = `
        <strong>${escapeHtml(notif.title)}</strong><br/>
        <span style="color: #555;">${escapeHtml(notif.message)}</span><br/>
        <small style="color: #888;">${notif.createdAt ? new Date(notif.createdAt).toLocaleString('fr-FR') : ''}</small>
        ${notif.watchPartyName ? `<br/><em>WatchParty: ${escapeHtml(notif.watchPartyName)}</em>` : ''}
      `;
      list.appendChild(li);
    });
    log(`Notifications de ${user} chargées (${notifications.length})`);
  } catch(e) {
    log('Erreur chargement notifications: ' + e);
  }
}

function bind(){
  document.getElementById('btn-refresh-wp').onclick = refreshWatchParties;
  document.getElementById('btn-create-wp').onclick = createWatchParty;
  document.getElementById('btn-create-bet').onclick = createBet;
  document.getElementById('btn-create-bet-num').onclick = createNumericBet;
  document.getElementById('btn-create-bet-rank').onclick = createRankingBet;
  document.getElementById('btn-vote').onclick = vote;
  document.getElementById('btn-vote-num').onclick = voteNumeric;
  document.getElementById('btn-vote-rank').onclick = voteRanking;
  document.getElementById('btn-end-voting').onclick = endVoting;
  document.getElementById('btn-resolve').onclick = resolveBet;
  document.getElementById('btn-resolve-num').onclick = resolveNumeric;
  document.getElementById('btn-resolve-rank').onclick = resolveRanking;
  document.getElementById('btn-join-wp').onclick = joinWatchParty;
  document.getElementById('btn-leave-wp').onclick = leaveWatchParty;
  document.getElementById('btn-refresh-rank').onclick = refreshRankings;
  document.getElementById('btn-refresh-wp-rank').onclick = refreshWatchPartyRanking;
  document.getElementById('bet-wp').onchange = () => { refreshWatchPartyRanking(); setWpAdminFromSelector(); updateChatWPSelector(); };
  document.getElementById('wp-mode').onchange = toggleWpMode;
  document.getElementById('cal-provider').onchange = toggleCalendarProviderFields;
  document.getElementById('btn-lookup-user').onclick = lookupUser;
  document.getElementById('btn-load-chat').onclick = loadWatchPartyChat;
  document.getElementById('btn-send-chat').onclick = sendChatMessage;

  // Calendar bindings
  document.getElementById('btn-connect-calendar').onclick = connectCalendar;
  document.getElementById('btn-list-calendars').onclick = listCalendars;
  document.getElementById('btn-delete-calendar').onclick = deleteCalendar;
  document.getElementById('btn-get-events').onclick = getCalendarEvents;

  // Notification bindings
  document.getElementById('btn-load-notifications').onclick = loadNotifications;
}

globalThis.addEventListener('DOMContentLoaded', async () => { bind(); toggleWpMode(); toggleCalendarProviderFields(); await refreshWatchParties(); updateChatWPSelector(); await refreshRankings(); await refreshWatchPartyRanking(); log('UI ready'); });
