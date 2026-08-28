import { describe, expect, it, vi } from 'vitest';
import type { FetchLike } from '@workq/http';
import { TeamsConnectorClient } from './connector.js';

function jsonResponse(status: number, body: unknown = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

function textResponse(status: number, body: string): Response {
  return new Response(body, { status, headers: { 'content-type': 'text/plain' } });
}

const CREDS = {
  appId: 'app-id',
  appPassword: 'app-secret',
  tenantId: 'tenant-1',
} as const;

describe('TeamsConnectorClient', () => {
  it('posts an activity and includes replyToId when given', async () => {
    const fetchImpl = vi.fn(async (url: string | URL, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/oauth2/v2.0/token')) {
        return jsonResponse(200, { access_token: 'bot-tok', expires_in: 3600 });
      }
      expect(u).toBe(
        'https://smba.example/amer/v3/conversations/conv%2F1/activities',
      );
      expect(init?.method).toBe('POST');
      expect(new Headers(init?.headers).get('authorization')).toBe('Bearer bot-tok');
      const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
      expect(body).toEqual({
        type: 'message',
        text: 'ack',
        textFormat: 'markdown',
        replyToId: 'parent-9',
      });
      return jsonResponse(200, { id: 'act-1' });
    }) as FetchLike;

    const client = new TeamsConnectorClient({ ...CREDS, fetch: fetchImpl });
    await expect(
      client.postActivity({
        serviceUrl: 'https://smba.example/amer/',
        conversationId: 'conv/1',
        text: 'ack',
        replyToId: 'parent-9',
      }),
    ).resolves.toEqual({ id: 'act-1' });
    expect(fetchImpl).toHaveBeenCalledTimes(2);
  });

  it('throws on connector HTTP errors and missing activity id', async () => {
    const fetchImpl = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { access_token: 't', expires_in: 3600 }))
      .mockResolvedValueOnce(textResponse(503, 'upstream down'))
      .mockResolvedValueOnce(jsonResponse(200, {})) as FetchLike;

    const client = new TeamsConnectorClient({ ...CREDS, fetch: fetchImpl });
    await expect(
      client.postActivity({
        serviceUrl: 'https://smba.example',
        conversationId: 'c',
        text: 'x',
      }),
    ).rejects.toThrow(/connector post HTTP 503: upstream down/);

    // Cached token is reused for the second post.
    await expect(
      client.postActivity({
        serviceUrl: 'https://smba.example',
        conversationId: 'c',
        text: 'x',
      }),
    ).rejects.toThrow(/connector post returned no activity id/);
    expect(fetchImpl.mock.calls.filter(([u]) => String(u).includes('/oauth2/')).length).toBe(1);
  });

  it('reuses a bot token until near expiry, then refreshes', async () => {
    let now = 1_000_000;
    const fetchImpl = vi.fn(async (url: string | URL) => {
      const u = String(url);
      if (u.includes('/oauth2/v2.0/token')) {
        return jsonResponse(200, {
          access_token: `tok-${fetchImpl.mock.calls.length}`,
          expires_in: 3600,
        });
      }
      return jsonResponse(200, { id: `act-${fetchImpl.mock.calls.length}` });
    }) as FetchLike;

    const client = new TeamsConnectorClient({
      ...CREDS,
      fetch: fetchImpl,
      now: () => now,
    });

    await client.postActivity({
      serviceUrl: 'https://smba.example',
      conversationId: 'c',
      text: 'a',
    });
    // expiresAt = 1_000_000 + 3600*1000 = 4_600_000 — still reusable.
    now = 4_000_000;
    await client.postActivity({
      serviceUrl: 'https://smba.example',
      conversationId: 'c',
      text: 'b',
    });
    expect(fetchImpl.mock.calls.filter(([u]) => String(u).includes('/oauth2/')).length).toBe(1);

    // Within 60s of expiry → refresh.
    now = 4_570_000;
    await client.postActivity({
      serviceUrl: 'https://smba.example',
      conversationId: 'c',
      text: 'c',
    });
    expect(fetchImpl.mock.calls.filter(([u]) => String(u).includes('/oauth2/')).length).toBe(2);
  });

  it('uses botframework.com tenant when tenantId is omitted', async () => {
    const fetchImpl = vi.fn(async (url: string | URL) => {
      const u = String(url);
      if (u.includes('/oauth2/v2.0/token')) {
        expect(u).toBe(
          'https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token',
        );
        return jsonResponse(200, { access_token: 'bf', expires_in: 3600 });
      }
      return jsonResponse(200, { id: 'a1' });
    }) as FetchLike;

    const client = new TeamsConnectorClient({
      appId: 'a',
      appPassword: 'p',
      fetch: fetchImpl,
    });
    await expect(
      client.postActivity({
        serviceUrl: 'https://smba.example',
        conversationId: 'c',
        text: 'hi',
      }),
    ).resolves.toEqual({ id: 'a1' });
  });

  it('throws when the token grant fails or omits access_token', async () => {
    const failHttp = vi.fn(async () => textResponse(401, 'nope')) as FetchLike;
    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: failHttp }).postActivity({
        serviceUrl: 'https://smba.example',
        conversationId: 'c',
        text: 'x',
      }),
    ).rejects.toThrow(/token HTTP 401/);

    const missing = vi.fn(async () => jsonResponse(200, { expires_in: 60 })) as FetchLike;
    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: missing }).postActivity({
        serviceUrl: 'https://smba.example',
        conversationId: 'c',
        text: 'x',
      }),
    ).rejects.toThrow(/token response missing access_token/);
  });

  it('loads an Entra profile and returns {} on Graph failure', async () => {
    const okFetch = vi.fn(async (url: string | URL) => {
      const u = String(url);
      if (u.includes('/oauth2/v2.0/token')) {
        expect(u).toContain('/tenant-1/oauth2/');
        return jsonResponse(200, { access_token: 'g', expires_in: 3600 });
      }
      expect(u).toContain('graph.microsoft.com/v1.0/users/user%2F1');
      return jsonResponse(200, {
        displayName: 'Ada',
        mail: 'ada@example.com',
      });
    }) as FetchLike;

    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: okFetch }).profile('user/1'),
    ).resolves.toEqual({ displayName: 'Ada', email: 'ada@example.com' });

    const upnFetch = vi.fn(async (url: string | URL) => {
      if (String(url).includes('/oauth2/')) {
        return jsonResponse(200, { access_token: 'g', expires_in: 3600 });
      }
      return jsonResponse(200, { userPrincipalName: 'bob@example.com' });
    }) as FetchLike;
    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: upnFetch }).profile('bob'),
    ).resolves.toEqual({ email: 'bob@example.com' });

    const badFetch = vi.fn(async (url: string | URL) => {
      if (String(url).includes('/oauth2/')) {
        return jsonResponse(200, { access_token: 'g', expires_in: 3600 });
      }
      return textResponse(404, 'missing');
    }) as FetchLike;
    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: badFetch }).profile('x'),
    ).resolves.toEqual({});

    await expect(
      new TeamsConnectorClient({
        appId: 'a',
        appPassword: 'p',
        fetch: vi.fn() as FetchLike,
      }).profile('x'),
    ).resolves.toEqual({});
  });

  it('lists chat messages after a watermark and strips HTML', async () => {
    const fetchImpl = vi.fn(async (url: string | URL) => {
      if (String(url).includes('/oauth2/')) {
        return jsonResponse(200, { access_token: 'g', expires_in: 3600 });
      }
      return jsonResponse(200, {
        value: [
          {
            id: 'old',
            createdDateTime: '2026-01-01T00:00:00.000Z',
            body: { content: 'too old' },
          },
          {
            id: 'm1',
            replyToId: 'root',
            createdDateTime: '2026-08-01T12:00:00.000Z',
            body: { content: '<p>hello&nbsp;&amp;&lt;world&gt;</p>' },
            from: { user: { id: 'u1' } },
          },
          {
            id: 'empty-body',
            createdDateTime: '2026-08-01T13:00:00.000Z',
            body: { content: '' },
          },
          { createdDateTime: '2026-08-01T14:00:00.000Z' },
        ],
      });
    }) as FetchLike;

    const afterMs = Date.parse('2026-01-01T00:00:00.000Z');
    const rows = await new TeamsConnectorClient({ ...CREDS, fetch: fetchImpl }).listChatMessages({
      chatId: 'chat/1',
      afterMs,
    });
    expect(rows).toEqual([
      {
        id: 'm1',
        text: 'hello &<world>',
        fromUserId: 'u1',
        createdMs: Date.parse('2026-08-01T12:00:00.000Z'),
        replyToId: 'root',
      },
      {
        id: 'empty-body',
        createdMs: Date.parse('2026-08-01T13:00:00.000Z'),
      },
    ]);

    const failFetch = vi.fn(async (url: string | URL) => {
      if (String(url).includes('/oauth2/')) {
        return jsonResponse(200, { access_token: 'g', expires_in: 3600 });
      }
      return textResponse(500, 'boom');
    }) as FetchLike;
    await expect(
      new TeamsConnectorClient({ ...CREDS, fetch: failFetch }).listChatMessages({
        chatId: 'c',
        afterMs: 0,
      }),
    ).resolves.toEqual([]);
  });
});
