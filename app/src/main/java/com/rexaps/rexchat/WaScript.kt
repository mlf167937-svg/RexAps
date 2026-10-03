package com.rexaps.rexchat

/** JS helpers injected into WhatsApp Web. Best-effort: WA can change its DOM at any time. */
object WaScript {
    val JS = """
(function () {
  var R = window.__rex = window.__rex || {};

  R.state = function () {
    if (document.querySelector('[data-link-code]')) return 'code';
    if (document.querySelector('#pane-side')) return 'chats';
    if (document.querySelector('canvas')) return 'qr';
    return 'loading';
  };

  R.viewport = function (w) {
    var m = document.querySelector('meta[name=viewport]');
    if (!m) { m = document.createElement('meta'); m.name = 'viewport'; document.head.appendChild(m); }
    var c = w > 0 ? ('width=' + w) : 'width=device-width, initial-scale=1';
    if (m.content !== c) m.content = c;
    return 'ok';
  };

  R.clickText = function (phrases) {
    var els = document.querySelectorAll('button,[role=button],a,span,div');
    for (var i = 0; i < els.length; i++) {
      var e = els[i];
      if (e.childElementCount !== 0) continue;
      var t = (e.textContent || '').trim().toLowerCase();
      if (t && phrases.indexOf(t) >= 0) {
        (e.closest('button,[role=button],a') || e).click();
        return true;
      }
    }
    return false;
  };

  R.phoneInput = function () {
    var ins = document.querySelectorAll('input[type=text],input[type=tel],input[type=number]');
    for (var i = 0; i < ins.length; i++) {
      if (ins[i].offsetParent !== null) return ins[i];
    }
    return null;
  };

  R.openPhone = function () {
    if (R.phoneInput()) return 'ready';
    var ok = R.clickText([
      'link with phone number', 'log in with phone number',
      'link with phone number instead',
      'tautkan dengan nomor telepon', 'masuk dengan nomor telepon'
    ]);
    return ok ? 'clicked' : 'missing';
  };

  R.fill = function (num) {
    var i = R.phoneInput();
    if (!i) return 'noinput';
    i.focus();
    var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(i, num);
    i.dispatchEvent(new Event('input', { bubbles: true }));
    return 'ok';
  };

  R.next = function () {
    return R.clickText(['next', 'berikutnya', 'lanjut', 'selanjutnya']) ? 'ok' : 'missing';
  };

  R.code = function () {
    var c = document.querySelector('[data-link-code]');
    if (c) return c.getAttribute('data-link-code') || '';
    var m = (document.body.innerText || '').match(/\b[A-Z0-9]{4}-[A-Z0-9]{4}\b/);
    return m ? m[0] : '';
  };

  R.qr = function () {
    var c = document.querySelector('canvas');
    try { return c ? c.toDataURL('image/png') : ''; } catch (e) { return ''; }
  };
})();
""".trimIndent()
}