import { formatPrice, formatDate } from "../lib/format";

export default function ProductQuickView({ product, open, onClose, onAddToCart }) {
  if (!open || !product) {
    return null;
  }

  return (
    <div className="overlay-shell" onClick={onClose}>
      <section className="side-sheet quick-view-sheet" onClick={(event) => event.stopPropagation()}>
        <header className="sheet-header">
          <div>
            <small>Ürün Bilgisi</small>
            <h2>{product.name}</h2>
          </div>
          <button type="button" onClick={onClose}>
            Kapat
          </button>
        </header>

        <div className="quick-view-body">
          <div className="quick-view-visual">
            {product.mainImageUrl ? (
              <img className="quick-view-image" src={product.mainImageUrl} alt={product.name} loading="lazy" />
            ) : null}
            <span>{product.category?.name ?? "Ürün"}</span>
            <strong>{product.sku}</strong>
          </div>

          <div className="quick-view-copy">
            <p>{product.description}</p>
            <dl className="detail-list">
              <div>
                <dt>Fiyat</dt>
                <dd>{formatPrice(product.price)}</dd>
              </div>
              <div>
                <dt>Kategori</dt>
                <dd>{product.category?.name ?? "-"}</dd>
              </div>
              <div>
                <dt>SKU</dt>
                <dd>{product.sku}</dd>
              </div>
              <div>
                <dt>Güncelleme</dt>
                <dd>{formatDate(product.updatedAt)}</dd>
              </div>
            </dl>

            <div className="detail-note">
              Stok bilgisi bu ekranda gösterilmiyor. Kontrol sipariş aşamasında yapılıyor.
            </div>
          </div>
        </div>

        <footer className="sheet-footer">
          <button type="button" className="secondary-button" onClick={onClose}>
            Geri Dön
          </button>
          <button type="button" className="primary-button" onClick={() => onAddToCart(product)}>
            Sepete Ekle
          </button>
        </footer>
      </section>
    </div>
  );
}
