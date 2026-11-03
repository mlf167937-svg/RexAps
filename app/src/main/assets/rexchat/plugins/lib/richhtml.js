export function escapeText(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

export function bold(value) {
  return `*${String(value)}*`
}

export function italic(value) {
  return `_${String(value)}_`
}

export function monospace(value) {
  return `\`${String(value)}\``
}

export function quote(value) {
  return String(value).split('\n').map(line => `> ${line}`).join('\n')
}
