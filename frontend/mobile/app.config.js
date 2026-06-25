// Dynamic Expo config layered on top of app.json.
//
// app.json stays the source of truth; this only injects the Android Firebase
// config (google-services.json) when it's available via the EAS *file* env var
// GOOGLE_SERVICES_JSON. EAS materializes that secret to a local path at build
// time and exposes the path in this env var. When the var is absent (local dev,
// or a build without the secret) the app builds normally with push disabled —
// so a missing Firebase file never breaks the build.
//
// To enable push: upload the file once with
//   eas env:create --name GOOGLE_SERVICES_JSON --type file \
//     --value ./google-services.json --visibility secret \
//     --environment preview --environment production
// then upload the FCM v1 key via `eas credentials` and rebuild.
module.exports = ({ config }) => {
  const googleServicesFile = process.env.GOOGLE_SERVICES_JSON;
  if (googleServicesFile) {
    config.android = { ...(config.android || {}), googleServicesFile };
  }
  // Single source of truth for the API URL: the eas.json per-profile
  // `API_URL` env (also EXPO_PUBLIC_API_URL for `expo start`) overrides the
  // app.json `extra.apiUrl` fallback. Without this the eas.json env was dead
  // and every build silently used the hardcoded app.json value.
  const apiUrl = process.env.API_URL || process.env.EXPO_PUBLIC_API_URL;
  if (apiUrl) {
    config.extra = { ...(config.extra || {}), apiUrl };
  }
  return config;
};
