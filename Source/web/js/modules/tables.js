/**
 * Tables Module — Interactive table insertion, cell background, rows/columns operations.
 */
import { showDialog } from './dialog.js';

export function mkCell(tag) {
  const c = document.createElement(tag || 'td');
  c.innerHTML = '<br>';
  return c;
}

export function getActiveCell(sheet, rng) {
  let n = rng && rng.startContainer;
  while (n && n !== sheet) {
    if (/^T[DH]$/.test(n.nodeName)) return n;
    n = n.parentNode;
  }
  return null;
}

export function promptInsertTable(execFn) {
  showDialog('Insert table',
    `<label>Rows</label><input type="number" id="tr" value="4" min="1" max="30">
     <label>Columns</label><input type="number" id="tc" value="3" min="1" max="12">
     <label><input type="checkbox" id="th" checked style="width:auto"> Header row</label>`, o => {
      const r = Math.min(30, +o.querySelector('#tr').value || 2);
      const c = Math.min(12, +o.querySelector('#tc').value || 2);
      const h = o.querySelector('#th').checked;
      let t = '<table><tbody>';
      for (let i = 0; i < r; i++) {
        t += '<tr>';
        for (let j = 0; j < c; j++) {
          t += (i === 0 && h) ? '<th style="background:#dbeafe"><br></th>' : '<td><br></td>';
        }
        t += '</tr>';
      }
      execFn('insertHTML', t + '</tbody></table><p><br></p>');
    }, 'Insert');
}
