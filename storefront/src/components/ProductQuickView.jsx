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
            <small>Urun Detayi</small>
            <h2>{product.name}</h2>
          </div>
          <button type="button" onClick={onClose}>
            Kapat
          </button>
        </header>

        <div className="quick-view-body">
          <div className="quick-view-visual">
            <span>{product.category?.name ?? "Urun"}</span>
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
                <dt>Guncelleme</dt>
                <dd>{formatDate(product.updatedAt)}</dd>
              </div>
            </dl>

            <div className="detail-note">
              Canli stok verisi musteri scope'u ile acik olmadigi icin urun kartinda gizli tutuluyor.
              Stok dogrulamasi checkout sirasinda yapiliyor.
            </div>
          </div>
        </div>

        <footer className="sheet-footer">
          <button type="button" className="secondary-button" onClick={onClose}>
            Vitrine Don
          </button>
          <button type="button" className="primary-button" onClick={() => onAddToCart(product)}>
            Sepete Ekle
          </button>
        </footer>
      </section>
    </div>
  );
}
