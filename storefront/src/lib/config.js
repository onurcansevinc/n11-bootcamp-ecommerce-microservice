export const storefrontConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8760",
  oidcAuthority:
    import.meta.env.VITE_OIDC_AUTHORITY ?? "http://localhost:8080/realms/ecommerce",
  oidcClientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? "storefront-web"
};
