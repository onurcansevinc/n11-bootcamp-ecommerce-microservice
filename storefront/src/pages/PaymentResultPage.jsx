import { Link, useSearchParams } from "react-router-dom";

export default function PaymentResultPage() {
  const [searchParams] = useSearchParams();
  const paymentId = searchParams.get("paymentId");
  const orderId = searchParams.get("orderId");
  const status = searchParams.get("status");

  const isSuccess = status === "SUCCESS";

  return (
    <main className="result-page">
      <section className={isSuccess ? "result-card success" : "result-card warning"}>
        <small>Odeme Sonucu</small>
        <h1>{isSuccess ? "Odemeniz alindi" : "Odeme beklemede ya da basarisiz"}</h1>
        <p>
          {isSuccess
            ? "Siparisiniz kayda alindi. Bildirimler event akisi uzerinden gonderilecek."
            : "Durumu hesap panelinden veya odeme kayitlarindan takip edebilirsin."}
        </p>

        <dl className="detail-list result-details">
          <div>
            <dt>Payment ID</dt>
            <dd>{paymentId ?? "-"}</dd>
          </div>
          <div>
            <dt>Order ID</dt>
            <dd>{orderId ?? "-"}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{status ?? "-"}</dd>
          </div>
        </dl>

        <div className="result-actions">
          <Link to="/" className="primary-button">
            Vitrine Don
          </Link>
        </div>
      </section>
    </main>
  );
}
