
(function () {
  const stored = localStorage.getItem('reconx-theme') || 'light';
  document.documentElement.dataset.theme = stored;

  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('theme-toggle');
    btn && btn.addEventListener('click', () => {
      const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
      document.documentElement.dataset.theme = next;
      localStorage.setItem('reconx-theme', next);
    });
  });
})();