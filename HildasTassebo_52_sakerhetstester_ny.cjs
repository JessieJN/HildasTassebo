// Hildas Tassebo – lokal Firestore-regeltestsvit.
// Kör: node .\testa_alla_firestore_regler.cjs
// Kräver att emulatorerna körs med --project demo-hildastassebo.
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, updateDoc, deleteDoc, getDocs, collection,
  runTransaction, Timestamp, serverTimestamp, arrayUnion, arrayRemove } = require('firebase/firestore');

const PROJECT = 'demo-hildastassebo';
const U = 'suite-volontar', V = 'suite-annan', A = 'suite-admin';
const DATE = '2030-03-15', NEAR = '2030-03-16';
const localDayOffset = days => {
  const d = new Date(Date.now() + days * 86400000);
  return new Intl.DateTimeFormat('sv-SE', {timeZone:'Europe/Stockholm',year:'numeric',month:'2-digit',day:'2-digit'}).format(d);
};
const CLOSE = localDayOffset(3);
const BORDER = localDayOffset(7);
const dayPath = date => `arbetspass/${date}`;
const bookingPath = (date, type, uid) => `bokningar/${date}_${type}_${uid}`;
const dayData = (date, morning = [], afternoon = [], aloneM = false, aloneA = false) => ({
  datum: Timestamp.fromDate(new Date(`${date}T00:00:00+01:00`)),
  'förmiddag': '', 'eftermiddag': '',
  'förmiddag_bokade': morning, 'eftermiddag_bokade': afternoon,
  'förmiddag_ensam': aloneM, 'eftermiddag_ensam': aloneA
});
const bookingData = (date, type, uid, alone = false, time = serverTimestamp()) => ({
  'användar_id': uid, 'arbetspass_id': date, 'passtyp': type,
  'bokad_tidpunkt': time, 'önskar_arbeta_själv': alone
});
const noteData = (uid, isAdmin) => ({
  datum: DATE, text: 'Testanteckning', 'författar_id': uid,
  'författarnamn': 'Testperson', 'skapad_tidpunkt': serverTimestamp(),
  'admin_anteckning': isAdmin
});
const eventData = uid => ({
  datum: Timestamp.fromDate(new Date('2030-03-15T12:00:00Z')),
  titel: 'Testevenemang', beskrivning: 'Test', 'skapad_av': uid,
  starttid: null, sluttid: null
});

(async () => {
  let env;
  let passed = 0, failed = 0;
  const errors = [];
  try {
    env = await initializeTestEnvironment({
      projectId: PROJECT,
      firestore: { host: '127.0.0.1', port: 8080 }
    });
    // Isolerad lokal emulator. Rensar endast emulatorns Firestore-data, aldrig molnet.
    await env.clearFirestore();
    const seed = async fn => env.withSecurityRulesDisabled(async c => fn(c.firestore()));
    await seed(async db => {
      for (const [uid, role] of [[U, 0], [V, 0], [A, 1]]) {
        await setDoc(doc(db, 'användare', uid), {
          'förnamn': 'Test', 'efternamn': uid, 'epost': `${uid}@example.com`, roll: role
        });
      }
    });
    const volunteer = env.authenticatedContext(U, { email: `${U}@example.com`, email_verified: true }).firestore();
    const other = env.authenticatedContext(V, { email: `${V}@example.com`, email_verified: true }).firestore();
    const admin = env.authenticatedContext(A, { email: `${A}@example.com`, email_verified: true }).firestore();
    const guest = env.unauthenticatedContext().firestore();

    async function test(label, fn) {
      try { await fn(); passed++; console.log(`GODKÄNT  ${label}`); }
      catch (e) { failed++; errors.push({label, error: e.message}); console.error(`MISSLYCKADES  ${label}: ${e.code || ''} ${e.message}`); }
    }
    async function expectDenied(promise) { await assertFails(promise); }
    async function expectAllowed(promise) { return assertSucceeds(promise); }
    async function book(db, date, type, uid, alone = false) {
      const morning = type === 'MORNING';
      const field = morning ? 'förmiddag_bokade' : 'eftermiddag_bokade';
      const aloneField = morning ? 'förmiddag_ensam' : 'eftermiddag_ensam';
      return runTransaction(db, async tx => {
        const d = doc(db, dayPath(date)), b = doc(db, bookingPath(date, type, uid));
        const snap = await tx.get(d);
        const booked = await tx.get(b);
        if (booked.exists()) throw new Error('Bokningen finns redan');
        if (!snap.exists()) tx.set(d, dayData(date));
        tx.set(d, { [field]: arrayUnion(uid), [aloneField]: alone }, { merge: true });
        tx.set(b, bookingData(date, type, uid, alone));
      });
    }
    async function cancel(db, date, type, uid) {
      const morning = type === 'MORNING';
      const field = morning ? 'förmiddag_bokade' : 'eftermiddag_bokade';
      const aloneField = morning ? 'förmiddag_ensam' : 'eftermiddag_ensam';
      return runTransaction(db, async tx => {
        const d = doc(db, dayPath(date)), b = doc(db, bookingPath(date, type, uid));
        const snap = await tx.get(d), booked = await tx.get(b);
        if (!snap.exists() || !booked.exists()) throw new Error('Testbokning saknas');
        const old = snap.data()[field] || [];
        const update = { [field]: arrayRemove(uid) };
        if (old.length === 1) update[aloneField] = false;
        tx.update(d, update);
        tx.delete(b);
      });
    }

    await test('Oinloggad får inte läsa användare', async () => expectDenied(getDocs(collection(guest, 'användare'))));
    await test('Volontär får läsa profil', async () => {
      const s = await expectAllowed(getDoc(doc(volunteer, 'användare', U)));
      if (!s.exists()) throw new Error('Profilen saknas');
    });
    await test('Volontär får inte göra sig till admin', async () => expectDenied(updateDoc(doc(volunteer, 'användare', U), { roll: 1 })));
    await test('Admin får ändra annan volontärs namn', async () => expectAllowed(updateDoc(doc(admin, 'användare', V), { 'förnamn': 'Ändrad' })));
    await test('Volontär får inte ändra annans namn', async () => expectDenied(updateDoc(doc(volunteer, 'användare', V), { 'förnamn': 'Hackat' })));
    await test('Volontär får boka eget pass', async () => {
      await expectAllowed(book(volunteer, DATE, 'MORNING', U));
      const d = await getDoc(doc(volunteer, dayPath(DATE)));
      if (!d.data()['förmiddag_bokade'].includes(U)) throw new Error('Saknas i arbetspass');
    });
    await test('Volontär får inte boka åt annan', async () => expectDenied(book(volunteer, DATE, 'AFTERNOON', V)));
    await test('Admin får boka åt annan', async () => expectAllowed(book(admin, DATE, 'AFTERNOON', V)));
    await test('Volontär får inte avboka annans pass', async () => expectDenied(cancel(volunteer, DATE, 'AFTERNOON', V)));
    await test('Admin får avboka annans pass', async () => {
      await expectAllowed(cancel(admin, DATE, 'AFTERNOON', V));
      if ((await getDoc(doc(admin, bookingPath(DATE, 'AFTERNOON', V)))).exists()) throw new Error('Bokningen finns kvar');
    });
    await test('Volontär får avboka eget pass långt i förväg', async () => {
      await expectAllowed(cancel(volunteer, DATE, 'MORNING', U));
      if ((await getDoc(doc(volunteer, bookingPath(DATE, 'MORNING', U)))).exists()) throw new Error('Bokningen finns kvar');
    });
    await test('Volontär får inte ändra en boknings ägare', async () => {
      await seed(async db => {
        await setDoc(doc(db, dayPath(NEAR)), dayData(NEAR, [U]));
        await setDoc(doc(db, bookingPath(NEAR, 'MORNING', U)), bookingData(NEAR, 'MORNING', U, false, Timestamp.now()));
      });
      return expectDenied(updateDoc(doc(volunteer, bookingPath(NEAR, 'MORNING', U)), { 'användar_id': V }));
    });
    await test('Volontär får skapa egen anteckning', async () => expectAllowed(setDoc(doc(volunteer, 'anteckningar/suite-note-u'), noteData(U, false))));
    await test('Volontär får inte ändra annans anteckning', async () => {
      await seed(db => setDoc(doc(db, 'anteckningar/suite-note-v'), {
        ...noteData(V, false), 'skapad_tidpunkt': Timestamp.now()
      }));
      return expectDenied(updateDoc(doc(volunteer, 'anteckningar/suite-note-v'), { text: 'Otillåten ändring' }));
    });
    await test('Admin får ändra annans anteckning', async () => expectAllowed(updateDoc(doc(admin, 'anteckningar/suite-note-v'), { text: 'Adminändring' })));
    await test('Volontär får inte skapa adminanteckning', async () => expectDenied(setDoc(doc(volunteer, 'anteckningar/suite-fake-admin-note'), noteData(U, true))));
    await test('Volontär får skapa eget evenemang', async () => expectAllowed(setDoc(doc(volunteer, 'evenemang/suite-event-u'), eventData(U))));
    await test('Volontär får inte ändra annans evenemang', async () => {
      await seed(db => setDoc(doc(db, 'evenemang/suite-event-v'), eventData(V)));
      return expectDenied(updateDoc(doc(volunteer, 'evenemang/suite-event-v'), { titel: 'Otillåten ändring' }));
    });
    await test('Admin får ändra annans evenemang', async () => expectAllowed(updateDoc(doc(admin, 'evenemang/suite-event-v'), { titel: 'Adminändring' })));
    await test('Volontär får inte byta skapare av evenemang', async () => expectDenied(updateDoc(doc(volunteer, 'evenemang/suite-event-u'), { 'skapad_av': V })));
    await test('Oinloggad får inte läsa anteckningar', async () => expectDenied(getDocs(collection(guest, 'anteckningar'))));
    await test('Oinloggad får inte läsa evenemang', async () => expectDenied(getDocs(collection(guest, 'evenemang'))));
    await test('Okänd samling är spärrad', async () => expectDenied(setDoc(doc(volunteer, 'hemlig_samling/suite-test'), { test: true })));

    // Ytterligare tester: körs i SAMMA fil och mot SAMMA lokala emulator.
    await test('Volontär får inte avboka pass om tre dagar', async () => {
      await seed(async db => {
        await setDoc(doc(db, dayPath(CLOSE)), dayData(CLOSE, [U]));
        await setDoc(doc(db, bookingPath(CLOSE, 'MORNING', U)), bookingData(CLOSE, 'MORNING', U, false, Timestamp.now()));
      });
      await expectDenied(cancel(volunteer, CLOSE, 'MORNING', U));
      if (!(await getDoc(doc(volunteer, bookingPath(CLOSE, 'MORNING', U)))).exists()) throw new Error('Bokningen försvann');
    });
    await test('Volontär får inte avboka exakt sju kalenderdagar framåt', async () => {
      await seed(async db => {
        await setDoc(doc(db, dayPath(BORDER)), dayData(BORDER, [U]));
        await setDoc(doc(db, bookingPath(BORDER, 'MORNING', U)), bookingData(BORDER, 'MORNING', U, false, Timestamp.now()));
      });
      await expectDenied(cancel(volunteer, BORDER, 'MORNING', U));
    });
    await test('Admin får avboka ett pass inom sjudagarsgränsen', async () => {
      await expectAllowed(cancel(admin, CLOSE, 'MORNING', U));
      if ((await getDoc(doc(admin, bookingPath(CLOSE, 'MORNING', U)))).exists()) throw new Error('Adminavbokningen sparades inte');
    });
    await test('Volontär får boka ensam på ett tomt pass', async () => {
      await expectAllowed(book(volunteer, '2030-04-01', 'MORNING', U, true));
      const d = (await getDoc(doc(volunteer, dayPath('2030-04-01')))).data();
      if (!d['förmiddag_ensam'] || d['förmiddag_bokade'].length !== 1) throw new Error('Ensammarkeringen är fel');
    });
    await test('Annan volontär får inte boka på ensampass', async () => {
      await expectDenied(book(other, '2030-04-01', 'MORNING', V));
    });
    await test('Volontär får inte önska ensam när någon redan är bokad', async () => {
      await expectAllowed(book(volunteer, '2030-04-02', 'MORNING', U));
      await expectDenied(book(other, '2030-04-02', 'MORNING', V, true));
    });
    await test('Två volontärer får boka samma pass utan ensamönskemål', async () => {
      await expectAllowed(book(volunteer, '2030-04-03', 'MORNING', U));
      await expectAllowed(book(other, '2030-04-03', 'MORNING', V));
      const ids = (await getDoc(doc(volunteer, dayPath('2030-04-03')))).data()['förmiddag_bokade'];
      if (!ids.includes(U) || !ids.includes(V)) throw new Error('Någon av volontärerna saknas');
    });
    await test('Avbokning av en av två lämnar den andra kvar', async () => {
      await expectAllowed(cancel(volunteer, '2030-04-03', 'MORNING', U));
      const ids = (await getDoc(doc(volunteer, dayPath('2030-04-03')))).data()['förmiddag_bokade'];
      if (ids.length !== 1 || ids[0] !== V) throw new Error('Fel användare kvar efter avbokning');
    });
    await test('Ensammarkering återställs när sista bokningen tas bort', async () => {
      await expectAllowed(cancel(volunteer, '2030-04-01', 'MORNING', U));
      const d = (await getDoc(doc(volunteer, dayPath('2030-04-01')))).data();
      if (d['förmiddag_ensam'] !== false || d['förmiddag_bokade'].length) throw new Error('Passet återställdes inte');
    });
    await test('Volontär får inte lägga till sig i arbetspass utan bokningsdokument', async () => {
      await expectDenied(updateDoc(doc(volunteer, dayPath('2030-04-02')), {'förmiddag_bokade': arrayUnion(V)}));
    });
    await test('Volontär får inte ta bort annans bokning genom att ändra arbetspasset', async () => {
      await expectDenied(updateDoc(doc(volunteer, dayPath('2030-04-03')), {'förmiddag_bokade': arrayRemove(V)}));
    });
    await test('Volontär får inte skapa en bokning utan att uppdatera arbetspasset', async () => {
      await expectDenied(setDoc(doc(volunteer, bookingPath('2030-04-05', 'MORNING', U)), bookingData('2030-04-05', 'MORNING', U)));
    });
    await test('Volontär får inte ändra passets datum eller beskrivning', async () => {
      await expectDenied(updateDoc(doc(volunteer, dayPath('2030-04-02')), {'förmiddag':'Manipulerad'}));
    });
    await test('Volontär får inte radera ett helt arbetspass', async () => {
      await expectDenied(deleteDoc(doc(volunteer, dayPath('2030-04-02'))));
    });
    await test('Volontär får inte radera en annans anteckning', async () => {
      await expectDenied(deleteDoc(doc(volunteer, 'anteckningar/suite-note-v')));
    });
    await test('Volontär får radera sin egen anteckning', async () => {
      await expectAllowed(deleteDoc(doc(volunteer, 'anteckningar/suite-note-u')));
    });
    await test('Volontär får inte radera en annans evenemang', async () => {
      await expectDenied(deleteDoc(doc(volunteer, 'evenemang/suite-event-v')));
    });
    await test('Volontär får radera sitt eget evenemang', async () => {
      await expectAllowed(deleteDoc(doc(volunteer, 'evenemang/suite-event-u')));
    });
    await test('Admin får radera en annans anteckning', async () => {
      await expectAllowed(deleteDoc(doc(admin, 'anteckningar/suite-note-v')));
    });
    await test('Admin får radera en annans evenemang', async () => {
      await expectAllowed(deleteDoc(doc(admin, 'evenemang/suite-event-v')));
    });
    await test('Volontär får inte skapa profil med adminroll', async () => {
      const fake = env.authenticatedContext('suite-ny-admin', {email:'suite-ny-admin@example.com'}).firestore();
      await expectDenied(setDoc(doc(fake, 'användare/suite-ny-admin'), {
        'förnamn':'Ny','efternamn':'Admin','epost':'suite-ny-admin@example.com',roll:1
      }));
    });
    await test('Ny användare får skapa egen profil med volontärroll', async () => {
      const newcomer = env.authenticatedContext('suite-ny', {email:'suite-ny@example.com'}).firestore();
      await expectAllowed(setDoc(doc(newcomer, 'användare/suite-ny'), {
        'förnamn':'Ny','efternamn':'Volontär','epost':'suite-ny@example.com',roll:0
      }));
    });
    await test('Volontär får inte radera en annans profil', async () => {
      await expectDenied(deleteDoc(doc(volunteer, 'användare', V)));
    });
    await test('Kontoradering: markerad användare får radera egen anteckning', async () => {
      await seed(async db => {
        await setDoc(doc(db, 'användare/suite-raderas'), {
          'förnamn':'Radera','efternamn':'Mig','epost':'raderas@example.com',roll:0,
          'kontoradering_pågår':true
        });
        await setDoc(doc(db, 'anteckningar/suite-delete-note'), {
          ...noteData('suite-raderas',false), 'skapad_tidpunkt':Timestamp.now()
        });
      });
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectAllowed(deleteDoc(doc(deletingDb, 'anteckningar/suite-delete-note')));
    });
    await test('Kontoradering: markerad användare får radera eget evenemang', async () => {
      await seed(db => setDoc(doc(db, 'evenemang/suite-delete-event'), eventData('suite-raderas')));
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectAllowed(deleteDoc(doc(deletingDb, 'evenemang/suite-delete-event')));
    });
    await test('Kontoradering: markerad användare får rensa eget arbetspass', async () => {
      await seed(async db => {
        await setDoc(doc(db, dayPath('2030-04-10')), dayData('2030-04-10',['suite-raderas']));
        await setDoc(doc(db, bookingPath('2030-04-10','MORNING','suite-raderas')),
          bookingData('2030-04-10','MORNING','suite-raderas',false,Timestamp.now()));
      });
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectAllowed(updateDoc(doc(deletingDb, dayPath('2030-04-10')),
        {'förmiddag_bokade':arrayRemove('suite-raderas'),'förmiddag_ensam':false}));
      await expectAllowed(deleteDoc(doc(deletingDb, bookingPath('2030-04-10','MORNING','suite-raderas'))));
    });
    await test('Kontoradering: markerad användare får inte skapa nytt evenemang', async () => {
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectDenied(setDoc(doc(deletingDb, 'evenemang/suite-delete-new-event'), eventData('suite-raderas')));
    });
    await test('Kontoradering: användare får radera sin egen profil', async () => {
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectAllowed(deleteDoc(doc(deletingDb, 'användare/suite-raderas')));
    });
    await test('Kontoradering: egen bokning kan raderas även om profilen saknas', async () => {
      await seed(db => setDoc(doc(db, bookingPath('2030-04-11','MORNING','suite-raderas')),
        bookingData('2030-04-11','MORNING','suite-raderas',false,Timestamp.now())));
      const deletingDb = env.authenticatedContext('suite-raderas').firestore();
      await expectAllowed(deleteDoc(doc(deletingDb, bookingPath('2030-04-11','MORNING','suite-raderas'))));
    });

    console.log(`\nRESULTAT: ${passed} godkända, ${failed} misslyckade, totalt ${passed + failed}.`);
    if (failed) {
      console.log('Misslyckade tester:');
      for (const e of errors) console.log(` - ${e.label}: ${e.error}`);
      process.exitCode = 1;
    } else {
      console.log('Alla tester i denna fil klarade sig. Detta är inte en fullständig säkerhetsgranskning.');
    }
  } catch (e) {
    console.error('Kunde inte starta testsviten:', e.code || '', e.message);
    process.exitCode = 1;
  } finally {
    if (env) await env.cleanup();
  }
})();
