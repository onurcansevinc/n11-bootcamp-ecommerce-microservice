import { formatPrice } from "../lib/format";

export default function HeroSection({ products, onQuickView }) {
  const spotlight = products.slice(0, 4);
  const leadProduct = spotlight[0];

  return (
    <section className="hero-grid">
      <div className="hero-stage">
        <div className="hero-main-card">
          <div className="hero-copy">
            <span className="hero-kicker">Günün Fırsatları</span>
            <h1>Aradığın ürünü bul, sepete ekle, ödemeyi tamamla.</h1>
            <p>
              Kategori, arama ve kampanya kutularıyla hızlı alışveriş akışı tek sayfada.
            </p>

            <div className="hero-badges">
              <span>Hızlı alışveriş</span>
              <span>Güvenli giriş</span>
              <span>Sandbox ödeme</span>
            </div>
          </div>

          <button
            type="button"
            className="hero-showcase"
            onClick={() => leadProduct && onQuickView(leadProduct.id)}
          >
            <small>Bugünün önerisi</small>
            <strong>{leadProduct?.name ?? "Öne çıkan ürünleri incele"}</strong>
            <span>{leadProduct ? formatPrice(leadProduct.price) : "Avantajlı fiyatlar burada"}</span>
          </button>
        </div>

        <div className="hero-promo-row">
          {spotlight.map((product) => (
            <button
              type="button"
              key={product.id}
              className="promo-tile"
              onClick={() => onQuickView(product.id)}
            >
              <small>{product.category?.name ?? "Önerilen Ürün"}</small>
              <strong>{product.name}</strong>
              <span>{formatPrice(product.price)}</span>
            </button>
          ))}
        </div>
      </div>

      <aside className="hero-sidebar">
        <div className="hero-sidebar-header">
          <span>Bugün neler var?</span>
          <strong>Alışverişe hızlı başla</strong>
        </div>

        <div className="spotlight-stack">
          <article className="spotlight-card static-card">
            <span>01</span>
            <div>
              <strong>Kupon ve kampanyalar</strong>
              <small>Günün fırsatlarını tek yerde gör</small>
            </div>
          </article>

          <article className="spotlight-card static-card">
            <span>02</span>
            <div>
              <strong>Sepetten siparişe</strong>
              <small>Ürün ekle, sipariş ver, ödemeyi tamamla</small>
            </div>
          </article>

          <article className="spotlight-card static-card">
            <span>03</span>
            <div>
              <strong>Ödeme seçenekleri</strong>
              <small>Iyzico ve PAYTR demo akışı</small>
            </div>
          </article>
        </div>
      </aside>
    </section>
  );
}
