import http from 'http';
import { AddressInfo } from 'net';
import { WorklinkMcpHttpServer } from '@src/mcp/transport/httpServer';
import { StaticOptInRegistry } from '@src/mcp/security/optInRegistry';
import { evictIdleMcpTransports } from '@src/mcp/security/mcpSessionSeverance';
import type { McpServerConfig } from '@src/mcp/config';
import type { McpPrincipal } from '@src/mcp/security/principal';

function config(): McpServerConfig
{
    return {
        enabled: true,
        host: '127.0.0.1',
        port: 0,
        apiKey: 'dev',
        apiKeyCmsUserId: '42',
        keyValidation: 'env',
        devAllAccess: false,
        requirePickerSelection: true,
        publicBaseUrl: null,
        trustProxy: false,
        path: '/mcp',
        transportIdleMs: 10000,
        transportEvictMs: 60000,
        transportSweepMs: 30000,
    };
}

const principal: McpPrincipal = { keyId: 'k', cmsUserId: '42', mcpEnabled: true };

function get(
    port: number,
    path: string,
    authorization?: string,
    sessionId?: string,
): Promise<{ status: number; body: string; headers: http.IncomingHttpHeaders }>
{
    return new Promise((resolve, reject) =>
    {
        const headers: Record<string, string> = {};
        if (authorization)
        {
            headers.authorization = authorization;
        }
        if (sessionId)
        {
            headers['mcp-session-id'] = sessionId;
        }
        const req = http.request(
            { host: '127.0.0.1', port, path, method: 'GET', headers },
            (res) =>
            {
                let body = '';
                res.on('data', (chunk) => { body += chunk; });
                res.on('end', () => resolve({
                    status: res.statusCode ?? 0,
                    body,
                    headers: res.headers,
                }));
            },
        );
        req.on('error', reject);
        req.end();
    });
}

/** Issue an MCP session-termination DELETE for `sessionId`. */
function del(
    port: number,
    sessionId: string | undefined,
    authorization?: string,
): Promise<{ status: number; body: string }>
{
    return new Promise((resolve, reject) =>
    {
        const headers: Record<string, string> = {
            ...(authorization ? { authorization } : {}),
            'mcp-protocol-version': '2024-11-05',
        };
        if (sessionId)
        {
            headers['mcp-session-id'] = sessionId;
        }
        const req = http.request(
            { host: '127.0.0.1', port, path: '/mcp', method: 'DELETE', headers },
            (res) =>
            {
                let body = '';
                res.on('data', (chunk) => { body += chunk; });
                res.on('end', () => resolve({ status: res.statusCode ?? 0, body }));
            },
        );
        req.on('error', reject);
        req.end();
    });
}

type McpPostResult = {
    status: number;
    body: string;
    sessionId: string | undefined;
    headers: http.IncomingHttpHeaders;
};

/** POST JSON to /mcp; resolves when the response ends (or rejects on socket error). */
function postMcp(
    port: number,
    authorization: string,
    body: unknown,
    sessionId?: string,
): Promise<McpPostResult>
{
    const payload = JSON.stringify(body);
    return new Promise((resolve, reject) =>
    {
        const headers: Record<string, string> = {
            authorization,
            'content-type': 'application/json',
            accept: 'application/json, text/event-stream',
            'content-length': String(Buffer.byteLength(payload)),
            'mcp-protocol-version': '2024-11-05',
        };
        if (sessionId)
        {
            headers['mcp-session-id'] = sessionId;
        }
        const req = http.request(
            { host: '127.0.0.1', port, path: '/mcp', method: 'POST', headers },
            (res) =>
            {
                const sid = res.headers['mcp-session-id'];
                let text = '';
                res.on('data', (chunk) => { text += chunk; });
                res.on('end', () => resolve({
                    status: res.statusCode ?? 0,
                    body: text,
                    sessionId: typeof sid === 'string' ? sid : sessionId,
                    headers: res.headers,
                }));
            },
        );
        req.on('error', reject);
        req.end(payload);
    });
}

/**
 * Start a POST and return both the ClientRequest (so the test can abort) and a promise for the
 * eventual response. Used to prove in-flight request streams survive idle eviction.
 */
function postMcpPending(
    port: number,
    authorization: string,
    body: unknown,
    sessionId: string,
): { req: http.ClientRequest; done: Promise<McpPostResult> }
{
    const payload = JSON.stringify(body);
    let req!: http.ClientRequest;
    const done = new Promise<McpPostResult>((resolve, reject) =>
    {
        const headers: Record<string, string> = {
            authorization,
            'content-type': 'application/json',
            accept: 'application/json, text/event-stream',
            'content-length': String(Buffer.byteLength(payload)),
            'mcp-protocol-version': '2024-11-05',
            'mcp-session-id': sessionId,
        };
        req = http.request(
            { host: '127.0.0.1', port, path: '/mcp', method: 'POST', headers },
            (res) =>
            {
                let text = '';
                res.on('data', (chunk) => { text += chunk; });
                res.on('end', () => resolve({
                    status: res.statusCode ?? 0,
                    body: text,
                    sessionId,
                    headers: res.headers,
                }));
            },
        );
        req.on('error', reject);
        req.end(payload);
    });
    return { req, done };
}

/** Perform a real MCP `initialize` POST and return the assigned mcp-session-id (from the header). */
function initialize(port: number, authorization: string): Promise<{ status: number; sessionId: string | undefined }>
{
    return postMcp(port, authorization, {
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
            protocolVersion: '2024-11-05',
            capabilities: {},
            clientInfo: { name: 'zombie-test', version: '1.0.0' },
        },
    }).then(({ status, sessionId }) => ({ status, sessionId }));
}

/** MCP handshake after initialize so tools/call is accepted. */
async function notifyInitialized(port: number, authorization: string, sessionId: string): Promise<void>
{
    const res = await postMcp(port, authorization, {
        jsonrpc: '2.0',
        method: 'notifications/initialized',
    }, sessionId);
    // Spec allows 202/200/empty; treat non-4xx/5xx as ok.
    expect(res.status).toBeLessThan(400);
}

describe('mcp/httpServer trust proxy wiring', () =>
{
    it('applies config.trustProxy onto the Express app (not a hardcoded false)', async () =>
    {
        const server = new WorklinkMcpHttpServer(
            { ...config(), trustProxy: 1 },
            {
                keyValidator: { validate: async () => null },
                optInRegistry: new StaticOptInRegistry([]),
            },
        );
        try
        {
            const app = (server as unknown as { app: { get: (k: string) => unknown } }).app;
            expect(app.get('trust proxy')).toBe(1);
        }
        finally
        {
            await server.close();
        }
    });

    it('keeps trust proxy off when config.trustProxy is false', async () =>
    {
        const server = new WorklinkMcpHttpServer(config(), {
            keyValidator: { validate: async () => null },
            optInRegistry: new StaticOptInRegistry([]),
        });
        try
        {
            const app = (server as unknown as { app: { get: (k: string) => unknown } }).app;
            expect(app.get('trust proxy')).toBe(false);
        }
        finally
        {
            await server.close();
        }
    });
});

describe('mcp/httpServer auth integration (/health)', () =>
{
    let server: WorklinkMcpHttpServer;
    let port = 0;

    beforeAll(async () =>
    {
        server = new WorklinkMcpHttpServer(config(), {
            keyValidator: { validate: async (key) => (key === 'good' ? principal : null) },
            optInRegistry: new StaticOptInRegistry(['42']),
        });
        await server.listen();
        // Reach into the underlying http server for the bound port.
        const httpServer = (server as unknown as { httpServer: http.Server }).httpServer;
        port = (httpServer.address() as AddressInfo).port;
    });

    afterAll(async () =>
    {
        await server.close();
    });

    it('401 without a bearer token', async () =>
    {
        expect((await get(port, '/health')).status).toBe(401);
    });

    it('401 with an unknown key', async () =>
    {
        expect((await get(port, '/health', 'Bearer nope')).status).toBe(401);
    });

    it('200 with a valid opted-in key', async () =>
    {
        expect((await get(port, '/health', 'Bearer good')).status).toBe(200);
    });
});

describe('mcp/httpServer auth integration (opted-out -> 403)', () =>
{
    let server: WorklinkMcpHttpServer;
    let port = 0;

    beforeAll(async () =>
    {
        server = new WorklinkMcpHttpServer(config(), {
            keyValidator: { validate: async (key) => (key === 'good' ? principal : null) },
            optInRegistry: new StaticOptInRegistry([]), // nobody opted in
        });
        await server.listen();
        const httpServer = (server as unknown as { httpServer: http.Server }).httpServer;
        port = (httpServer.address() as AddressInfo).port;
    });

    afterAll(async () =>
    {
        await server.close();
    });

    it('403 when the resolved user is not opted in', async () =>
    {
        expect((await get(port, '/health', 'Bearer good')).status).toBe(403);
    });
});

describe('mcp/httpServer idle eviction (zombie detach on sever)', () =>
{
    let server: WorklinkMcpHttpServer;
    let port = 0;

    beforeAll(async () =>
    {
        server = new WorklinkMcpHttpServer(config(), {
            keyValidator: { validate: async (key) => (key === 'good' ? principal : null) },
            optInRegistry: new StaticOptInRegistry(['42']),
        });
        await server.listen();
        const httpServer = (server as unknown as { httpServer: http.Server }).httpServer;
        port = (httpServer.address() as AddressInfo).port;
    });

    afterAll(async () =>
    {
        await server.close();
    });

    it('user-scopes the /health session count (no global tenant leak)', async () =>
    {
        const res = await get(port, '/health', 'Bearer good');
        expect(res.status).toBe(200);
        const body = JSON.parse(res.body);
        expect(body).toMatchObject({ ok: true, service: 'worklink-mcp' });
        expect(body).toHaveProperty('mcpSessions');
        expect(body).not.toHaveProperty('activeMcpSessions');
    });

    it('keeps a freshly-active transport but evicts it once idle (severed Cursor → no zombie)', async () =>
    {
        const init = await initialize(port, 'Bearer good');
        expect(init.status).toBe(200);
        expect(init.sessionId).toBeTruthy();

        // The transport is registered and counted for its own user.
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        // A recent POST (the initialize) keeps it alive — the sweep must NOT evict a live connection.
        expect(await evictIdleMcpTransports(60000)).toBe(0);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        // Once idle past the threshold (no open SSE stream, no recent POST), it's evicted and the
        // binding torn down — exactly what stops a severed Cursor from leaving a zombie behind.
        await new Promise((r) => setTimeout(r, 20));
        expect(await evictIdleMcpTransports(1)).toBe(1);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(0);
    });

    it('keeps an in-flight POST/response stream alive past the idle threshold (no standalone GET)', async () =>
    {
        // MCP clients may omit the standalone GET SSE stream. A long tools/call (picker wait,
        // script) must not be idle-evicted just because lastPostAt is older than idleMs.
        const init = await initialize(port, 'Bearer good');
        expect(init.status).toBe(200);
        expect(init.sessionId).toBeTruthy();
        const sessionId = init.sessionId as string;
        await notifyInitialized(port, 'Bearer good', sessionId);

        const pending = postMcpPending(port, 'Bearer good', {
            jsonrpc: '2.0',
            id: 2,
            method: 'tools/call',
            params: {
                name: 'worklink_wait_for_selection',
                arguments: { timeoutMs: 2500 },
            },
        }, sessionId);

        // Let the tool enter its wait loop so the POST response stream is open, then age past
        // any lastPostAt-only window and sweep with a 1ms idle threshold.
        await new Promise((r) => setTimeout(r, 80));
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);
        expect(await evictIdleMcpTransports(1)).toBe(0);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        const result = await pending.done;
        expect(result.status).toBe(200);
        expect(result.body.length).toBeGreaterThan(0);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        // After the in-flight call finishes, genuine idle eviction still applies.
        await new Promise((r) => setTimeout(r, 20));
        expect(await evictIdleMcpTransports(1)).toBe(1);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(0);
    }, 15000);

    it('releases in-flight POST activity on client abort so genuine idle eviction can run', async () =>
    {
        const init = await initialize(port, 'Bearer good');
        expect(init.sessionId).toBeTruthy();
        const sessionId = init.sessionId as string;
        await notifyInitialized(port, 'Bearer good', sessionId);

        const pending = postMcpPending(port, 'Bearer good', {
            jsonrpc: '2.0',
            id: 3,
            method: 'tools/call',
            params: {
                name: 'worklink_wait_for_selection',
                arguments: { timeoutMs: 10_000 },
            },
        }, sessionId);

        await new Promise((r) => setTimeout(r, 80));
        expect(await evictIdleMcpTransports(1)).toBe(0);

        // Abort the client socket. Do not require the response promise to settle — a hung
        // tools/call may keep the SDK await alive; activity accounting must still drop.
        pending.req.destroy();
        await new Promise((r) => setTimeout(r, 80));

        expect(await evictIdleMcpTransports(1)).toBe(1);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(0);

        // Drain the aborted client promise so it cannot leak into sibling tests.
        await Promise.race([
            pending.done.catch(() => undefined),
            new Promise((r) => setTimeout(r, 200)),
        ]);
    }, 15000);

    it('DELETE terminates the session immediately (disable MCP in Cursor → no zombie)', async () =>
    {
        const init = await initialize(port, 'Bearer good');
        expect(init.status).toBe(200);
        expect(init.sessionId).toBeTruthy();
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        // The MCP spec's session-termination DELETE (what Cursor sends on disable) tears the
        // transport down right away — no waiting on the idle sweep.
        const removed = await del(port, init.sessionId, 'Bearer good');
        expect(removed.status).toBe(200);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(0);
    });

    it('DELETE requires the owning principal (no cross-tenant teardown)', async () =>
    {
        const init = await initialize(port, 'Bearer good');
        expect(init.sessionId).toBeTruthy();

        // No bearer → rejected; the session must survive.
        expect((await del(port, init.sessionId)).status).toBe(401);
        expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

        // Clean up so the count assertion in sibling tests isn't affected.
        await del(port, init.sessionId, 'Bearer good');
    });
});

describe('mcp/httpServer live session severance on invalid key (rotate/revoke mid-session)', () =>
{
    it('hard-closes an established transport when the presented key stops validating', async () =>
    {
        // Simulate CMS rotate/revoke: the same Bearer that opened the session later fails validation.
        // Docs claim live sessions are severed — pin the HTTP wiring, not only the auth middleware unit.
        let allowKey = true;
        const server = new WorklinkMcpHttpServer(config(), {
            keyValidator: {
                validate: async (key) => (allowKey && key === 'good' ? principal : null),
            },
            optInRegistry: new StaticOptInRegistry(['42']),
        });
        try
        {
            await server.listen();
            const httpServer = (server as unknown as { httpServer: http.Server }).httpServer;
            const port = (httpServer.address() as AddressInfo).port;

            const init = await initialize(port, 'Bearer good');
            expect(init.status).toBe(200);
            expect(init.sessionId).toBeTruthy();
            expect(JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions).toBe(1);

            allowKey = false;
            const revoked = await get(port, '/health', 'Bearer good', init.sessionId);
            expect(revoked.status).toBe(401);
            expect(revoked.headers['mcp-auth-error']).toBe('api_key_revoked');
            expect(JSON.parse(revoked.body).code).toBe('api_key_revoked');

            // Default onSessionKeyInvalid severs asynchronously via severMcpSessionById.
            allowKey = true;
            let count = 1;
            for (let i = 0; i < 40; i += 1)
            {
                count = JSON.parse((await get(port, '/health', 'Bearer good')).body).mcpSessions;
                if (count === 0)
                {
                    break;
                }
                await new Promise((r) => setTimeout(r, 25));
            }
            expect(count).toBe(0);
        }
        finally
        {
            await server.close();
        }
    });
});
