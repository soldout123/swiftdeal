/* Firebase Messaging Service Worker — SwiftDeal v9.0.0.0 */
const SW_VERSION = 'v9.0.0.0';
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyCmHg4nOAXC-JaW2wPesh6VJY7v1twiMw4",
    authDomain: "swiftdeal-3cee9.firebaseapp.com",
    projectId: "swiftdeal-3cee9",
    storageBucket: "swiftdeal-3cee9.firebasestorage.app",
    messagingSenderId: "626252184385",
    appId: "1:626252184385:web:b525e1b382c72ccdf43fc0"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    const n = payload.notification || {};
    const d = payload.data || {};
    const title = n.title || d.title || 'SwiftDeal';
    return self.registration.showNotification(title, {
        body: n.body || d.body || 'You have a new notification',
        icon: '/icons/icon-192x192.png',
        badge: '/icons/icon-72x72.png',
        vibrate: [300, 100, 300, 100, 300],
        requireInteraction: true,
        renotify: true,
        tag: 'swift-push-' + Date.now(),
        actions: [{ action: 'open', title: 'Open SwiftDeal' }]
    });
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
            for (const c of list) { if ('focus' in c) return c.focus(); }
            return clients.openWindow('/');
        })
    );
});

self.addEventListener('install', () => {
    console.log('[SW] Installing ' + SW_VERSION);
    self.skipWaiting();
});
self.addEventListener('activate', (e) => e.waitUntil(
    caches.keys().then(names => Promise.all(names.map(n => caches.delete(n)))).then(() => clients.claim())
));
