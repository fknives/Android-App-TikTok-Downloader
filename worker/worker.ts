// Cloudflare Worker entry point.
// You need to build the Kotlin/JS library first: `./gradlew :worker:jsNodeProductionLibraryDistribution`
import { resolveVideo } from "./build/dist/js/productionLibrary/TikTokDownloader-worker.mjs";

export default {
  async fetch(request: Request) {
    const url = new URL(request.url).searchParams.get("url");
    if (!url) {
      return Response.json(
        { error: "Missing ?url= query parameter" },
        { status: 400 },
      );
    }

    try {
      const bytes = await resolveVideo(url);
      return new Response(bytes, {
        headers: {
          "content-type": "video/mp4",
          "content-disposition": 'attachment; filename="video.mp4"',
        },
      });
    } catch (error) {
      return new Response(String(error), { status: 502 });
    }
  },
};
