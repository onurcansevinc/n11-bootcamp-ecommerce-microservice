import { formatPrice } from "../lib/format";

export default function HeroSection({ products, onQuickView }) {
  const spotlight = products.slice(0, 4);
  const leadProduct = spotlight[0];

  return (
    <section className="hero-grid">
      <div className="hero-stage">
        <div className="hero-main-card">
          <div className="hero-copy">
            <span className="hero-kicker">Super Firsatlar Koridoru</span>
            <h1>Tek ekranda urun, sepet, siparis ve odeme akisi.</h1>
            <p>
              n11 ana sayfasindaki yogun marketplace ritmine yakin bir vitrin: buyuk kampanya
              alani, coklu promosyon kutulari ve hizli urun gecisleri.
            </p>

            <div className="hero-badges">
              <span>Ayni gun sepet aksiyonu</span>
              <span>Keycloak ile guvenli giris</span>
              <span>Iyzico sandbox checkout</span>
            </div>
          </div>

          <button
            type="button"
            className="hero-showcase"
            onClick={() => leadProduct && onQuickView(leadProduct.id)}
          >
            <small>Gunluk One Cikan</small>
            <strong>{leadProduct?.name ?? "Bugun vitrindeki urunler yenileniyor"}</strong>
            <span>{leadProduct ? formatPrice(leadProduct.price) : "Kampanya bekleniyor"}</span>
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
              <small>{product.category?.name ?? "One Cikan"}</small>
              <strong>{product.name}</strong>
              <span>{formatPrice(product.price)}</span>
            </button>
          ))}
        </div>
      </div>

      <aside className="hero-sidebar">
        <div className="hero-sidebar-header">
          <span>Hizli Koridorlar</span>
          <strong>Alisveris rehberi</strong>
        </div>

        <div className="spotlight-stack">
          <article className="spotlight-card static-card">
            <span>01</span>
            <div>
              <strong>Kupon ve kampanya vitrini</strong>
              <small>n11 hissini veren yogun promosyon alani</small>
            </div>
          </article>

          <article className="spotlight-card static-card">
            <span>02</span>
            <div>
              <strong>Sepetten odemeye tek akis</strong>
              <small>Cart, order, payment ve notification zinciri</small>
            </div>
          </article>

          <article className="spotlight-card static-card">
            <span>03</span>
            <div>
              <strong>Demo odeme secenekleri</strong>
              <small>Iyzico checkout ve PAYTR simulasyonu</small>
            </div>
          </article>
        </div>
      </aside>
    </section>
  );
}
