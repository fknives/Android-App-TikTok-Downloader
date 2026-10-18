
const input = document.getElementById("url");
const form = document.querySelector(".download");
const button = document.getElementById("download");
const statusElement = document.getElementById("status");
const downloadFrame = document.getElementById("download-frame");
const blobDownload = document.getElementById("blob-download");
const manualDownload = document.getElementById("manual-download");

form.addEventListener("submit", function (event) {
    event.preventDefault();
    download(input.value);
});

let inFlight = false;
function syncButtonState() {
    button.disabled = input.value.trim() === "" || inFlight;
    if (inFlight) {
        button.dataset.state = "loading";
    } else {
        delete button.dataset.state;
    }
}
input.addEventListener("input", syncButtonState);

downloadFrame.addEventListener("load", function () {
    const body = downloadFrame.contentDocument && downloadFrame.contentDocument.body;
    if (!body) {
        return;
    }
    try {
        const payload = JSON.parse(body.textContent);
        if (payload && typeof payload.error === "string") {
            setStatus(friendlyError(0, payload.error), true);
        }
    } catch (error) {
        // Not an error document (e.g. the browser handled the response as a download).
    }
});

const SHORT_CODE = /^[A-Za-z0-9_-]{3,}$/;
const VIDEO_PATH = /^@.*\/video\/.*$/;
const TIKTOK_URL_PATH = /^(?:[a-z0-9-]+\.)*tiktok\.com\/.+/i;
const LARGE_FILE_BYTES = 12 * 1024 * 1024;
const L = {
    status: {
        downloadStarted: "Download started.",
        downloadShouldStartSoon: "Download should start soon. If it didn't, use the manual link.",
    },
    error: {
        captcha: "Ran into a captcha. Service is temporarily unavailable; please retry later.",
        unavailableVideo: "Video unavailable.",
        internalError: "Internal error. Please retry.",
        internalNetworkError: "Internal network issue. Please retry.",
        invalidLink: "Only TikTok links are supported.",
        refused: "Request was refused. Please retry later.",
        notFound: "Not found.",
        timeout: "Request timed out. Please retry.",
        serviceUnavailable: "Service temporarily unavailable. Please retry later.",
        requestFailed: (status) => "Request failed (" + status + ").",
        unexpected: (message) => "Something went wrong: " + message,
    },
};

let parsed = parsePath();
input.value = parsed.value;

if (parsed.plausible) {
    download(parsed.value);
} else {
    syncButtonState();
}

function setStatus(message, isError) {
    statusElement.textContent = message;
    if (isError) {
        statusElement.dataset.state = "error";
    } else {
        delete statusElement.dataset.state;
    }
}

function parsePath() {
    const raw = decodeURIComponent(location.pathname).replace(/^\/+|\/+$/g, "");
    if (!raw) {
        return { value: "", plausible: false };
    }

    if (VIDEO_PATH.test(raw)) {
        return { value: "https://www.tiktok.com/" + raw, plausible: true };
    }

    if (TIKTOK_URL_PATH.test(raw)) {
        return { value: "https://" + raw, plausible: true };
    }

    if (SHORT_CODE.test(raw)) {
        return { value: "https://vm.tiktok.com/" + raw + "/", plausible: true };
    }

    return { value: raw, plausible: false };
}

function normalizeTikTokUrl(value) {
    const trimmed = (value || "").trim();
    if (!trimmed) {
        return "";
    }

    const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : "https://" + trimmed;
    const withoutScheme = withScheme.replace(/^https?:\/\//i, "");
    const authority = withoutScheme.split(/[/?#]/)[0].toLowerCase();

    if (!authority || authority.indexOf("@") !== -1 || authority.indexOf(":") !== -1) {
        return "";
    }
    if (authority !== "tiktok.com" && !authority.endsWith(".tiktok.com")) {
        return "";
    }
    return withScheme;
}

async function download(value) {
    const url = normalizeTikTokUrl(value);
    if (!url) {
        setStatus(L.error.invalidLink, true);
        return;
    }
    if (inFlight) {
        return;
    }

    inFlight = true;
    syncButtonState();
    manualDownload.hidden = true;
    setStatus("", false);

    try {
        const response = await fetch("/api/resolve?url=" + encodeURIComponent(url));
        if (!response.ok) {
            const userError = new Error(await friendlyErrorFrom(response));
            userError.userFacing = true;
            throw userError;
        }

        const downloadId = response.headers.get("x-download-id");

        const size = Number(response.headers.get("x-content-length") || response.headers.get("content-length") || 0);
        if (size > LARGE_FILE_BYTES) {
            if (response.body) {
                response.body.cancel().catch(function () { });
            }
            startNativeDownload(url, downloadId);
            return;
        }

        const fallbackName = (url.split("/").filter(Boolean).pop() || "video") + ".mp4";
        const fileName =
            fileNameFromDisposition(response.headers.get("content-disposition")) ||
            fallbackName;

        let blob;
        try {
            blob = await readBlobWithinLimit(response, LARGE_FILE_BYTES);
        } catch (error) {
            // content-length was missing/lied; the real body is too big to buffer.
            if (error && error.code === "too_large") {
                startNativeDownload(url, downloadId);
                return;
            }
            throw error;
        }

        triggerFileDownload({ blob: blob, fileName: fileName });

        setStatus(L.status.downloadStarted);
    } catch (error) {
        const message = error && error.message ? error.message : String(error);
        setStatus(error && error.userFacing ? message : L.error.unexpected(message), true);
    } finally {
        inFlight = false;
        syncButtonState();
    }
}

function startNativeDownload(url, id) {
    let downloadUrl = "/api/download?url=" + encodeURIComponent(url);
    if (id) {
        downloadUrl += "&id=" + encodeURIComponent(id);
    }

    triggerDownload(downloadUrl);

    manualDownload.href = downloadUrl;
    manualDownload.hidden = false;

    setStatus(L.status.downloadShouldStartSoon);
}

async function readBlobWithinLimit(response, limit) {
    if (!response.body) {
        return response.blob();
    }

    const reader = response.body.getReader();
    const chunks = [];
    let total = 0;

    while (true) {
        const result = await reader.read();
        if (result.done) {
            break;
        }

        total += result.value.byteLength;
        if (total > limit) {
            reader.cancel().catch(function () { });
            const tooLarge = new Error("Video is too large to buffer.");
            tooLarge.code = "too_large";
            throw tooLarge;
        }

        chunks.push(result.value);
    }

    return new Blob(chunks, {
        type: response.headers.get("content-type") || "video/mp4",
    });
}

function triggerDownload(url) {
    downloadFrame.src = url;
}

async function friendlyErrorFrom(response) {
    let detail = "";
    try {
        const payload = await response.json();
        if (payload && typeof payload.error === "string") {
            detail = payload.error;
        }
    } catch (error) {
        // not a JSON error body
    }
    return friendlyError(response.status, detail);
}

function friendlyError(status, detail) {
    const text = (detail || "").toLowerCase();

    if (text.indexOf("captcha") !== -1) {
        return L.error.captcha;
    }
    if (text.indexOf("videoprivate") !== -1 || text.indexOf("videodeleted") !== -1) {
        return L.error.unavailableVideo;
    }
    if (text.indexOf("parsing") !== -1) {
        return L.error.internalError;
    }
    if (text.indexOf("network") !== -1) {
        return L.error.internalNetworkError;
    }

    if (status === 400) {
        return L.error.invalidLink;
    }
    if (status === 401 || status === 403) {
        return L.error.refused;
    }
    if (status === 404) {
        return L.error.notFound;
    }
    if (status === 408) {
        return L.error.timeout;
    }
    if (status === 429 || status >= 500) {
        return L.error.serviceUnavailable;
    }

    return detail || L.error.requestFailed(status);
}

function fileNameFromDisposition(header) {
    if (!header) {
        return "";
    }
    const match = /filename="?([^";]+)"?/i.exec(header);
    return match ? match[1].trim() : "";
}

function triggerFileDownload({ blob, fileName }) {
    const objectUrl = URL.createObjectURL(blob);
    blobDownload.href = objectUrl;
    blobDownload.download = fileName;
    blobDownload.click();
    URL.revokeObjectURL(objectUrl);
}
