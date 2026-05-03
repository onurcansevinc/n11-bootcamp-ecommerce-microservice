import { formatDate, formatPrice } from "../lib/format";

export default function AccountPanel({
  open,
  onClose,
  orders,
  payments,
  loading,
  onContinuePayment,
  onSandboxSuccess,
  onSandboxFailure
}) {
  if (!open) {
    return null;
  }

  return (
    <div className="overlay-shell" onClick={onClose}>
      <aside className="side-sheet account-sheet" onClick={(event) => event.stopPropagation()}>
        <header className="sheet-header">
          <div>
            <small>Hesabim</small>
            <h2>Siparis ve Odeme Akisi</h2>
          </div>
          <button type="button" onClick={onClose}>
            Kapat
          </button>
        </header>

        {loading ? (
          <div className="empty-state">
            <strong>Kayitlar yukleniyor...</strong>
          </div>
        ) : (
          <div className="account-columns">
            <section className="account-section">
              <div className="section-head">
                <h3>Son Siparisler</h3>
              </div>
              {orders.length ? (
                <div className="stack-list">
                  {orders.map((order) => (
                    <article key={order.id} className="line-card compact-card">
                      <div>
                        <strong>{order.id}</strong>
                        <small>{formatDate(order.createdAt)}</small>
                      </div>
                      <div className="status-block">
                        <span>{order.status}</span>
                        <strong>{formatPrice(order.totalAmount)}</strong>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="empty-inline">Henuz siparis yok.</div>
              )}
            </section>

            <section className="account-section">
              <div className="section-head">
                <h3>Son Odemeler</h3>
              </div>
              {payments.length ? (
                <div className="stack-list">
                  {payments.map((payment) => (
                    <article key={payment.id} className="line-card compact-card payment-card">
                      <div>
                        <strong>{payment.orderId}</strong>
                        <small>{payment.provider}</small>
                      </div>

                      <div className="status-block">
                        <span>{payment.status}</span>
                        <strong>{formatPrice(payment.amount)}</strong>
                      </div>

                      {payment.status === "PENDING" ? (
                        <div className="payment-actions">
                          <button type="button" onClick={() => onContinuePayment(payment)}>
                            Odeme Ekranini Ac
                          </button>
                          {payment.provider === "PAYTR" ? (
                            <>
                              <button type="button" onClick={() => onSandboxSuccess(payment.id)}>
                                Basarili Simule Et
                              </button>
                              <button type="button" onClick={() => onSandboxFailure(payment.id)}>
                                Basarisiz Simule Et
                              </button>
                            </>
                          ) : null}
                        </div>
                      ) : null}
                    </article>
                  ))}
                </div>
              ) : (
                <div className="empty-inline">Henuz odeme kaydi yok.</div>
              )}
            </section>
          </div>
        )}
      </aside>
    </div>
  );
}
