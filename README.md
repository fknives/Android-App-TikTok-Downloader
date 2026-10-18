# TikTok-Downloader [![Verify Download Method still works](https://github.com/fknives/TikTok-Downloader/actions/workflows/up-to-date-jobs.yml/badge.svg?branch=develop)](https://github.com/fknives/TikTok-Downloader/actions/workflows/up-to-date-jobs.yml)
Simple Open Source App to Download TikTok Videos using share feature of the App. 

The idea is that some of the videos cannot be downloaded within the app, or one simply doesn't want tiktok grant access to external storage, but they can be downloaded from the Website. This small project automates that process, by levreging the share / other feature of the TikTok app.

There is already plenty of applications in the Play Store doing the same so this is not released. Probably won't be updated as often as they are. However feel free to look over the inner workings of the code if your heart desires.

## Medium

There is also a medium post about the project, feel free to check it out [here](https://fnives.medium.com/unintended-behaviour-using-a-website-as-your-api-fd4241100e3c).

## Download

If you wish to try it out, here is the QR code to download the application.

![QR Code for APK](tiktok_downloader_apk_qr_code.png)

## Demo

Click on the image for the demo video

[![Usage within the App](https://img.youtube.com/vi/NXv3JpmwA8Y/0.jpg)](https://www.youtube.com/watch?v=NXv3JpmwA8Y)
[![Usage within TikTok](https://img.youtube.com/vi/jxaxffE8c4c/0.jpg)](https://www.youtube.com/watch?v=jxaxffE8c4c)

## Website

A hosted version is available at [tt.fomevod.bid](https://tt.fomevod.bid) — paste a TikTok share link and download the video from the browser, no app install needed. The website source lives in `worker/`.

## CLI

A small Linux x64 CLI lives in `cli/` and writes the video straight to stdout:

```sh
./cli/build/bin/linuxX64/releaseExecutable/cli.kexe "https://vm.tiktok.com/ZN8SyMfnP/" > output.mp4
```

See [`cli/README.md`](cli/README.md) for building and running it via Docker.

## Secret

Here is another Secret Video QR code just for the curious minded

![QR Code for Secret](secret_video_qr_code.png)


## Note to self

When checking error messages, `jq` can be a life saver.

Example getting the stack trace for a specific error message:

`jq -r '.[].errors[] | select(.message == "Parsing Error") | .stacktrace' errors.json`

Or getting all htmls if an error happened in the json:
`jq '.[] | select(.errors[]?.message == "Parsing Error") | .errors[].html' errors.json`

> Use -r when you want to avoid the JSON string quotes around output. 