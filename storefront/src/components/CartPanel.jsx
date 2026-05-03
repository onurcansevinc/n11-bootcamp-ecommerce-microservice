import { formatPrice } from "../lib/format";

export default function CartPanel({
  open,
  cart,
  onClose,
  onQuantityChange,
  onDelete,
  onCheckout
}) {
  if (!open) {
    return null;
  }

  const items = cart?.items ?? [];

  return (
    <div className="overlay-shell" onClick={onClose}>
      <aside className="side-sheet cart-sheet" onClick={(event) => event.stopPropagation()}>
        <header className="sheet-header">
          <div>
            <small>Aktif Sepet</small>
            <h2>Sepet Ozeti</h2>
          </div>
          <button type="button" onClick={onClose}>
            Kapat
          </button>
        </header>

        {!items.length ? (
          <div className="empty-state">
            <strong>Sepetin su an bos.</strong>
            <p>Urun kartlarindan hizli ekleme yaparak akisi baslat.</p>
          </div>
        ) : (
          <>
            <div className="stack-list">
              {items.map((item) => (
                <article key={item.id} className="line-card">
                  <div>
                    <strong>{item.productName}</strong>
                    <small>{formatPrice(item.unitPrice)} x {item.quantity}</small>
                  </div>

                  <div className="line-actions">
                    <label>
                      Adet
                      <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(event) => onQuantityChange(item.id, Number(event.target.value))}
                      />
                    </label>
                    <button type="button" onClick={() => onDelete(item.id)}>
                      Sil
                    </button>
                  </div>
                </article>
              ))}
            </div>

            <footer className="sheet-footer cart-footer">
              <div className="total-box">
                <small>Toplam</small>
                <strong>{formatPrice(cart.totalAmount)}</strong>
              </div>
              <button type="button" className="primary-button" onClick={onCheckout}>
                Siparis ve Odeme
              </button>
            </footer>
          </>
        )}
      </aside>
    </div>
  );
}
