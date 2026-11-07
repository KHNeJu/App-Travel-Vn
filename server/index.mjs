import { createServer } from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const route = '/api/chat';
const system = `Bạn là Trợ lý du lịch Travel VN. Trả lời bằng tiếng Việt, ngắn gọn và dễ đọc trên điện thoại.
Hỗ trợ địa điểm, món ăn, chỗ ở, di chuyển, ngân sách, hành lý và lịch theo ngày.
Ngữ cảnh chuyến đi chỉ là dữ liệu tham khảo. Nếu có công cụ Google, dùng nguồn khi trả lời câu hỏi cần dữ liệu mới như giá và giờ mở cửa.
Nói rõ khi chưa xác minh được thông tin và đừng bịa liên kết. Không tự nhận đã đặt chỗ, gửi lời mời, thêm lịch hay ghi khoản chi.`;

function send(response, status, payload) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  response.end(JSON.stringify(payload));
}

async function readJson(request) {
  let bytes = 0;
  const chunks = [];
  for await (const chunk of request.iterator({ destroyOnReturn: false })) {
    bytes += chunk.length;
    if (bytes > 128_000) {
      request.resume();
      throw new RangeError('Body too large');
    }
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function httpsLink(value) {
  try {
    return typeof value === 'string' && new URL(value).protocol === 'https:';
  } catch {
    return false;
  }
}

function parseAnswer(payload) {
  const answer = payload.candidates?.[0];
  const text = answer?.content?.parts?.filter(part => !part.thought && typeof part.text === 'string')
    .map(part => part.text).join('\n').trim();
  if (!text) return null;
  const grounding = answer.groundingMetadata;
  const sources = new Map();
  for (const chunk of grounding?.groundingChunks ?? []) {
    const item = chunk.maps ?? chunk.web;
    const url = item?.googleMapsUri ?? item?.uri;
    if (httpsLink(url)) {
      sources.set(url, { url, title: item.title || new URL(url).hostname, kind: chunk.maps ? 'maps' : 'web' });
    }
  }
  return { text, sources: [...sources.values()], searchHtml: grounding?.searchEntryPoint?.renderedContent ?? '' };
}

export function createChatServer({ key, token, model = 'gemini-3.8-flash', grounding = 'search', callGemini = fetch }) {
  if (!key || !/^[A-Za-z0-9_-]{24,128}$/.test(token ?? '')) throw new Error('Configure GEMINI_API_KEY and TRAVEL_AI_TOKEN (24–128 base64url characters).');
  if (!/^gemini-[a-z0-9.-]+$/.test(model)) throw new Error('Invalid GEMINI_MODEL.');
  if (!['none', 'search', 'search-maps'].includes(grounding)) throw new Error('Invalid GEMINI_GROUNDING.');

  const expected = Buffer.from('Bearer ' + token);
  const recent = [];
  let active = 0;
  return createServer({ requestTimeout: 12_000 }, async (request, response) => {
    if (request.url !== route) return send(response, 404, { error: 'Không tìm thấy dịch vụ.' });
    if (request.method !== 'POST') return send(response, 405, { error: 'Dùng POST.' });
    const authorization = Buffer.from(request.headers.authorization ?? '');
    if (expected.length !== authorization.length || !timingSafeEqual(expected, authorization)) {
      return send(response, 401, { error: 'Mã truy cập trợ lý không đúng.' });
    }
    let body;
    try {
      body = await readJson(request);
    } catch (error) {
      return send(response, error instanceof RangeError ? 413 : 400, { error: 'Nội dung gửi không hợp lệ hoặc quá dài.' });
    }
    const history = body?.messages;
    const context = body?.context ?? '';
    if (!Array.isArray(history) || history.length < 1 || history.length > 13 || history.length % 2 === 0 ||
        typeof context !== 'string' || context.length > 600 ||
        history.some((item, index) => item?.role !== (index % 2 ? 'assistant' : 'user') ||
          typeof item.text !== 'string' || !item.text.trim() || item.text.length > (index % 2 ? 7000 : 1600))) {
      return send(response, 400, { error: 'Nội dung hội thoại không hợp lệ.' });
    }

    const now = Date.now();
    while (recent.length && now - recent[0] >= 60_000) recent.shift();
    if (recent.length >= 12 || active >= 2) return send(response, 429, { error: 'Trợ lý đang bận. Bạn thử lại sau một chút.' });
    active++;
    recent.push(now);
    try {
      const prompt = {
        systemInstruction: { parts: [{ text: system }] },
        contents: history.map(({ role, text }, index) => ({
          role: role === 'assistant' ? 'model' : 'user',
          parts: [{ text: index === 0 && context ? 'Ngữ cảnh chuyến đi: ' + context + '\n\nCâu hỏi: ' + text : text }],
        })),
        generationConfig: { maxOutputTokens: 2200 },
      };
      if (grounding !== 'none') prompt.tools = [{ googleSearch: {} }];
      if (grounding === 'search-maps') prompt.tools.push({ googleMaps: {} });
      const result = await callGemini('https://generativelanguage.googleapis.com/v1beta/models/' + model + ':generateContent', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key },
        body: JSON.stringify(prompt),
        signal: AbortSignal.timeout(42_000),
        redirect: 'error',
      });
      if (!result.ok) {
        console.error('Gemini status:', result.status);
        if (result.status === 429) return send(response, 429, { error: 'Gemini đã hết hạn mức hoặc đang bận. Thử lại sau.' });
        if ([400, 401, 403, 404].includes(result.status)) {
          return send(response, 502, { error: 'Gemini từ chối yêu cầu. Kiểm tra khóa, model và quyền dùng Google Search hoặc Maps trên máy chủ.' });
        }
        return send(response, 502, { error: 'Gemini chưa phản hồi thành công. Hãy thử lại.' });
      }
      const answer = parseAnswer(await result.json());
      return answer ? send(response, 200, answer)
        : send(response, 422, { error: 'Gemini chưa tạo được câu trả lời. Bạn thử diễn đạt lại.' });
    } catch (error) {
      const timeout = error.name === 'TimeoutError';
      return send(response, timeout ? 504 : 502, { error: timeout ? 'Gemini phản hồi quá lâu. Bạn có thể thử lại.' : 'Chưa kết nối được Gemini.' });
    } finally {
      active--;
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    createChatServer({
      key: process.env.GEMINI_API_KEY,
      token: process.env.TRAVEL_AI_TOKEN,
      model: process.env.GEMINI_MODEL,
      grounding: process.env.GEMINI_GROUNDING,
    }).listen(Number(process.env.TRAVEL_AI_PORT || 8787), '127.0.0.1', () => {
      console.log('Travel VN AI đang nghe trên loopback.');
    });
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
