'use strict';

/**
 * Mikey Note Studio — Front-End Application Controller
 * Handles REST synchronization with Java 24 + SQLite backend,
 * modern UI/UX interactions, rich text ribbon, SVG charts, and telemetry.
 */

const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];
const uid = () => Math.random().toString(36).slice(2, 9);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

const KEY = 'notedown-v2';
const COLORS = [
  '#2563eb', // Royal Azure
  '#dc2626', // Crimson
  '#16a34a', // Emerald Forest
  '#7c3aed', // Amethyst Purple
  '#ea580c', // Sunset Amber
  '#0d9488', // Ocean Teal
  '#334155'  // Slate Titanium
];
const PAL = ['#2563eb', '#e11d48', '#16a34a', '#f59e0b', '#7c3aed', '#0891b2', '#db2777', '#65a30d', '#ea580c', '#475569'];

let S = { books: [] };
let isServerOnline = false;
let book = null;
let lesson = null;
let rng = null;
let armed = null;
let isSaving = false;
let globalSearchQuery = '';
const sheet = $('#sheet');

/* ==========================================================================
   1. Theme Management (Dark / Light)
   ========================================================================== */
function initTheme() {
  const saved = localStorage.getItem('notedown-theme') || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  applyTheme(saved);

  const toggleBtn = $('#btn-theme-toggle');
  if (toggleBtn) {
    toggleBtn.onclick = () => {
      const isDark = document.body.classList.contains('dark-theme');
      applyTheme(isDark ? 'light' : 'dark');
    };
  }
}

function applyTheme(theme) {
  if (theme === 'dark') {
    document.body.classList.add('dark-theme');
    document.documentElement.setAttribute('data-theme', 'dark');
    const b = $('#btn-theme-toggle');
    if (b) b.textContent = '☀️';
  } else {
    document.body.classList.remove('dark-theme');
    document.documentElement.setAttribute('data-theme', 'light');
    const b = $('#btn-theme-toggle');
    if (b) b.textContent = '🌙';
  }
  localStorage.setItem('notedown-theme', theme);
}

/* ==========================================================================
   2. API Communication Layer
   ========================================================================== */
async function api(path, options = {}) {
  try {
    const res = await fetch(path, {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `HTTP ${res.status}`);
    }
    const json = await res.json();
    return json.data !== undefined ? json.data : json;
  } catch (e) {
    console.warn(`[API] Request to ${path} failed:`, e.message);
    throw e;
  }
}

/* ==========================================================================
   3. Adobe Creative Boot Splash Screen
   ========================================================================== */
const splashMessages = [
  'Connecting to Java SQLite Server…',
  'Verifying ACID Storage Engine (WAL mode)…',
  'Mounting Mikey Note Document Pipeline…',
  'Ready!'
];

let splashStep = 0;
const splashInterval = setInterval(() => {
  splashStep++;
  const sbar = $('#sbar');
  const smsg = $('#smsg');
  if (sbar) sbar.style.width = Math.min(100, splashStep * 28) + '%';
  if (smsg) smsg.textContent = splashMessages[Math.min(splashStep, splashMessages.length - 1)];

  if (splashStep >= 4) {
    clearInterval(splashInterval);
    initApp();
  }
}, 200);

async function initApp() {
  initTheme();
  setupGlobalSearch();

  try {
    const books = await api('/api/books');
    isServerOnline = true;
    S.books = books.map(b => ({
      id: b.id,
      title: b.title,
      color: b.color || '#2563eb',
      lessons: [],
      lessonCount: b.lessonCount || 0
    }));
  } catch (err) {
    console.warn('[Mikey Note] Server offline; running in local storage fallback mode.');
    isServerOnline = false;
    try {
      const local = JSON.parse(localStorage.getItem(KEY));
      if (local && local.books) S = local;
    } catch (e) {}
  }

  const splash = $('#splash');
  if (splash) {
    splash.style.opacity = '0';
    setTimeout(() => {
      splash.remove();
      showHome();
    }, 350);
  } else {
    showHome();
  }
}

/* ==========================================================================
   4. Modern Custom Modal & Centered Alert System
   ========================================================================== */
function getModalIconSvg(type) {
  if (type === 'success') {
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
  }
  if (type === 'warning' || type === 'danger') {
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
  }
  if (type === 'confirm') {
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
  }
  return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
}

function showModernAlert(title, message, type = 'info') {
  return new Promise(resolve => {
    const iconSvg = getModalIconSvg(type);
    const o = document.createElement('div');
    o.className = 'ov modern-alert-overlay';
    o.innerHTML = `
      <div class="dg modern-alert-card" role="alertdialog">
        <div class="modern-alert-header">
          <div class="modern-alert-icon ${type}">${iconSvg}</div>
          <h3>${esc(title)}</h3>
        </div>
        <div class="modern-alert-body">
          <p>${esc(message).replace(/\n/g, '<br>')}</p>
        </div>
        <div class="ft" style="display:flex;justify-content:flex-end;">
          <button class="ok btn-primary" style="min-width:90px;">OK</button>
        </div>
      </div>`;
    document.body.appendChild(o);

    const close = () => {
      o.classList.add('closing');
      setTimeout(() => o.remove(), 160);
      resolve(true);
    };

    o.querySelector('.ok').onclick = close;
    o.onmousedown = e => { if (e.target === o) close(); };
    o.addEventListener('keydown', e => {
      if (e.key === 'Escape' || e.key === 'Enter') close();
    });
    setTimeout(() => o.querySelector('.ok').focus(), 40);
  });
}

function showModernConfirm(title, message, type = 'warning', confirmText = 'Confirm', cancelText = 'Cancel') {
  return new Promise(resolve => {
    const iconSvg = getModalIconSvg(type);
    const o = document.createElement('div');
    o.className = 'ov modern-alert-overlay';
    o.innerHTML = `
      <div class="dg modern-alert-card" role="dialog">
        <div class="modern-alert-header">
          <div class="modern-alert-icon ${type}">${iconSvg}</div>
          <h3>${esc(title)}</h3>
        </div>
        <div class="modern-alert-body">
          <p>${esc(message).replace(/\n/g, '<br>')}</p>
        </div>
        <div class="ft" style="display:flex;justify-content:flex-end;gap:8px;">
          <button class="c" style="min-width:80px;">${esc(cancelText)}</button>
          <button class="ok btn-primary ${type === 'danger' || type === 'warning' ? 'btn-danger' : ''}" style="min-width:90px;">${esc(confirmText)}</button>
        </div>
      </div>`;
    document.body.appendChild(o);

    const close = (result) => {
      o.classList.add('closing');
      setTimeout(() => o.remove(), 160);
      resolve(result);
    };

    o.querySelector('.c').onclick = () => close(false);
    o.querySelector('.ok').onclick = () => close(true);
    o.onmousedown = e => { if (e.target === o) close(false); };
    o.addEventListener('keydown', e => {
      if (e.key === 'Escape') close(false);
      if (e.key === 'Enter') close(true);
    });
    setTimeout(() => o.querySelector('.ok').focus(), 40);
  });
}

function showModernPrompt(title, message, defaultValue = '', placeholder = '') {
  return new Promise(resolve => {
    const o = document.createElement('div');
    o.className = 'ov modern-alert-overlay';
    o.innerHTML = `
      <div class="dg modern-alert-card" role="dialog">
        <div class="modern-alert-header">
          <div class="modern-alert-icon info">${getModalIconSvg('info')}</div>
          <h3>${esc(title)}</h3>
        </div>
        <div class="modern-alert-body">
          <p>${esc(message).replace(/\n/g, '<br>')}</p>
          <input type="text" id="modern-prompt-input" value="${esc(defaultValue)}" placeholder="${esc(placeholder)}" style="width:100%;margin-top:12px;padding:9px 12px;font-size:14px;border-radius:6px;border:1px solid var(--border-subtle);background:var(--bg-surface-elevated);">
        </div>
        <div class="ft" style="display:flex;justify-content:flex-end;gap:8px;">
          <button class="c">Cancel</button>
          <button class="ok btn-primary">OK</button>
        </div>
      </div>`;
    document.body.appendChild(o);

    const input = o.querySelector('#modern-prompt-input');
    const close = (val) => {
      o.classList.add('closing');
      setTimeout(() => o.remove(), 160);
      resolve(val);
    };

    o.querySelector('.c').onclick = () => close(null);
    o.querySelector('.ok').onclick = () => close(input.value.trim());
    o.onmousedown = e => { if (e.target === o) close(null); };
    o.addEventListener('keydown', e => {
      if (e.key === 'Escape') close(null);
      if (e.key === 'Enter') close(input.value.trim());
    });
    setTimeout(() => { input.focus(); input.select(); }, 40);
  });
}

// Global modal overrides
window.alert = (msg) => showModernAlert('Notice', String(msg), 'info');

function dlg(title, body, onOk, okText = 'OK') {
  const o = document.createElement('div');
  o.className = 'ov';
  o.innerHTML = `
    <div class="dg" role="dialog" aria-label="${esc(title)}">
      <h3>${esc(title)}</h3>
      ${body}
      <div class="ft">
        <button class="c">Cancel</button>
        <button class="ok btn-primary">${okText}</button>
      </div>
    </div>`;
  document.body.appendChild(o);

  const close = () => {
    o.classList.add('closing');
    setTimeout(() => o.remove(), 160);
  };
  const f = o.querySelector('input, textarea');
  if (f) f.focus();

  o.querySelector('.c').onclick = close;
  o.onmousedown = e => { if (e.target === o) close(); };

  const go = async () => {
    const res = await onOk(o);
    if (res !== false) close();
  };

  o.querySelector('.ok').onclick = go;
  o.addEventListener('keydown', e => {
    if (e.key === 'Escape') close();
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') go();
  });
  return o;
}

/* ==========================================================================
   5. Bookshelf & Analytics Dashboard
   ========================================================================== */
function updateHudStats() {
  const totalBooks = S.books.length;
  let totalLessons = 0;
  let totalWords = 0;

  S.books.forEach(b => {
    totalLessons += (b.lessonCount || (b.lessons ? b.lessons.length : 0));
    if (b.lessons) {
      b.lessons.forEach(l => {
        totalWords += (l.wordCount || 0);
      });
    }
  });

  const hb = $('#hud-books');
  const hl = $('#hud-lessons');
  const hw = $('#hud-words');
  const hd = $('#hud-db');

  if (hb) hb.textContent = totalBooks;
  if (hl) hl.textContent = totalLessons;
  if (hw) hw.textContent = totalWords.toLocaleString();
  if (hd) hd.textContent = isServerOnline ? 'WAL Active' : 'Local Fallback';
}

function showHome() {
  book = lesson = null;
  $('#bookview').classList.add('hide');
  $('#home').classList.remove('hide');

  updateHudStats();

  let filteredBooks = S.books;
  if (globalSearchQuery) {
    const q = globalSearchQuery.toLowerCase();
    filteredBooks = S.books.filter(b => b.title.toLowerCase().includes(q));
  }

  const cardsHtml = filteredBooks.map(b => {
    const count = b.lessonCount !== undefined ? b.lessonCount : (b.lessons ? b.lessons.length : 0);
    return `
      <div class="card" tabindex="0" role="button" data-id="${b.id}" style="background:${b.color || '#2563eb'}">
        <div class="bookmark-tag"></div>
        <button class="x" data-x="${b.id}" title="Delete notebook">${armed === b.id ? 'Delete?' : '✕'}</button>
        <b>${esc(b.title)}</b>
        <small>📄 ${count} lesson${count === 1 ? '' : 's'}</small>
      </div>`;
  }).join('');

  $('#shelf').innerHTML = cardsHtml + `
    <div class="card new" tabindex="0" role="button" id="newbook">
      <div class="new-plus-icon">＋</div>
      <div>New Notebook</div>
    </div>`;
}

function setupGlobalSearch() {
  const searchInput = $('#global-search');
  if (!searchInput) return;

  searchInput.addEventListener('input', e => {
    globalSearchQuery = e.target.value.trim();
    showHome();
  });

  // Global Keyboard Shortcut: ⌘K or Ctrl+K to focus search
  document.addEventListener('keydown', e => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      searchInput.focus();
      searchInput.select();
    }
  });
}

$('#shelf').addEventListener('click', async e => {
  const x = e.target.closest('[data-x]');
  if (x) {
    const id = x.dataset.x;
    const b = S.books.find(item => item.id === id);
    const title = b ? b.title : 'this notebook';
    const confirmed = await showModernConfirm(
      'Delete Notebook',
      `Delete notebook '${title}' and all its lessons? This action cannot be undone.`,
      'danger',
      'Delete Notebook',
      'Cancel'
    );
    if (!confirmed) return;
    if (isServerOnline) {
      try {
        await api(`/api/books/${id}`, { method: 'DELETE' });
      } catch (err) {
        showModernAlert('Error', 'Could not delete notebook from SQLite: ' + err.message, 'danger');
      }
    }
    S.books = S.books.filter(b => b.id !== id);
    saveLocalFallback();
    return showHome();
  }
  armed = null;

  if (e.target.closest('#newbook')) return newBook();
  const c = e.target.closest('.card[data-id]');
  if (c) openBook(c.dataset.id);
});

$('#shelf').addEventListener('keydown', e => {
  if (e.key === 'Enter' && e.target.classList.contains('card')) e.target.click();
});

const newBookBtn = $('#btn-new-book');
if (newBookBtn) newBookBtn.onclick = () => newBook();

function newBook() {
  dlg('Create New Notebook',
    `<label>Notebook Title</label>
     <input type="text" id="bn" placeholder="e.g. Science & Engineering Notes">
     <label>Cover Theme Color</label>
     <div class="sw">
       ${COLORS.map((c, i) => `
         <label>
           <input type="radio" name="bc" value="${c}" ${i === 0 ? 'checked' : ''}>
           <span style="background:${c}"></span>
         </label>`).join('')}
     </div>`,
    async o => {
      const t = o.querySelector('#bn').value.trim();
      if (!t) {
        o.querySelector('#bn').focus();
        return false;
      }
      const col = o.querySelector('[name=bc]:checked').value;
      let createdBook = { id: uid(), title: t, color: col, lessons: [] };

      if (isServerOnline) {
        try {
          const res = await api('/api/books', {
            method: 'POST',
            body: JSON.stringify({ title: t, color: col })
          });
          createdBook = { id: res.id, title: res.title, color: res.color, lessons: [] };
        } catch (e) {
          console.warn('[Mikey Note] Fallback to local book creation:', e.message);
        }
      }

      S.books.push(createdBook);
      saveLocalFallback();
      openBook(createdBook.id);
    }, 'Create Notebook');
}

async function openBook(id) {
  let found = S.books.find(b => b.id === id);
  if (!found) return;

  if (isServerOnline) {
    try {
      const full = await api(`/api/books/${id}`);
      found = {
        id: full.id,
        title: full.title,
        color: full.color || '#2563eb',
        lessons: (full.lessons || []).map(l => ({
          id: l.id,
          title: l.title,
          html: l.html,
          wordCount: l.wordCount || 0,
          charCount: l.charCount || 0
        }))
      };
      const idx = S.books.findIndex(b => b.id === id);
      if (idx !== -1) S.books[idx] = found;
    } catch (e) {
      console.warn('Could not fetch book details from server:', e.message);
    }
  }

  book = found;
  if (!book.lessons) book.lessons = [];

  $('#home').classList.add('hide');
  $('#bookview').classList.remove('hide');
  $('#btitle').textContent = book.title;

  const colorDot = $('#book-color-dot');
  if (colorDot) colorDot.style.backgroundColor = book.color || '#2563eb';

  lesson = null;
  renderLessons();
  if (book.lessons[0]) {
    openLesson(book.lessons[0].id);
  } else {
    sheet.classList.add('hide');
    $('#empty').classList.remove('hide');
    stat();
  }
}

$('#back').onclick = () => {
  flush();
  showHome();
};

/* ==========================================================================
   6. Lessons Outline Navigation Sidebar
   ========================================================================== */
function renderLessons() {
  const container = $('#lessons-list');
  if (!container) return;

  container.innerHTML = book.lessons.map(l => `
    <div class="ls ${lesson && lesson.id === l.id ? 'on' : ''}">
      <button class="n" data-o="${l.id}">📄 ${esc(l.title)}</button>
      <button class="m" data-r="${l.id}" title="Rename">✎</button>
      <button class="m" data-d="${l.id}" title="Delete">${armed === l.id ? 'Sure?' : '🗑'}</button>
    </div>`).join('');
}

const addLessonBtn = $('#addL');
if (addLessonBtn) {
  addLessonBtn.onclick = () => {
    dlg('New Lesson',
      `<label>Lesson Title</label>
       <input type="text" id="ln" placeholder="e.g. Chapter 1: Introduction">`,
      async o => {
        const v = o.querySelector('#ln').value.trim();
        if (!v) {
          o.querySelector('#ln').focus();
          return false;
        }
        flush();
        let newLesson = { id: uid(), title: v, html: `<h1>${esc(v)}</h1><p><br></p>` };

        if (isServerOnline) {
          try {
            const res = await api(`/api/books/${book.id}/lessons`, {
              method: 'POST',
              body: JSON.stringify({ title: v, html: newLesson.html })
            });
            newLesson = { id: res.id, title: res.title, html: res.html };
          } catch (err) {
            console.warn('[Mikey Note] Fallback to local lesson creation');
          }
        }

        book.lessons.push(newLesson);
        saveLocalFallback();
        renderLessons();
        openLesson(newLesson.id);
      }, 'Create Lesson');
  };
}

const btnCreateFirstLesson = $('#btn-create-first-lesson');
if (btnCreateFirstLesson) {
  btnCreateFirstLesson.onclick = () => {
    if (addLessonBtn) addLessonBtn.click();
  };
}

$('#lessons').addEventListener('click', async e => {
  const t = e.target.closest('button');
  if (!t) return;

  if (t.dataset.o) {
    flush();
    armed = null;
    openLesson(t.dataset.o);
  }

  if (t.dataset.r) {
    const l = book.lessons.find(x => x.id === t.dataset.r);
    dlg('Rename Lesson', `<label>Lesson Title</label><input type="text" id="ln" value="${esc(l.title)}">`, async o => {
      const updatedTitle = o.querySelector('#ln').value.trim() || l.title;
      l.title = updatedTitle;
      if (isServerOnline) {
        api(`/api/lessons/${l.id}`, {
          method: 'PUT',
          body: JSON.stringify({ title: updatedTitle })
        }).catch(() => {});
      }
      saveLocalFallback();
      renderLessons();
    }, 'Save Title');
  }

  if (t.dataset.d) {
    const id = t.dataset.d;
    const targetLesson = book.lessons.find(x => x.id === id);
    const title = targetLesson ? targetLesson.title : 'this lesson';
    const confirmed = await showModernConfirm(
      'Delete Lesson',
      `Delete lesson '${title}'? This action cannot be undone.`,
      'danger',
      'Delete Lesson',
      'Cancel'
    );
    if (!confirmed) return;

    if (isServerOnline) {
      api(`/api/lessons/${id}`, { method: 'DELETE' }).catch(() => {});
    }
    book.lessons = book.lessons.filter(l => l.id !== id);
    saveLocalFallback();

    if (book.lessons.length === 0) {
      lesson = null;
      sheet.classList.add('hide');
      $('#empty').classList.remove('hide');
      renderLessons();
      stat();
    } else if (lesson && lesson.id === id) {
      openLesson(book.lessons[0].id);
    } else {
      renderLessons();
    }
  }
});

function openLesson(id) {
  lesson = book.lessons.find(l => l.id === id);
  if (!lesson) return;
  sheet.innerHTML = lesson.html || '<p><br></p>';
  sheet.classList.remove('hide');
  $('#empty').classList.add('hide');
  renderLessons();
  stat();
}

/* ==========================================================================
   7. Real-Time Auto-Save directly to SQLite
   ========================================================================== */
function flush() {
  if (lesson) {
    lesson.html = sheet.innerHTML;
    saveLocalFallback();
    if (isServerOnline) {
      const text = sheet.innerText || '';
      const words = (text.match(/\S+/g) || []).length;
      lesson.wordCount = words;
      lesson.charCount = text.length;

      api(`/api/lessons/${lesson.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: lesson.title,
          html: sheet.innerHTML,
          wordCount: words,
          charCount: text.length
        })
      }).catch(err => console.warn('SQLite auto-save error:', err));
    }
  }
}

let autoSaveTimer;
sheet.addEventListener('input', () => {
  isSaving = true;
  updateSyncIndicator();
  clearTimeout(autoSaveTimer);
  autoSaveTimer = setTimeout(() => {
    flush();
    isSaving = false;
    updateSyncIndicator();
    stat();
  }, 350);
});

/* ==========================================================================
   7b. Smart Lorem Ipsum Shorthand Expander
   ========================================================================== */
const LOREM_DICTIONARY = [
  'lorem', 'ipsum', 'dolor', 'sit', 'amet', 'consectetur', 'adipiscing', 'elit',
  'sed', 'do', 'eiusmod', 'tempor', 'incididunt', 'ut', 'labore', 'et', 'dolore',
  'magna', 'aliqua', 'ut', 'enim', 'ad', 'minim', 'veniam', 'quis', 'nostrud',
  'exercitation', 'ullamco', 'laboris', 'nisi', 'ut', 'aliquip', 'ex', 'ea',
  'commodo', 'consequat', 'duis', 'aute', 'irure', 'dolor', 'in', 'reprehenderit',
  'in', 'voluptate', 'velit', 'esse', 'cillum', 'dolore', 'eu', 'fugiat', 'nulla',
  'pariatur', 'excepteur', 'sint', 'occaecat', 'cupidatat', 'non', 'proident',
  'sunt', 'in', 'culpa', 'qui', 'officia', 'deserunt', 'mollit', 'anim', 'id',
  'est', 'laborum', 'at', 'vero', 'eos', 'et', 'accusamus', 'et', 'iusto',
  'odio', 'dignissimos', 'ducimus', 'qui', 'blanditiis', 'praesentium', 'voluptatum',
  'deleniti', 'atque', 'corrupti', 'quos', 'dolores', 'et', 'quas', 'molestias',
  'excepturi', 'sint', 'occaecati', 'cupiditate', 'non', 'provident', 'similique',
  'sunt', 'in', 'culpa', 'qui', 'officia', 'deserunt', 'mollitia', 'animi', 'id',
  'est', 'laborum', 'et', 'dolorum', 'fuga', 'harum', 'quidem', 'rerum', 'facilis'
];

function generateLoremText(count) {
  if (count <= 0) return '';
  const out = [];
  let wordsInSentence = 0;
  let sentenceLen = 8 + (count % 5);
  let startOfSentence = true;

  for (let i = 0; i < count; i++) {
    let w = LOREM_DICTIONARY[i % LOREM_DICTIONARY.length];
    if (startOfSentence) {
      w = w.charAt(0).toUpperCase() + w.slice(1);
      startOfSentence = false;
    }

    if (i === count - 1) {
      w += '.';
    } else if (wordsInSentence >= sentenceLen) {
      w += '.';
      startOfSentence = true;
      wordsInSentence = 0;
      sentenceLen = 8 + ((i * 7) % 7);
    } else if (wordsInSentence === 4 || wordsInSentence === 9) {
      w += ',';
    }
    out.push(w);
    wordsInSentence++;
  }
  return out.join(' ');
}

function getLoremCommandAtCaret() {
  const sel = window.getSelection();
  if (!sel || !sel.rangeCount) return null;
  const range = sel.getRangeAt(0);

  let node = range.startContainer;
  let offset = range.startOffset;

  if (node.nodeType === Node.ELEMENT_NODE) {
    if (offset > 0 && node.childNodes[offset - 1]) {
      node = node.childNodes[offset - 1];
      if (node.nodeType === Node.TEXT_NODE) {
        offset = node.textContent.length;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  if (node.nodeType === Node.TEXT_NODE) {
    const textBefore = node.textContent.substring(0, offset);
    const m = textBefore.match(/(?:^|[\s\u00A0])(=?lorem\s*\(?\s*(\d+)?\s*\)?\s*)$/i);
    if (m) {
      const fullToken = m[1];
      const numStr = m[2];
      let count = (numStr && numStr.trim()) ? parseInt(numStr, 10) : 0;
      if (count === 0 && fullToken.trim().toLowerCase() === 'lorem') {
        count = 50;
      }
      if (count > 0 && count <= 5000) {
        return {
          node,
          offset,
          fullToken,
          count
        };
      }
    }
  }
  return null;
}

sheet.addEventListener('keydown', async e => {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
    const cmd = getLoremCommandAtCaret();
    if (cmd) {
      e.preventDefault();
      const confirmed = await showModernConfirm(
        'Generate Lorem Ipsum',
        `Create ${cmd.count} sample words using Lorem Ipsum?`,
        'info',
        'Generate Words',
        'Cancel'
      );

      if (confirmed) {
        const sel = window.getSelection();
        const r = document.createRange();
        const tokenStart = cmd.offset - cmd.fullToken.length;
        r.setStart(cmd.node, tokenStart);
        r.setEnd(cmd.node, cmd.offset);
        r.deleteContents();

        const lorem = generateLoremText(cmd.count);
        const textNode = document.createTextNode(lorem);
        r.insertNode(textNode);

        const newRange = document.createRange();
        newRange.setStartAfter(textNode);
        newRange.collapse(true);
        sel.removeAllRanges();
        sel.addRange(newRange);

        sync();
      } else {
        document.execCommand('insertParagraph');
      }
    }
  }
});

function updateSyncIndicator() {
  const ind = $('#sync-indicator');
  if (!ind) return;
  if (!isServerOnline) {
    ind.className = 'sync-tag offline';
    ind.innerHTML = 'Browser Storage (Offline)';
  } else if (isSaving) {
    ind.className = 'sync-tag saving';
    ind.innerHTML = '💾 Syncing to SQLite...';
  } else {
    ind.className = 'sync-tag saved';
    ind.innerHTML = '🟢 Saved to SQLite';
  }
}

function stat() {
  const t = sheet.innerText || '';
  const w = (t.match(/\S+/g) || []).length;
  const readTimeMin = Math.max(1, Math.round(w / 200));

  updateSyncIndicator();

  $('#stat').innerHTML = lesson ? `
    <span><b>${w.toLocaleString()}</b> words</span>
    <span><b>${t.length.toLocaleString()}</b> characters</span>
    <span>⏱ ~${readTimeMin} min read</span>
    <span style="flex:1"></span>
    <label style="display:flex;align-items:center;gap:6px;">
      Zoom:
      <select id="zm" style="padding:2px 8px;font-size:12px;">
        <option value="0.75">75%</option>
        <option value="0.9">90%</option>
        <option value="1" ${sheet.style.zoom === '' || sheet.style.zoom === '1' ? 'selected' : ''}>100%</option>
        <option value="1.15">115%</option>
        <option value="1.25">125%</option>
        <option value="1.5">150%</option>
      </select>
    </label>
    <span class="sync-tag saved" style="font-size:11px;">🗄️ SQLite WAL</span>` : '';
}

document.addEventListener('change', e => {
  if (e.target.id === 'zm') {
    sheet.style.zoom = e.target.value;
  }
});

function saveLocalFallback() {
  try {
    localStorage.setItem(KEY, JSON.stringify(S));
  } catch (e) {}
}

/* ==========================================================================
   8. MS Word-Style Ribbon Toolbar & Formatting
   ========================================================================== */
document.addEventListener('selectionchange', () => {
  const s = getSelection();
  if (s.rangeCount && sheet.contains(s.anchorNode)) {
    rng = s.getRangeAt(0).cloneRange();
  }
});

function restore() {
  sheet.focus();
  if (rng) {
    const s = getSelection();
    s.removeAllRanges();
    s.addRange(rng);
  }
}

function exec(c, v) {
  restore();
  document.execCommand(c, false, v);
  sync();
}

function sync() {
  flush();
  stat();
}

$('.ribbon').addEventListener('mousedown', e => {
  if (!e.target.closest('select, input')) e.preventDefault();
});

$('.ribbon').addEventListener('click', e => {
  const b = e.target.closest('[data-c]');
  if (b) exec(b.dataset.c);
  const t = e.target.closest('.tab');
  if (t) {
    $$('.tab').forEach(x => x.classList.toggle('on', x === t));
    $$('.panel').forEach(p => p.classList.toggle('hide', p.id !== 'p-' + t.dataset.t));
  }
});

$('#ff').onchange = e => exec('fontName', e.target.value);
$('#st').onchange = e => exec('formatBlock', '<' + e.target.value + '>');
$('#fc').oninput = e => exec('foreColor', e.target.value);
$('#hc').oninput = e => exec('hiliteColor', e.target.value);

$('#fz').onchange = e => {
  restore();
  document.execCommand('fontSize', false, '7');
  sheet.querySelectorAll('font[size="7"]').forEach(f => {
    const s = document.createElement('span');
    s.style.fontSize = e.target.value + 'px';
    s.innerHTML = f.innerHTML;
    f.replaceWith(s);
  });
  sync();
};

$('#ls').onchange = e => {
  restore();
  const s = getSelection();
  let n = s.anchorNode;
  while (n && n !== sheet && !/^(P|DIV|H[1-6]|LI|BLOCKQUOTE)$/.test(n.nodeName)) n = n.parentNode;
  if (n && n !== sheet) n.style.lineHeight = e.target.value;
  sync();
};

$('#sym').onchange = e => {
  if (e.target.value) exec('insertText', e.target.value);
  e.target.selectedIndex = 0;
};

$('#i-hr').onclick = () => exec('insertHorizontalRule');
$('#i-pb').onclick = () => exec('insertHTML', '<div class="pbk" contenteditable="false">— A4 Page Break —</div><p><br></p>');
$('#i-date').onclick = () => exec('insertText', new Date().toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }));
$('#i-chk').onclick = () => exec('insertHTML', '<span contenteditable="false" style="user-select:none;margin-right:4px;">☐</span>&nbsp;');
$('#i-q').onclick = () => exec('formatBlock', '<blockquote>');
$('#i-code').onclick = () => exec('formatBlock', '<pre>');
$('#i-img').onclick = () => $('#file').click();

$('#file').onchange = e => {
  const f = e.target.files[0];
  if (!f) return;
  const r = new FileReader();
  r.onload = () => {
    exec('insertImage', r.result);
    setTimeout(() => {
      const imgs = sheet.querySelectorAll('img');
      if (imgs.length) selectImage(imgs[imgs.length - 1]);
    }, 60);
  };
  r.readAsDataURL(f);
  e.target.value = '';
};

$('#i-link').onclick = () => dlg('Insert Link', '<label>Web Address URL</label><input type="text" id="u" placeholder="https://example.com">', o => {
  const u = o.querySelector('#u').value.trim();
  if (!u) return false;
  exec('createLink', /^https?:/.test(u) ? u : 'https://' + u);
});

/* ==========================================================================
   9. Rich Table Studio
   ========================================================================== */
$('#i-table').onclick = () => dlg('Insert Table',
  `<div style="display:flex;gap:16px;">
     <div style="flex:1;">
       <label>Rows</label><input type="number" id="tr" value="4" min="1" max="30">
     </div>
     <div style="flex:1;">
       <label>Columns</label><input type="number" id="tc" value="3" min="1" max="12">
     </div>
   </div>
   <label style="margin-top:12px;display:flex;align-items:center;gap:6px;">
     <input type="checkbox" id="th" checked style="width:auto;"> Include Header Row
   </label>`, o => {
    const r = Math.min(30, +o.querySelector('#tr').value || 2);
    const c = Math.min(12, +o.querySelector('#tc').value || 2);
    const h = o.querySelector('#th').checked;
    let t = '<table><tbody>';
    for (let i = 0; i < r; i++) {
      t += '<tr>';
      for (let j = 0; j < c; j++) {
        t += (i === 0 && h) ? '<th style="background:#f1f5f9;"><br></th>' : '<td><br></td>';
      }
      t += '</tr>';
    }
    exec('insertHTML', t + '</tbody></table><p><br></p>');
  }, 'Insert Table');

const cell = () => {
  let n = rng && rng.startContainer;
  while (n && n !== sheet) {
    if (/^T[DH]$/.test(n.nodeName)) return n;
    n = n.parentNode;
  }
  return null;
};

function mk(tag) {
  const c = document.createElement(tag || 'td');
  c.innerHTML = '<br>';
  return c;
}

$('#p-table').addEventListener('click', e => {
  const b = e.target.closest('[data-tb]');
  if (!b) return;
  const c = cell();
  if (!c) return showModernAlert('Table Selection Required', 'Please click inside a table cell first to use Table Studio tools.', 'info');

  const tr = c.parentNode;
  const tb = tr.closest('table');
  const i = [...tr.children].indexOf(c);
  const a = b.dataset.tb;

  if (a === 'rowA' || a === 'rowB') {
    const n = document.createElement('tr');
    [...tr.children].forEach(() => n.appendChild(mk('td')));
    a === 'rowA' ? tr.after(n) : tr.before(n);
  }
  if (a === 'colA' || a === 'colB') {
    [...tb.rows].forEach(r => {
      const x = mk(r.cells[i] ? r.cells[i].nodeName : 'td');
      r.cells[i][a === 'colA' ? 'after' : 'before'](x);
    });
  }
  if (a === 'rowD') tr.remove();
  if (a === 'colD') [...tb.rows].forEach(r => r.cells[i] && r.cells[i].remove());
  if (a === 'tblD') tb.remove();
  if (a === 'head') {
    [...tb.rows[0].cells].forEach(x => {
      x.style.background = '#f1f5f9';
      x.style.fontWeight = '700';
    });
  }
  sync();
});

$('#cc').oninput = e => {
  const c = cell();
  if (c) {
    c.style.background = e.target.value;
    sync();
  }
};

/* ==========================================================================
   9b. Picture Format, Image Effects & Context Menu
   ========================================================================== */
let selectedImage = null;

function selectImage(img) {
  if (!img) return;
  if (selectedImage && selectedImage !== img) {
    selectedImage.classList.remove('selected-img');
  }
  selectedImage = img;
  img.classList.add('selected-img');
  const picTab = $('#tab-picture');
  if (picTab) {
    picTab.classList.remove('hide');
    picTab.click();
  }
}

function deselectImage() {
  if (selectedImage) {
    selectedImage.classList.remove('selected-img');
    selectedImage = null;
  }
  const picTab = $('#tab-picture');
  if (picTab) {
    picTab.classList.add('hide');
    const picPanel = $('#p-picture');
    if (picPanel && !picPanel.classList.contains('hide')) {
      const homeTab = $('.tab[data-t="home"]');
      if (homeTab) homeTab.click();
    }
  }
}

function applyImageVisualFilter(img, filter) {
  if (!img) return;
  img.style.filter = '';
  img.style.borderRadius = '';
  img.style.boxShadow = '';
  img.style.border = '';
  img.style.aspectRatio = '';
  img.style.objectFit = '';

  switch (filter) {
    case 'original':
      break;
    case 'grayscale':
      img.style.filter = 'grayscale(100%)';
      break;
    case 'sepia':
      img.style.filter = 'sepia(85%) saturate(120%)';
      break;
    case 'vivid':
      img.style.filter = 'saturate(180%) contrast(115%)';
      break;
    case 'contrast':
      img.style.filter = 'contrast(160%) brightness(95%)';
      break;
    case 'invert':
      img.style.filter = 'invert(100%)';
      break;
    case 'rounded':
      img.style.borderRadius = '24px';
      break;
    case 'oval':
      img.style.borderRadius = '50%';
      img.style.aspectRatio = '1 / 1';
      img.style.objectFit = 'cover';
      break;
    case 'shadow':
      img.style.boxShadow = '0 16px 36px -8px rgba(0, 0, 0, 0.45)';
      break;
    case 'border':
      img.style.border = '4px solid var(--accent-primary, #2563eb)';
      img.style.borderRadius = '8px';
      break;
    case 'vignette':
      img.style.filter = 'contrast(125%) brightness(90%) drop-shadow(0 0 16px rgba(0, 0, 0, 0.6))';
      break;
    case 'blur':
      img.style.filter = 'blur(2.5px)';
      break;
  }
  sync();
}

// Picture Format Ribbon Panel Interaction
const picturePanel = $('#p-picture');
if (picturePanel) {
  picturePanel.addEventListener('click', async e => {
    if (!selectedImage) {
      showModernAlert('Image Selection Required', 'Please click on an image in the document canvas first.', 'info');
      return;
    }

    const filterBtn = e.target.closest('[data-img-filter]');
    if (filterBtn) {
      applyImageVisualFilter(selectedImage, filterBtn.dataset.imgFilter);
      return;
    }

    const sizeBtn = e.target.closest('[data-img-size]');
    if (sizeBtn) {
      const sz = sizeBtn.dataset.imgSize;
      selectedImage.style.width = sz + 'px';
      selectedImage.style.maxWidth = '100%';
      selectedImage.style.height = 'auto';
      sync();
      return;
    }

    const alignBtn = e.target.closest('[data-img-align]');
    if (alignBtn) {
      const al = alignBtn.dataset.imgAlign;
      selectedImage.style.display = 'block';
      selectedImage.style.float = 'none';
      if (al === 'left') {
        selectedImage.style.marginLeft = '0';
        selectedImage.style.marginRight = 'auto';
      } else if (al === 'center') {
        selectedImage.style.marginLeft = 'auto';
        selectedImage.style.marginRight = 'auto';
      } else if (al === 'right') {
        selectedImage.style.marginLeft = 'auto';
        selectedImage.style.marginRight = '0';
      }
      sync();
      return;
    }

    const wrapBtn = e.target.closest('[data-img-wrap]');
    if (wrapBtn) {
      const wr = wrapBtn.dataset.imgWrap;
      if (wr === 'inline') {
        selectedImage.style.float = 'none';
        selectedImage.style.display = 'inline-block';
        selectedImage.style.margin = '8px';
      } else if (wr === 'float-left') {
        selectedImage.style.float = 'left';
        selectedImage.style.display = 'block';
        selectedImage.style.margin = '8px 16px 8px 0';
      } else if (wr === 'float-right') {
        selectedImage.style.float = 'right';
        selectedImage.style.display = 'block';
        selectedImage.style.margin = '8px 0 8px 16px';
      }
      sync();
      return;
    }

    if (e.target.closest('#btn-img-replace')) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = ev => {
        const file = ev.target.files[0];
        if (!file || !selectedImage) return;
        const reader = new FileReader();
        reader.onload = re => {
          selectedImage.src = re.target.result;
          sync();
        };
        reader.readAsDataURL(file);
      };
      input.click();
      return;
    }

    if (e.target.closest('#btn-img-delete')) {
      const confirmed = await showModernConfirm(
        'Delete Image',
        'Are you sure you want to delete the selected image?',
        'danger',
        'Delete Image',
        'Cancel'
      );
      if (confirmed && selectedImage) {
        selectedImage.remove();
        deselectImage();
        sync();
      }
      return;
    }
  });
}

// Canvas & Window Image Interaction
sheet.addEventListener('click', e => {
  const img = e.target.closest('img');
  if (img) {
    selectImage(img);
  } else if (!e.target.closest('#p-picture') && !e.target.closest('#tab-picture') && !e.target.closest('#web-context-menu')) {
    deselectImage();
  }
});

document.addEventListener('click', e => {
  if (!e.target.closest('#sheet') && !e.target.closest('#p-picture') && !e.target.closest('#tab-picture') && !e.target.closest('#web-context-menu')) {
    deselectImage();
  }
  const ctx = $('#web-context-menu');
  if (ctx && !e.target.closest('#web-context-menu')) {
    ctx.classList.add('hide');
  }
});

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const ctx = $('#web-context-menu');
    if (ctx && !ctx.classList.contains('hide')) {
      ctx.classList.add('hide');
      return;
    }
    if (selectedImage) {
      deselectImage();
    }
  }
});

// Canvas Right-Click Context Menu
const webContextMenu = $('#web-context-menu');
sheet.addEventListener('contextmenu', e => {
  e.preventDefault();

  const img = e.target.closest('img');
  const td = e.target.closest('td, th');
  const table = td ? td.closest('table') : null;
  const sel = window.getSelection();
  const hasSelection = sel && !sel.isCollapsed && sel.toString().trim().length > 0;

  if (img) {
    selectImage(img);
  }

  let html = '';

  if (img) {
    html = `
      <div class="ctx-header">Picture</div>
      <div class="ctx-item ctx-sub-trigger">
        <span>Filters</span>
        <div class="ctx-sub-menu">
          <div class="ctx-item" data-action="img-f-grayscale"><span>Grayscale</span></div>
          <div class="ctx-item" data-action="img-f-sepia"><span>Warm Sepia</span></div>
          <div class="ctx-item" data-action="img-f-vivid"><span>Vivid Saturation</span></div>
          <div class="ctx-item" data-action="img-f-contrast"><span>High Contrast</span></div>
          <div class="ctx-item" data-action="img-f-rounded"><span>Rounded Corners</span></div>
          <div class="ctx-item" data-action="img-f-shadow"><span>Drop Shadow</span></div>
          <div class="ctx-sep"></div>
          <div class="ctx-item" data-action="img-f-original"><span>Reset Filters</span></div>
        </div>
      </div>
      <div class="ctx-item ctx-sub-trigger">
        <span>Resize</span>
        <div class="ctx-sub-menu">
          <div class="ctx-item" data-action="img-sz-420"><span>Medium (420px)</span></div>
          <div class="ctx-item" data-action="img-sz-720"><span>Full (720px)</span></div>
        </div>
      </div>
      <div class="ctx-item" data-action="img-al-center"><span>Align Center</span></div>
      <div class="ctx-item" data-action="img-wr-float"><span>Float Wrap</span></div>
      <div class="ctx-sep"></div>
      <div class="ctx-item" data-action="img-replace"><span>Replace...</span></div>
      <div class="ctx-item danger" data-action="img-delete"><span>Delete Image</span></div>
    `;
  } else if (td && table) {
    html = `
      <div class="ctx-header">Table</div>
      <div class="ctx-item" data-action="tbl-row-above"><span>＋ Row Above</span></div>
      <div class="ctx-item" data-action="tbl-row-below"><span>＋ Row Below</span></div>
      <div class="ctx-item" data-action="tbl-col-left"><span>＋ Col Left</span></div>
      <div class="ctx-item" data-action="tbl-col-right"><span>＋ Col Right</span></div>
      <div class="ctx-sep"></div>
      <div class="ctx-item" data-action="tbl-del-row"><span>− Delete Row</span></div>
      <div class="ctx-item" data-action="tbl-del-col"><span>− Delete Column</span></div>
      <div class="ctx-item danger" data-action="tbl-del-table"><span>Delete Table</span></div>
    `;
  } else if (hasSelection) {
    html = `
      <div class="ctx-header">Text</div>
      <div class="ctx-item" data-action="fmt-bold"><span><b>Bold</b></span><span class="ctx-shortcut">Ctrl+B</span></div>
      <div class="ctx-item" data-action="fmt-italic"><span><i>Italic</i></span><span class="ctx-shortcut">Ctrl+I</span></div>
      <div class="ctx-item" data-action="fmt-underline"><span><u>Underline</u></span><span class="ctx-shortcut">Ctrl+U</span></div>
      <div class="ctx-item" data-action="fmt-strike"><span><s>Strikethrough</s></span></div>
      <div class="ctx-sep"></div>
      <div class="ctx-item ctx-sub-trigger">
        <span>Headings</span>
        <div class="ctx-sub-menu">
          <div class="ctx-item" data-action="fmt-h1"><span>Heading 1</span></div>
          <div class="ctx-item" data-action="fmt-h2"><span>Heading 2</span></div>
          <div class="ctx-item" data-action="fmt-p"><span>Normal</span></div>
        </div>
      </div>
      <div class="ctx-item" data-action="fmt-quote"><span>❝ Quote</span></div>
      <div class="ctx-item" data-action="fmt-code"><span>&lt;/&gt; Code</span></div>
      <div class="ctx-sep"></div>
      <div class="ctx-item" data-action="fmt-clear"><span>Clear Format</span></div>
    `;
  } else {
    html = `
      <div class="ctx-header">Canvas</div>
      <div class="ctx-item" data-action="canv-undo"><span>↶ Undo</span><span class="ctx-shortcut">Ctrl+Z</span></div>
      <div class="ctx-item" data-action="canv-redo"><span>↷ Redo</span><span class="ctx-shortcut">Ctrl+Y</span></div>
      <div class="ctx-sep"></div>
      <div class="ctx-item ctx-sub-trigger">
        <span>Lists</span>
        <div class="ctx-sub-menu">
          <div class="ctx-item" data-action="canv-bullet"><span>• Bullet</span></div>
          <div class="ctx-item" data-action="canv-num"><span>1. Numbered</span></div>
        </div>
      </div>
      <div class="ctx-item ctx-sub-trigger">
        <span>Insert</span>
        <div class="ctx-sub-menu">
          <div class="ctx-item" data-action="canv-table"><span>▦ Table</span></div>
          <div class="ctx-item" data-action="canv-image"><span>🖼 Image...</span></div>
          <div class="ctx-item" data-action="canv-pagebreak"><span>⤓ Page Break</span></div>
          <div class="ctx-item" data-action="canv-date"><span>📅 Date</span></div>
        </div>
      </div>
    `;
  }

  webContextMenu.innerHTML = html;
  webContextMenu.classList.remove('hide');

  const menuWidth = 175;
  const menuHeight = webContextMenu.offsetHeight || 260;
  let posX = e.clientX;
  let posY = e.clientY;

  if (posX + menuWidth > window.innerWidth - 10) {
    posX = window.innerWidth - menuWidth - 10;
  }
  if (posY + menuHeight > window.innerHeight - 10) {
    posY = Math.max(10, window.innerHeight - menuHeight - 10);
  }

  webContextMenu.style.left = `${Math.max(10, posX)}px`;
  webContextMenu.style.top = `${Math.max(10, posY)}px`;

  webContextMenu._targetCell = td;
  webContextMenu._targetImg = img;
});

webContextMenu.addEventListener('click', async e => {
  const item = e.target.closest('[data-action]');
  if (!item) return;

  const act = item.dataset.action;
  const targetImg = webContextMenu._targetImg || selectedImage;
  const targetCell = webContextMenu._targetCell;

  webContextMenu.classList.add('hide');

  // Image actions
  if (act.startsWith('img-f-')) {
    const filter = act.replace('img-f-', '');
    applyImageVisualFilter(targetImg, filter);
  } else if (act === 'img-sz-420' && targetImg) {
    targetImg.style.width = '420px';
    targetImg.style.maxWidth = '100%';
    targetImg.style.height = 'auto';
    sync();
  } else if (act === 'img-sz-720' && targetImg) {
    targetImg.style.width = '720px';
    targetImg.style.maxWidth = '100%';
    targetImg.style.height = 'auto';
    sync();
  } else if (act === 'img-al-center' && targetImg) {
    targetImg.style.display = 'block';
    targetImg.style.float = 'none';
    targetImg.style.marginLeft = 'auto';
    targetImg.style.marginRight = 'auto';
    sync();
  } else if (act === 'img-wr-float' && targetImg) {
    targetImg.style.float = 'left';
    targetImg.style.display = 'block';
    targetImg.style.margin = '8px 16px 8px 0';
    sync();
  } else if (act === 'img-replace' && targetImg) {
    $('#btn-img-replace')?.click();
  } else if (act === 'img-delete' && targetImg) {
    const confirmed = await showModernConfirm('Delete Image', 'Remove selected image from document?', 'danger', 'Delete');
    if (confirmed) {
      targetImg.remove();
      deselectImage();
      sync();
    }
  }

  // Table actions
  else if (act.startsWith('tbl-') && targetCell) {
    const tr = targetCell.parentNode;
    const tb = tr.closest('table');
    const i = [...tr.children].indexOf(targetCell);
    if (act === 'tbl-row-above') {
      const n = document.createElement('tr');
      [...tr.children].forEach(() => n.appendChild(mk('td')));
      tr.before(n);
    } else if (act === 'tbl-row-below') {
      const n = document.createElement('tr');
      [...tr.children].forEach(() => n.appendChild(mk('td')));
      tr.after(n);
    } else if (act === 'tbl-col-left') {
      [...tb.rows].forEach(r => {
        const x = mk(r.cells[i] ? r.cells[i].nodeName : 'td');
        r.cells[i].before(x);
      });
    } else if (act === 'tbl-col-right') {
      [...tb.rows].forEach(r => {
        const x = mk(r.cells[i] ? r.cells[i].nodeName : 'td');
        r.cells[i].after(x);
      });
    } else if (act === 'tbl-del-row') {
      tr.remove();
    } else if (act === 'tbl-del-col') {
      [...tb.rows].forEach(r => r.cells[i] && r.cells[i].remove());
    } else if (act === 'tbl-del-table') {
      tb.remove();
    }
    sync();
  }

  // Formatting actions
  else if (act === 'fmt-bold') exec('bold');
  else if (act === 'fmt-italic') exec('italic');
  else if (act === 'fmt-underline') exec('underline');
  else if (act === 'fmt-strike') exec('strikeThrough');
  else if (act === 'fmt-h1') exec('formatBlock', '<h1>');
  else if (act === 'fmt-h2') exec('formatBlock', '<h2>');
  else if (act === 'fmt-p') exec('formatBlock', '<p>');
  else if (act === 'fmt-quote') exec('formatBlock', '<blockquote>');
  else if (act === 'fmt-code') exec('formatBlock', '<pre>');
  else if (act === 'fmt-clear') exec('removeFormat');

  // Canvas actions
  else if (act === 'canv-undo') exec('undo');
  else if (act === 'canv-redo') exec('redo');
  else if (act === 'canv-bullet') exec('insertUnorderedList');
  else if (act === 'canv-num') exec('insertOrderedList');
  else if (act === 'canv-table') $('#i-table')?.click();
  else if (act === 'canv-image') $('#i-img')?.click();
  else if (act === 'canv-pagebreak') $('#i-pb')?.click();
  else if (act === 'canv-date') $('#i-date')?.click();
});

/* ==========================================================================
   10. Interactive Vector Chart Designer (Connected to Java SVG Engine)
   ========================================================================== */
const CHART_PRESETS = {
  bar: 'Quarter 1: 45\nQuarter 2: 78\nQuarter 3: 62\nQuarter 4: 95',
  pie: 'Research: 35\nDesign: 25\nDevelopment: 30\nTesting: 10',
  line: 'Jan: 12\nFeb: 28\nMar: 42\nApr: 68\nMay: 90\nJun: 110',
  tree: 'System\n  Frontend\n    UI: 8\n    State: 6\n  Backend\n    REST: 10\n    SQLite: 12',
  diagram: 'Architecture\n  Presentation Layer\n    Web Client\n    Desktop Helper\n  Core Domain\n  SQLite Engine'
};

$('#i-chart').onclick = () => {
  const chartModalHtml = `
    <div style="display:flex;flex-direction:column;gap:12px;">
      <div style="display:flex;gap:12px;">
        <div style="flex:1;">
          <label>Chart Type</label>
          <select id="ct" style="width:100%;">
            <option value="bar">Bar Chart</option>
            <option value="pie">Pie Chart</option>
            <option value="line">Line Graph</option>
            <option value="tree">Treemap</option>
            <option value="diagram">Hierarchy Diagram</option>
          </select>
        </div>
        <div style="flex:1;">
          <label>Data Presets</label>
          <select id="cp" style="width:100%;">
            <option value="bar">Quarterly Revenue</option>
            <option value="pie">Team Allocation</option>
            <option value="line">Growth Trend</option>
            <option value="tree">Project Modules</option>
            <option value="diagram">Architecture Stack</option>
          </select>
        </div>
      </div>

      <label>Data Points (Label: Value or Indented Hierarchy)</label>
      <textarea id="cd" rows="5" style="width:100%;font-family:var(--font-mono);">${CHART_PRESETS.bar}</textarea>

      <label>Live Vector SVG Preview</label>
      <div id="cprev" class="prev" style="min-height:180px;background:var(--bg-surface-elevated);border-radius:var(--radius-md);padding:14px;display:flex;align-items:center;justify-content:center;border:1px solid var(--border-subtle);overflow:auto;">
        <span style="color:var(--text-muted);font-size:12px;">Rendering chart preview…</span>
      </div>
    </div>`;

  const modal = dlg('📊 Vector Chart Studio', chartModalHtml, o => {
    const prev = o.querySelector('#cprev');
    const svg = prev.querySelector('svg');
    if (!svg) return false;
    exec('insertHTML', `<div class="chart" contenteditable="false">${svg.outerHTML}</div><p><br></p>`);
  }, 'Insert Chart');

  const ct = modal.querySelector('#ct');
  const cp = modal.querySelector('#cp');
  const cd = modal.querySelector('#cd');
  const prev = modal.querySelector('#cprev');

  async function updateChartPreview() {
    const type = ct.value;
    const data = cd.value.trim();

    if (isServerOnline) {
      try {
        const res = await api('/api/charts/render', {
          method: 'POST',
          body: JSON.stringify({ type, data })
        });
        if (res && res.svg) {
          prev.innerHTML = res.svg;
          return;
        }
      } catch (e) {
        console.warn('Server SVG preview error; falling back to client engine');
      }
    }

    // Client-side fallback rendering
    prev.innerHTML = renderClientChartSvg(type, data);
  }

  ct.onchange = () => {
    if (CHART_PRESETS[ct.value]) {
      cd.value = CHART_PRESETS[ct.value];
      cp.value = ct.value;
    }
    updateChartPreview();
  };

  cp.onchange = () => {
    ct.value = cp.value;
    cd.value = CHART_PRESETS[cp.value];
    updateChartPreview();
  };

  cd.oninput = () => updateChartPreview();
  updateChartPreview();
};

function renderClientChartSvg(type, txt) {
  const rows = txt.split('\n').filter(l => l.trim()).map(l => {
    const d = l.match(/^[ \t]*/)[0].replace(/\t/g, '  ').length >> 1;
    let s = l.trim(), v = null, m;
    if (m = s.match(/^(.*?)[:\s]\s*(-?\d+(?:\.\d+)?)\s*%?$/)) {
      s = m[1].replace(/[:\-\s]+$/, '') || s;
      v = +m[2];
    }
    return { n: s, v: v == null ? 1 : v, d };
  });

  if (!rows.length) return '<p style="color:#94a3b8;font-size:12px;">Enter data above to preview chart.</p>';

  const W = 460, H = 220, m = 36;
  const mx = Math.max(...rows.map(x => x.v), 1);
  let bars = '';
  const bw = (W - m - 20) / rows.length;

  rows.forEach((x, i) => {
    const h = (H - m - 30) * x.v / mx;
    const bx = m + i * bw + bw * 0.15;
    const fill = PAL[i % PAL.length];
    bars += `
      <rect x="${bx}" y="${H - m - h}" width="${bw * 0.7}" height="${h}" rx="4" fill="${fill}" />
      <text x="${bx + bw * 0.35}" y="${H - m - h - 5}" font-size="11" text-anchor="middle" fill="currentColor">${x.v}</text>
      <text x="${bx + bw * 0.35}" y="${H - m + 16}" font-size="11" text-anchor="middle" fill="currentColor">${esc(x.n.slice(0, 8))}</text>`;
  });

  return `<svg viewBox="0 0 ${W} ${H}" width="100%" style="color:var(--text-primary);">${bars}</svg>`;
}

/* ==========================================================================
   11. Export, Print & Backup Controls
   ========================================================================== */
$('#dl-md').onclick = () => {
  if (!lesson) return;
  flush();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([`# ${lesson.title}\n\n${lesson.html}`], { type: 'text/markdown' }));
  a.download = lesson.title.replace(/[^\w\- ]+/g, '') + '.md';
  a.click();
};

$('#dl').onclick = () => {
  if (!lesson) return;
  flush();
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${esc(lesson.title)}</title><style>body{font-family:system-ui,sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.6;}table{border-collapse:collapse;width:100%;margin:16px 0;}td,th{border:1px solid #ccc;padding:8px;}blockquote{border-left:4px solid #2563eb;margin:0;padding-left:14px;color:#555;}pre{background:#f1f5f9;padding:12px;border-radius:6px;}</style></head><body><h1>${esc(lesson.title)}</h1>${lesson.html}</body></html>`;
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([html], { type: 'text/html' }));
  a.download = lesson.title.replace(/[^\w\- ]+/g, '') + '.html';
  a.click();
};

$('#pr').onclick = () => {
  flush();
  window.print();
};

document.addEventListener('keydown', e => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault();
    flush();
  }
});

/* ==========================================================================
   12. SQLite Telemetry & Import/Export Backups
   ========================================================================== */
$('#btn-db-info').onclick = async () => {
  if (!isServerOnline) {
    return showModernAlert('SQLite Server Offline', 'Running in browser storage mode. Start the Mikey Note Java server to access live SQLite telemetry.', 'info');
  }
  try {
    const stats = await api('/api/db/stats');
    const kb = (stats.databaseSizeBytes / 1024).toFixed(1);
    const body = `
      <table class="stat-table">
        <tr><td>Database File</td><td><code>${esc(stats.databasePath)}</code></td></tr>
        <tr><td>Storage Size</td><td><b>${kb} KB</b> (${stats.databaseSizeBytes} bytes)</td></tr>
        <tr><td>Total Notebooks</td><td><b>${stats.totalBooks}</b></td></tr>
        <tr><td>Total Lessons</td><td><b>${stats.totalLessons}</b></td></tr>
        <tr><td>Total Words</td><td><b>${stats.totalWords.toLocaleString()}</b></td></tr>
        <tr><td>Java Runtime</td><td>Java ${esc(stats.javaVersion)} (Virtual Threads)</td></tr>
        <tr><td>Server Uptime</td><td>${Math.round(stats.uptimeMillis / 1000)} seconds</td></tr>
      </table>
      <p style="color:var(--text-muted);font-size:12px;margin-top:10px;">
        All notebook transactions are protected with SQLite WAL mode and foreign key cascading.
      </p>`;
    dlg('🗄️ SQLite Database Telemetry', body, () => {}, 'Close');
  } catch (err) {
    showModernAlert('Telemetry Error', 'Failed to retrieve database stats: ' + err.message, 'danger');
  }
};

$('#btn-export-all').onclick = () => {
  if (isServerOnline) {
    window.location.href = '/api/db/export';
  } else {
    const json = JSON.stringify(S, null, 2);
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
    a.download = 'notedown_backup.json';
    a.click();
  }
};

$('#btn-import-all').onclick = () => {
  $('#import-file-input').click();
};

$('#import-file-input').onchange = e => {
  const f = e.target.files[0];
  if (!f) return;
  const r = new FileReader();
  r.onload = async () => {
    try {
      const data = JSON.parse(r.result);
      const booksList = data.books || (Array.isArray(data) ? data : null);
      if (!booksList) throw new Error('Unrecognized format: expected JSON with "books" array.');

      if (isServerOnline) {
        await api('/api/db/import', {
          method: 'POST',
          body: JSON.stringify({ books: booksList })
        });
        const refreshed = await api('/api/books');
        S.books = refreshed.map(b => ({ id: b.id, title: b.title, color: b.color || '#2563eb', lessons: [], lessonCount: b.lessonCount || 0 }));
      } else {
        S.books = booksList;
        saveLocalFallback();
      }
      showHome();
      showModernAlert('Import Complete', 'Notes successfully imported into SQLite!', 'success');
    } catch (err) {
      showModernAlert('Import Failed', 'Import failed: ' + err.message, 'danger');
    }
  };
  r.readAsText(f);
  e.target.value = '';
};
