// Preferences page: dark-mode switch (stored in localStorage) and language switch (?lang=xx -> cookie).
// The saved theme itself is applied by the inline script in the <head> of every page.
(function () {
  var root = document.documentElement;

  var themeSwitch = document.getElementById('themeSwitch');
  if (themeSwitch) {
    themeSwitch.checked = root.getAttribute('data-theme') === 'dark';
    themeSwitch.addEventListener('change', function () {
      var dark = themeSwitch.checked;
      try { localStorage.setItem('dark_mode', dark ? 'true' : 'false'); } catch (e) { /* ignore */ }
      if (dark) { root.setAttribute('data-theme', 'dark'); } else { root.removeAttribute('data-theme'); }
    });
  }

  var languageSwitch = document.getElementById('languageSwitch');
  if (languageSwitch) {
    languageSwitch.addEventListener('change', function () {
      window.location.href = languageSwitch.getAttribute('data-url') + '?lang=' + (languageSwitch.checked ? 'fr' : 'en');
    });
  }
})();
