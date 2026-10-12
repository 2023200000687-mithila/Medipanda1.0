importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-app-compat.js','https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging-compat.js');
try{firebase.initializeApp(JSON.parse(atob(new URL(self.location).searchParams.get('cfg'))));firebase.messaging().onBackgroundMessage(m=>self.registration.showNotification(m.notification?.title||'MediPanda',{body:m.notification?.body||'',icon:'/favicon.ico',data:m.data}))}catch(e){}
self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow('/'))});
