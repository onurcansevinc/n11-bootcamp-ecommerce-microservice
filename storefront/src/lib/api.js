import { storefrontConfig } from "./config";

async function request(path, options = {}, accessToken) {
  const response = await fetch(`${storefrontConfig.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(options.headers ?? {})
    }
  });

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type");
  const payload = contentType?.includes("application/json") ? await response.json() : await response.text();

  if (!response.ok) {
    const error = new Error(
      typeof payload === "object" && payload?.message
        ? payload.message
        : typeof payload === "object" && payload?.detail
          ? payload.detail
          : "Beklenmeyen bir servis hatasi olustu."
    );
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

export function fetchProducts({ page, size, search, categoryId }) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    active: "true"
  });

  if (search) {
    params.set("search", search);
  }

  if (categoryId) {
    params.set("categoryId", String(categoryId));
  }

  return request(`/api/v1/products?${params.toString()}`);
}

export function fetchCategories() {
  return request("/api/v1/categories?page=0&size=12&active=true");
}

export function fetchProduct(productId) {
  return request(`/api/v1/products/${productId}`);
}

export function ensureCart(accessToken) {
  return request("/api/v1/carts", { method: "POST" }, accessToken);
}

export function fetchCart(cartId, accessToken) {
  return request(`/api/v1/carts/${cartId}`, {}, accessToken);
}

export function addCartItem(cartId, productId, quantity, accessToken) {
  return request(
    `/api/v1/carts/${cartId}/items`,
    {
      method: "POST",
      body: JSON.stringify({ productId, quantity })
    },
    accessToken
  );
}

export function updateCartItem(cartId, itemId, quantity, accessToken) {
  return request(
    `/api/v1/carts/${cartId}/items/${itemId}`,
    {
      method: "PATCH",
      body: JSON.stringify({ quantity })
    },
    accessToken
  );
}

export function deleteCartItem(cartId, itemId, accessToken) {
  return request(
    `/api/v1/carts/${cartId}/items/${itemId}`,
    { method: "DELETE" },
    accessToken
  );
}

export function createOrder(cartId, accessToken) {
  return request(
    "/api/v1/orders",
    {
      method: "POST",
      body: JSON.stringify({ cartId })
    },
    accessToken
  );
}

export function fetchOrders(accessToken) {
  return request("/api/v1/orders?page=0&size=10", {}, accessToken);
}

export function createPayment(body, accessToken) {
  return request(
    "/api/v1/payments",
    {
      method: "POST",
      body: JSON.stringify(body)
    },
    accessToken
  );
}

export function simulatePaymentSuccess(paymentId, accessToken) {
  return request(`/api/v1/payments/${paymentId}/simulate-success`, { method: "POST" }, accessToken);
}

export function simulatePaymentFailure(paymentId, accessToken) {
  return request(`/api/v1/payments/${paymentId}/simulate-failure`, { method: "POST" }, accessToken);
}

export function fetchPayments(accessToken) {
  return request("/api/v1/payments?page=0&size=10", {}, accessToken);
}
