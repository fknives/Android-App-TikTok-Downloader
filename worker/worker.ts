// Cloudflare Worker entry point. Thin router over the Kotlin resolver entry points.
//
// Build the Kotlin/JS library first: ./gradlew :worker:jsNodeProductionLibraryDistribution
import { downloadVideo, resolveVideo } from "./build/dist/js/productionLibrary/TikTokDownloader-worker.mjs";

export default {
    async fetch(request: Request) {
        const requestUrl = new URL(request.url);

        if (requestUrl.pathname === "/api/resolve") {
            return resolve(requestUrl);
        }
        if (requestUrl.pathname === "/api/download") {
            return download(requestUrl);
        }

        return errorResponse(404, "Not found");
    },
};

async function resolve(requestUrl: URL): Promise<Response> {
    const url = requestUrl.searchParams.get("url");
    if (!url) {
        return errorResponse(400, "Missing ?url= query parameter");
    }

    try {
        return await resolveVideo(url);
    } catch (error) {
        return errorResponse(502, String(error));
    }
}

async function download(requestUrl: URL): Promise<Response> {
    const fileUrl = requestUrl.searchParams.get("url");
    if (!fileUrl) {
        return errorResponse(400, "Missing ?url= query parameter");
    }

    try {
        return await downloadVideo(fileUrl, requestUrl.searchParams.get("cookie") || null);
    } catch (error) {
        return errorResponse(502, String(error));
    }
}

function errorResponse(status: number, message: string): Response {
    return new Response(JSON.stringify({ error: message }), {
        status,
        headers: {
            "content-type": "application/json; charset=utf-8",
            "cache-control": "no-store",
        },
    });
}
