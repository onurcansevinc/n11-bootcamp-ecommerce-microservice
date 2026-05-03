import { formatPrice, initials } from "../lib/format";

export default function ProductCard({ product, onQuickView, onAddToCart }) {
  return (
    <article className="product-card">
      <button type="button" className="product-visual" onClick={() => onQuickView(product.id)}>
        <span className="discount-ribbon">Super Fiyat</span>
        <span>{initials(product.name)}</span>
        <small>{product.category?.name ?? "Urun"}</small>
      </button>

      <div className="product-meta">
        <div className="card-badges">
          <span className="badge badge-orange">Ucretsiz Kargo</span>
          {product.active ? <span className="badge">Bugune Ozel</span> : null}
        </div>

        <button type="button" className="product-name" onClick={() => onQuickView(product.id)}>
          {product.name}
        </button>
        <p className="product-description">{product.description}</p>

        <div className="product-seller">Bootcamp Magazasi</div>

        <div className="product-pricing">
          <strong>{formatPrice(product.price)}</strong>
          <small>Peşin fiyatina 3 taksit demosu</small>
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
