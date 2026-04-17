/* SwiftDeal Service Worker v9.0.0.0 — Network-First for HTML */
const CACHE_NAME = 'swift-v9.2.0.0';
const STATIC_ASSETS = [
  '/icons/icon-72x72.png',
  '/icons/icon-96x96.png',
  '/icons/icon-128x128.png',
  '/icons/icon-144x144.png',
  '/icons/icon-152x152.png',
  '/icons/icon-192x192.png',
  '/icons/icon-384x384.png',
  '/icons/icon-512x512.png',
  '/offline.html'
];

/* Install — cache only static assets (icons + offline page) */
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(STATIC_ASSETS))
  );
  self.skipWaiting();
});

/* Activate — DELETE all old caches (including PWABuilder's old caches) */
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(names =>
      Promise.all(names.filter(n => n !== CACHE_NAME).map(n => caches.delete(n)))
    ).then(() => self.clients.claim())
  );
});

/* Fetch — NETWORK-FIRST for HTML, cache-first for static assets only */
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  /* NEVER cache HTML files — always go to network */
  if (event.request.mode === 'navigate' || 
      url.pathname.endsWith('.html') || 
      url.pathname === '/' ||
      event.request.headers.get('accept')?.includes('text/html')) {
    event.respondWith(
      fetch(event.request).catch(() => caches.match('/offline.html'))
    );
    return;
  }

  /* NEVER cache JS files — always fresh */
  if (url.pathname.endsWith('.js') || url.pathname.endsWith('.json')) {
    event.respondWith(fetch(event.request).catch(() => caches.match(event.request)));
    return;
  }

  /* Static assets (icons only) — cache-first */
  if (url.pathname.startsWith('/icons/')) {
    event.respondWith(
      caches.match(event.request).then(cached => cached || fetch(event.request))
    );
    return;
  }

  /* Everything else — network with no caching */
  event.respondWith(fetch(event.request));
});
