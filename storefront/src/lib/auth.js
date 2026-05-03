import { storefrontConfig } from "./config";

export const oidcConfig = {
  authority: storefrontConfig.oidcAuthority,
  client_id: storefrontConfig.oidcClientId,
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  response_type: "code",
  scope: "openid profile email",
  automaticSilentRenew: false,
  loadUserInfo: true,
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname + window.location.hash);
  }
};

export const demoCredentials = {
  username: "customer-user",
  password: "customer123"
};

export async function signinWithDemo(auth) {
  try {
    await navigator.clipboard?.writeText(demoCredentials.password);
  } catch {
    // Best-effort only.
  }

  return auth.signinRedirect({
    extraQueryParams: {
      login_hint: demoCredentials.username
    }
  });
}
