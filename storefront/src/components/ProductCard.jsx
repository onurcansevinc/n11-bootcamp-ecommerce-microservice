import { formatPrice, initials } from "../lib/format";

export default function ProductCard({ product, onQuickView, onAddToCart }) {
  return (
    <article className="product-card">
      <button type="button" className="product-visual" onClick={() => onQuickView(product.id)}>
        <span className="discount-ribbon">Süper Fiyat</span>
        {product.mainImageUrl ? (
          <img className="product-image" src={product.mainImageUrl} alt={product.name} loading="lazy" />
        ) : (
          <span>{initials(product.name)}</span>
        )}
        <small>{product.category?.name ?? "Ürün"}</small>
      </button>

      <div className="product-meta">
        <div className="card-badges">
          <span className="badge badge-orange">Ücretsiz Kargo</span>
          {product.active ? <span className="badge">Avantajlı Fiyat</span> : null}
        </div>

        <button type="button" className="product-name" onClick={() => onQuickView(product.id)}>
          {product.name}
        </button>
        <p className="product-description">{product.description}</p>

        <div className="product-seller">n11 Bootcamp Mağazası</div>

        <div className="product-pricing">
          <strong>{formatPrice(product.price)}</strong>
          <small>Peşin fiyatına 3 taksit (demo)</small>
        </div>

        <div className="product-foot">
          <button type="button" className="mini-cta" onClick={() => onAddToCart(product)}>
            Sepete Ekle
          </button>
        </div>
      </div>
    </article>
  );
}
