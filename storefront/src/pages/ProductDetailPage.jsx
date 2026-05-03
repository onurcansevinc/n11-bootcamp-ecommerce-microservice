import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import DemoLoginHint from "../components/DemoLoginHint";
import { addCartItem, ensureCart, fetchProduct } from "../lib/api";
import { formatDate, formatPrice, initials } from "../lib/format";

export default function ProductDetailPage() {
  const { productId } = useParams();
  const auth = useAuth();
  const accessToken = auth.user?.access_token;

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [bannerMessage, setBannerMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadProduct() {
      setLoading(true);
      setErrorMessage("");

      try {
        const response = await fetchProduct(productId);
        if (!ignore) {
          setProduct(response.data ?? null);
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error.message);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadProduct();

    return () => {
      ignore = true;
    };
  }, [productId]);

  async function handleAddToCart() {
    if (!product) {
      return;
    }

    if (!auth.isAuthenticated) {
      auth.signinRedirect();
      return;
    }

    if (!accessToken) {
      return;
    }

    setSubmitting(true);
    setBannerMessage("");
    setErrorMessage("");

    try {
      const created = await ensureCart(accessToken);
      const currentCart = created?.data;

      if (!currentCart?.id) {
        throw new Error("Sepet hazırlanamadı.");
      }

      await addCartItem(currentCart.id, product.id, 1, accessToken);
      setBannerMessage(`${product.name} sepete eklendi.`);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app-shell product-detail-page">
      <main className="product-detail-shell">
        <header className="detail-topbar">
          <Link to="/" className="brand-lockup detail-brand-link">
            <span className="brand-badge">n11</span>
            <span className="brand-copy">
              <strong>Bootcamp Storefront</strong>
              <small>Kataloğa geri dön ve alışverişe devam et</small>
            </span>
          </Link>

          {auth.isAuthenticated ? (
            <button type="button" className="ghost-chip" onClick={() => auth.signoutRedirect()}>
              Çıkış
            </button>
          ) : (
            <DemoLoginHint auth={auth} />
          )}
        </header>

        {bannerMessage ? (
          <div className="flash-banner success-banner">
            <span>{bannerMessage}</span>
            <button type="button" onClick={() => setBannerMessage("")}>
              Kapat
            </button>
          </div>
        ) : null}

        {errorMessage ? (
          <div className="flash-banner error-banner">
            <span>{errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage("")}>
              Kapat
            </button>
          </div>
        ) : null}

        <div className="product-detail-breadcrumb">
          <Link to="/">Kataloğa Dön</Link>
          <span>/</span>
          <span>{product?.category?.name ?? "Ürün Detayı"}</span>
        </div>

        {loading ? (
          <section className="surface-card detail-loading-card">
            <div className="loading-orb" />
            <strong>Ürün bilgisi yükleniyor...</strong>
          </section>
        ) : null}

        {!loading && !product ? (
          <section className="surface-card detail-loading-card">
            <strong>Ürün bulunamadı.</strong>
            <p>Listeye dönerek başka bir ürünü inceleyebilirsin.</p>
            <Link to="/" className="primary-button">
              Ana Sayfaya Dön
            </Link>
          </section>
        ) : null}

        {!loading && product ? (
          <section className="surface-card product-detail-card">
            <div className="product-detail-grid">
              <div className="product-detail-visual">
                {product.campaignLabel ? (
                  <span className="discount-ribbon product-detail-ribbon">{product.campaignLabel}</span>
                ) : null}

                {product.mainImageUrl ? (
                  <img
                    className="product-detail-image"
                    src={product.mainImageUrl}
                    alt={product.name}
                    loading="lazy"
                  />
                ) : (
                  <span className="product-detail-monogram">{initials(product.name)}</span>
                )}

                <div className="product-detail-visual-meta">
                  <span>{product.category?.name ?? "Ürün"}</span>
                  <strong>{product.sku}</strong>
                </div>
              </div>

              <div className="product-detail-copy">
                <small className="product-detail-kicker">Ürün Detayı</small>
                <h1>{product.name}</h1>
                <p>{product.description || "Bu ürün için açıklama eklenmemiş."}</p>

                <div className="card-badges">
                  <span className="badge badge-orange">Ücretsiz Kargo</span>
                  {product.active ? <span className="badge">Aynı Gün Kargo</span> : null}
                </div>

                <div className="product-detail-pricing">
                  <strong>{formatPrice(product.price)}</strong>
                  <small>Peşin fiyatına 3 taksit</small>
                </div>

                <dl className="detail-list">
                  <div>
                    <dt>Kategori</dt>
                    <dd>{product.category?.name ?? "-"}</dd>
                  </div>
                  <div>
                    <dt>SKU</dt>
                    <dd>{product.sku}</dd>
                  </div>
                  <div>
                    <dt>Durum</dt>
                    <dd>{product.active ? "Satışta" : "Pasif"}</dd>
                  </div>
                  <div>
                    <dt>Güncelleme</dt>
                    <dd>{formatDate(product.updatedAt)}</dd>
                  </div>
                </dl>

                <div className="detail-note">
                  Stok doğrulaması sipariş sırasında yapılıyor. Ürünü sepete ekleyip ödeme adımında
                  rezervasyon akışını tamamlayabilirsin.
                </div>

                <div className="product-detail-actions">
                  <Link to="/" className="secondary-button product-detail-link-button">
                    Alışverişe Dön
                  </Link>
                  <button
                    type="button"
                    className="primary-button"
                    disabled={submitting}
                    onClick={handleAddToCart}
                  >
                    {submitting ? "Ekleniyor..." : "Sepete Ekle"}
                  </button>
                </div>
              </div>
            </div>
          </section>
        ) : null}
      </main>
    </div>
  );
}
