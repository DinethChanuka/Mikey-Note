/**
 * Dialog Module — Displays accessible modal dialogs and inputs.
 */
export const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

export function showDialog(title, body, onOk, okText = 'OK') {
  const o = document.createElement('div');
  o.className = 'ov';
  o.innerHTML = `
    <div class="dg" role="dialog" aria-label="${esc(title)}">
      <h3>${esc(title)}</h3>
      ${body}
      <div class="ft">
        <button class="c">Cancel</button>
        <button class="ok">${okText}</button>
      </div>
    </div>`;
  document.body.appendChild(o);

  const close = () => o.remove();
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
