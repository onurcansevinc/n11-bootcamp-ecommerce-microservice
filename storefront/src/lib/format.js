export function formatPrice(value) {
  if (value == null) {
    return "-";
  }

  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    maximumFractionDigits: 2
  }).format(Number(value));
}

export function formatDate(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function initials(input) {
  if (!input) {
    return "N";
  }

  return input
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((segment) => segment[0]?.toUpperCase())
    .join("");
}

export function formatOrderStatus(value) {
  switch (value) {
    case "PENDING_PAYMENT":
      return "Ödeme Bekleniyor";
    case "PAID":
      return "Ödeme Alındı";
    case "PAYMENT_FAILED":
      return "Ödeme Başarısız";
    default:
      return value ?? "-";
  }
}

export function formatPaymentStatus(value) {
  switch (value) {
    case "PENDING":
      return "Beklemede";
    case "SUCCESS":
      return "Başarılı";
    case "FAILED":
      return "Başarısız";
    default:
      return value ?? "-";
  }
}

export function formatPaymentProvider(value) {
  switch (value) {
    case "IYZICO":
      return "iyzico";
    case "PAYTR":
      return "PayTR";
    default:
      return value ?? "-";
  }
}

export function formatOrderItems(items = []) {
  if (!items.length) {
    return "Ürün detayı yok";
  }

  const names = items
    .map((item) => item.productName)
    .filter(Boolean);

  if (!names.length) {
    return `${items.length} ürün`;
  }

  const preview = names.slice(0, 2).join(", ");
  const remaining = names.length - 2;

  if (remaining > 0) {
    return `${preview} + ${remaining} ürün`;
  }

  return preview;
}

export function formatShortId(value) {
  if (!value) {
    return "-";
  }

  if (value.length <= 12) {
    return value;
  }

  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}
