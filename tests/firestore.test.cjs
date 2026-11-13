const { readFileSync } = require('node:fs');
const { test, before, after, beforeEach } = require('node:test');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, deleteDoc, collection, query, where, getDocs } = require('firebase/firestore');
let env;
before(async () => {
  if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('An emulator is required');
  env = await initializeTestEnvironment({ projectId: 'demo-movies-portfolio', firestore: {
    rules: readFileSync('firestore.rules', 'utf8')
  }});
});
beforeEach(async () => env.clearFirestore());
after(async () => { if (env) await env.cleanup(); });
for (const [name, ownerField] of [['users_favorites', 'userId'], ['users_feedback', 'uid']]) {
  test(`${name}: ownership cannot be hijacked or transferred`, async () => {
    const owner = env.authenticatedContext('alice').firestore();
    const stranger = env.authenticatedContext('bob').firestore();
    const path = `${name}/alice_42`;
    await assertSucceeds(setDoc(doc(owner, path), { [ownerField]: 'alice', movieId: 42 }));
    await assertFails(setDoc(doc(stranger, path), { [ownerField]: 'bob', movieId: 42 }));
    await assertFails(setDoc(doc(owner, path), { [ownerField]: 'bob', movieId: 42 }));
    await assertFails(getDoc(doc(stranger, path)));
    await assertFails(deleteDoc(doc(stranger, path)));
    await assertSucceeds(getDocs(query(collection(owner, name), where(ownerField, '==', 'alice'))));
    await assertSucceeds(deleteDoc(doc(owner, path)));
  });
}
test('missing favorite is readable as absent, anonymous clients are denied', async () => {
  await assertSucceeds(getDoc(doc(env.authenticatedContext('alice').firestore(), 'users_favorites/alice_99')));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'users_favorites/alice_99')));
});
test('profile and nested feedback are private to their owner', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(setDoc(doc(alice, 'users/alice/feedback/42'), { uid: 'alice' }));
  await assertFails(getDoc(doc(bob, 'users/alice/feedback/42')));
  await assertFails(setDoc(doc(bob, 'users/alice'), { username: 'intruder' }));
});
