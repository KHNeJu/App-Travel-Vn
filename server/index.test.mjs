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
    context: 'Ninh Bình, 2 ngày',
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
});
