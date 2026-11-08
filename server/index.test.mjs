import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import { createChatServer } from './index.mjs';

test('chat rejects unauthenticated requests and forwards grounded, contextual conversation', async (t) => {
  const token = 'test-travel-token-24-chars';
  const calls = [];
  const server = createChatServer({
    key: 'test-key', token, grounding: 'search-maps',
    callGemini: async (url, options) => {
      calls.push({ url, headers: options.headers, body: JSON.parse(options.body) });
      return Response.json({ candidates: [{
        content: { parts: [{ text: 'internal', thought: true }, { text: 'Gợi ý ghé Tràng An.' }] },
        groundingMetadata: {
          groundingChunks: [
            { maps: { title: 'Tràng An', uri: 'https://maps.google.com/?cid=123' } },
            { web: { title: 'Trang thông tin', uri: 'https://example.com/dia-diem' } },
            { web: { title: 'Unsafe', uri: 'javascript:alert(1)' } },
          ],
          searchEntryPoint: { renderedContent: '<div>Google Search</div>' },
        },
      }] });
    },
  }).listen(0, '127.0.0.1');
  t.after(() => server.close());
  await once(server, 'listening');
  const url = 'http://127.0.0.1:' + server.address().port + '/api/chat';
  const question = { role: 'user', text: 'Đi đâu?' };
  const post = (authorization, body) => fetch(url, {
    method: 'POST', headers: { authorization, 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  });

  assert.equal((await post('Bearer invalid', { messages: [question] })).status, 401);
  assert.equal((await post('Bearer ' + token, { messages: [{ ...question, role: 'assistant' }] })).status, 400);
  assert.equal((await post('Bearer ' + token, { messages: [question], extra: 'x'.repeat(130000) })).status, 413);
  assert.equal(calls.length, 0);
  const reply = await post('Bearer ' + token, {
    context: 'Ninh Bình, 2 ngày\nNgày 2: 10:00 ghé Bái Đính',
    messages: [question, { role: 'assistant', text: 'Ghé Tràng An.' }, { role: 'user', text: 'Đi vào buổi nào?' }],
  });
  assert.equal(reply.status, 200);
  assert.deepEqual(await reply.json(), {
    text: 'Gợi ý ghé Tràng An.',
    sources: [
      { url: 'https://maps.google.com/?cid=123', title: 'Tràng An', kind: 'maps' },
      { url: 'https://example.com/dia-diem', title: 'Trang thông tin', kind: 'web' },
    ],
    searchHtml: '<div>Google Search</div>',
  });
  assert.equal(calls[0].headers['x-goog-api-key'], 'test-key');
  assert.deepEqual(calls[0].body.tools, [{ googleSearch: {} }, { googleMaps: {} }]);
  assert.deepEqual(calls[0].body.contents.map(turn => turn.role), ['user', 'model', 'user']);
  assert.match(calls[0].body.contents[0].parts[0].text, /Ninh Bình, 2 ngày/);
  assert.match(calls[0].body.contents[0].parts[0].text, /Ngày 2: 10:00 ghé Bái Đính/);
  assert.match(calls[0].body.systemInstruction.parts[0].text, /tra Google Search/);
});

test('places checks the Firebase project and returns scoped results with photo credits', async (t) => {
  const firebaseProject = 'travel-vn-test';
  const jwt = (project) => 'header.' + Buffer.from(JSON.stringify({
    aud: project, iss: 'https://securetoken.google.com/' + project, sub: 'user-1',
  })).toString('base64url') + '.signature';
  const calls = [];
  const server = createChatServer({
    placesKey: 'test-places-key', firebaseKey: 'test-firebase-key', firebaseProject,
    callGoogle: async (url, options) => {
      calls.push(url);
      if (url.includes('accounts:lookup')) {
        assert.equal(JSON.parse(options.body).idToken, jwt(firebaseProject));
        return Response.json({ users: [{ localId: 'user-1' }] });
      }
      assert.equal(options.headers['X-Goog-Api-Key'], 'test-places-key');
      if (url.endsWith(':searchText')) {
        assert.match(JSON.parse(options.body).textQuery, /quán cà phê tại khu vực Lâm Đồng/);
        return Response.json({ places: [{
          displayName: { text: 'Quán thử' }, formattedAddress: 'Lâm Đồng',
          googleMapsUri: 'https://maps.google.com/?cid=12',
          photos: [{ name: 'places/demo/photos/img', authorAttributions: [{ displayName: 'Tác giả' }] }],
        }] });
      }
      assert.match(url, /\/places\/demo\/photos\/img\/media\?/);
      return Response.json({ photoUri: 'https://lh3.googleusercontent.com/test-photo' });
    },
  }).listen(0, '127.0.0.1');
  t.after(() => server.close());
  await once(server, 'listening');
  const post = (token, body) => fetch('http://127.0.0.1:' + server.address().port + '/api/places', {
    method: 'POST', headers: { authorization: 'Bearer ' + token }, body: JSON.stringify(body),
  });
  const query = { province: 'Lâm Đồng', category: 'Cà phê' };
  assert.equal((await post(jwt('other-project'), query)).status, 401);
  assert.equal(calls.length, 0);
  assert.equal((await post(jwt(firebaseProject), { ...query, province: { trim: 'invalid' } })).status, 400);
  const response = await post(jwt(firebaseProject), query);
  assert.equal(response.status, 200);
  const result = await response.json();
  assert.deepEqual(result.places, [{
    title: 'Quán thử', address: 'Lâm Đồng', url: 'https://maps.google.com/?cid=12',
    photoUrl: 'https://lh3.googleusercontent.com/test-photo',
    authors: [{ displayName: 'Tác giả' }], attributions: [],
  }]);
  assert.doesNotMatch(JSON.stringify(result), /test-places-key|test-firebase-key/);
});

test('guest places search returns Google metadata even without photos, while chat stays protected', async (t) => {
  const range = { startPrice: { currencyCode: 'VND', units: '45000' } };
  const server = createChatServer({
    placesKey: 'test-places-key', publicPlaces: true,
    callGoogle: async (url, options) => {
      assert.match(url, /places:searchText$/);
      assert.equal(JSON.parse(options.body).textQuery, 'bún bò tại khu vực Huế, Việt Nam');
      assert.match(options.headers['X-Goog-FieldMask'], /places\.priceRange/);
      return Response.json({ places: [{
        displayName: { text: 'Quán thử' }, formattedAddress: 'Huế', googleMapsUri: 'https://maps.google.com/?cid=2',
        rating: 4.6, userRatingCount: 81, priceRange: range, priceLevel: 'PRICE_LEVEL_INEXPENSIVE',
        regularOpeningHours: { weekdayDescriptions: ['Thứ Hai: 08:00–17:00'] },
        websiteUri: 'https://example.org', nationalPhoneNumber: '0234 000 000',
      }] });
    },
  }).listen(0, '127.0.0.1');
  t.after(() => server.close());
  await once(server, 'listening');
  const post = (path, query) => fetch('http://127.0.0.1:' + server.address().port + path, {
    method: 'POST', body: JSON.stringify({ province: 'Huế', category: 'Tất cả', query }),
  });
  assert.equal((await post('/api/chat', '')).status, 401);
  assert.equal((await post('/api/places', 'x'.repeat(121))).status, 400);
  const result = await post('/api/places', 'bún bò');
  assert.equal(result.status, 200);
  assert.deepEqual((await result.json()).places, [{
    title: 'Quán thử', address: 'Huế', url: 'https://maps.google.com/?cid=2', authors: [], attributions: [],
    rating: 4.6, reviewCount: 81, priceRange: range, priceLevel: 'PRICE_LEVEL_INEXPENSIVE',
    hours: ['Thứ Hai: 08:00–17:00'], website: 'https://example.org', phone: '0234 000 000',
  }]);
});
