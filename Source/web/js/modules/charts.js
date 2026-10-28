/**
 * Charts Module — SVG generation for Bar, Line, Pie, Treemap, and Diagram charts.
 */
import { esc } from './dialog.js';

export const PAL = ['#2563eb', '#e11d48', '#16a34a', '#f59e0b', '#7c3aed', '#0891b2', '#db2777', '#65a30d', '#ea580c', '#475569'];

export const SAMPLE = {
  bar: 'Maths: 80\nScience: 65\nHistory: 50\nArt: 90',
  pie: 'Maths: 40\nScience: 30\nHistory: 20\nArt: 10',
  line: 'Jan: 10\nFeb: 25\nMar: 18\nApr: 40\nMay: 35',
  tree: 'School\n  Maths\n    Algebra: 5\n    Geometry: 3\n  Science\n    Physics: 4\n    Biology: 6',
  diagram: 'Plant\n  Roots\n  Stem\n  Leaves\n    Chlorophyll\n    Stomata'
};

export function parseChartData(t) {
  return t.split('\n').filter(l => l.trim()).map(l => {
    const d = l.match(/^[ \t]*/)[0].replace(/\t/g, '  ').length >> 1;
    let s = l.trim(), v = null, m;
    if (m = s.match(/^(.*?)[:\s]\s*(-?\d+(?:\.\d+)?)\s*%?$/)) {
      s = m[1].replace(/[:\-\s]+$/, '') || s;
      v = +m[2];
    }
    return { n: s, v, d };
  });
}

export function toTree(rows) {
  const root = { n: '', kids: [], d: -1 }, st = [root];
  rows.forEach(r => {
    const x = { n: r.n, v: r.v, kids: [], d: r.d };
    while (st[st.length - 1].d >= x.d) st.pop();
    st[st.length - 1].kids.push(x);
    st.push(x);
  });
  return root.kids.length === 1 ? root.kids[0] : { n: 'Root', kids: root.kids, v: null };
}

const sumTree = n => n.kids.length ? n.kids.reduce((a, k) => a + sumTree(k), 0) : (n.v == null ? 1 : Math.max(n.v, 0));
const nm = (s, n = 14) => esc(s.length > n ? s.slice(0, n - 1) + '…' : s);

export function pickChartType(r) {
  if (r.some(x => x.d > 0)) return 'diagram';
  if (r.length && r.every(x => x.v != null && x.v >= 0)) return r.length > 1 && r.length <= 6 ? 'pie' : 'bar';
  return 'diagram';
}

export function renderChartSvg(type, txt) {
  const r = parseChartData(txt);
  if (!r.length) return '<p>Add some data.</p>';
  if (type === 'auto') type = pickChartType(r);
  try {
    return {
      bar: () => renderBarChart(r),
      line: () => renderLineChart(r),
      pie: () => renderPieChart(r),
      tree: () => renderTreemapChart(toTree(r)),
      diagram: () => renderDiagramChart(toTree(r))
    }[type]();
  } catch (e) {
    return '<p>Could not draw this data.</p>';
  }
}

function chartAxes(r, W, H, m) {
  const mx = Math.max(...r.map(x => x.v), 1);
  let g = '';
  for (let i = 0; i <= 4; i++) {
    const y = H - m - (H - m - 20) * i / 4;
    g += `<line x1="${m}" x2="${W - 6}" y1="${y}" y2="${y}" stroke="#999" opacity=".3"/><text x="${m - 4}" y="${y + 4}" font-size="10" text-anchor="end">${+(mx * i / 4).toFixed(1)}</text>`;
  }
  return { g, mx };
}

function renderBarChart(r) {
  r = r.map(x => ({ n: x.n, v: x.v == null ? 1 : x.v }));
  const W = Math.max(380, r.length * 70 + 60), H = 260, m = 36, { g, mx } = chartAxes(r, W, H, m), bw = (W - m - 10) / r.length;
  let o = g;
  r.forEach((x, i) => {
    const h = (H - m - 20) * x.v / mx, bx = m + i * bw + bw * .15;
    o += `<rect x="${bx}" y="${H - m - h}" width="${bw * .7}" height="${h}" rx="4" fill="${PAL[i % 10]}"/><text x="${bx + bw * .35}" y="${H - m - h - 4}" font-size="11" text-anchor="middle">${x.v}</text><text x="${bx + bw * .35}" y="${H - m + 16}" font-size="11" text-anchor="middle">${nm(x.n, Math.floor(bw / 6.5))}</text>`;
  });
  return `<svg viewBox="0 0 ${W} ${H}" width="${W}">${o}</svg>`;
}

function renderLineChart(r) {
  r = r.map(x => ({ n: x.n, v: x.v == null ? 1 : x.v }));
  const W = Math.max(380, r.length * 70 + 60), H = 260, m = 36, { g, mx } = chartAxes(r, W, H, m), st = (W - m - 30) / Math.max(r.length - 1, 1);
  let p = '', d = '';
  r.forEach((x, i) => {
    const X = m + 15 + i * st, Y = H - m - (H - m - 20) * x.v / mx;
    p += (i ? 'L' : 'M') + X + ' ' + Y;
    d += `<circle cx="${X}" cy="${Y}" r="4" fill="#2563eb"/><text x="${X}" y="${Y - 9}" font-size="11" text-anchor="middle">${x.v}</text><text x="${X}" y="${H - m + 16}" font-size="11" text-anchor="middle">${nm(x.n, 10)}</text>`;
  });
  return `<svg viewBox="0 0 ${W} ${H}" width="${W}">${g}<path d="${p}" fill="none" stroke="#2563eb" stroke-width="2.5"/>${d}</svg>`;
}

function renderPieChart(r) {
  r = r.map(x => ({ n: x.n, v: Math.max(x.v == null ? 1 : x.v, 0) }));
  const T = r.reduce((a, x) => a + x.v, 0) || 1, R = 95, C = 110;
  let a = -Math.PI / 2, g = '', lg = '';
  r.forEach((x, i) => {
    const f = x.v / T, b = a + f * 2 * Math.PI, c = PAL[i % 10];
    if (f >= .999) g += `<circle cx="${C}" cy="${C}" r="${R}" fill="${c}"/>`;
    else if (f > 0) {
      const p = q => [C + R * Math.cos(q), C + R * Math.sin(q)], [x1, y1] = p(a), [x2, y2] = p(b);
      g += `<path d="M${C} ${C}L${x1} ${y1}A${R} ${R} 0 ${f > .5 ? 1 : 0} 1 ${x2} ${y2}Z" fill="${c}" stroke="#fff" stroke-width="2"/>`;
    }
    lg += `<rect x="235" y="${20 + i * 22}" width="12" height="12" rx="3" fill="${c}"/><text x="253" y="${31 + i * 22}" font-size="12">${nm(x.n, 18)} — ${(f * 100).toFixed(0)}%</text>`;
    a = b;
  });
  return `<svg viewBox="0 0 410 ${Math.max(220, 40 + r.length * 22)}" width="430">${g}${lg}</svg>`;
}

function renderDiagramChart(root) {
  let leaf = 0, md = 0;
  const pos = (n, d) => {
    n.dp = d;
    md = Math.max(md, d);
    if (!n.kids.length) n.x = leaf++;
    else {
      n.kids.forEach(k => pos(k, d + 1));
      n.x = (n.kids[0].x + n.kids[n.kids.length - 1].x) / 2;
    }
  };
  pos(root, 0);
  const cw = 112, ch = 70, bw = 100, bh = 30, W = Math.max(leaf, 1) * cw + 20, H = (md + 1) * ch + 20;
  let ln = '', nd = '';
  const draw = n => {
    const x = 10 + n.x * cw + cw / 2, y = 15 + n.dp * ch;
    n.kids.forEach(k => {
      const kx = 10 + k.x * cw + cw / 2, ky = 15 + k.dp * ch;
      ln += `<path d="M${x} ${y + bh}C${x} ${y + bh + 20} ${kx} ${ky - 20} ${kx} ${ky}" fill="none" stroke="#888" stroke-width="1.5"/>`;
      draw(k);
    });
    nd += `<rect x="${x - bw / 2}" y="${y}" width="${bw}" height="${bh}" rx="8" fill="${PAL[n.dp % 10]}"/><text x="${x}" y="${y + 19}" font-size="12" text-anchor="middle" style="fill:#fff">${nm(n.n + (n.v != null && !n.kids.length ? ' (' + n.v + ')' : ''), 16)}</text>`;
  };
  draw(root);
  return `<svg viewBox="0 0 ${W} ${H}" width="${W}">${ln}${nd}</svg>`;
}

function renderTreemapChart(root) {
  const W = 520, H = 300;
  let o = '', k = 0;
  const lay = (n, x, y, w, h, d, col) => {
    const c = d === 1 ? PAL[k++ % 10] : col;
    if (!n.kids.length) {
      o += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${c}" stroke="#fff" stroke-width="2" rx="4"/>`;
      if (w > 46 && h > 20) o += `<text x="${x + 6}" y="${y + 16}" font-size="11" style="fill:#fff">${nm(n.n, Math.floor(w / 6.5))}</text>`;
      return;
    }
    const t = sumTree(n) || 1;
    let off = 0;
    n.kids.forEach(ch => {
      const f = sumTree(ch) / t;
      w >= h ? lay(ch, x + off * w, y, w * f, h, d + 1, c) : lay(ch, x, y + off * h, w, h * f, d + 1, c);
      off += f;
    });
  };
  lay(root, 0, 0, W, H, 0, PAL[0]);
  return `<svg viewBox="0 0 ${W} ${H}" width="${W}">${o}</svg>`;
}
