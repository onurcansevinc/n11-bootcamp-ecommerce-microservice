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
        <small>Ödeme Sonucu</small>
        <h1>{isSuccess ? "Ödeme alındı" : "Ödeme tamamlanmadı"}</h1>
        <p>
          {isSuccess
            ? "Siparişin kayda alındı. Durumu Hesabım alanından takip edebilirsin."
            : "Ödeme durumunu Hesabım alanındaki ödeme kayıtlarından kontrol edebilirsin."}
        </p>

        <dl className="detail-list result-details">
          <div>
            <dt>Ödeme No</dt>
            <dd>{paymentId ?? "-"}</dd>
          </div>
          <div>
            <dt>Sipariş No</dt>
            <dd>{orderId ?? "-"}</dd>
          </div>
          <div>
            <dt>Durum</dt>
            <dd>{status ?? "-"}</dd>
          </div>
        </dl>

        <div className="result-actions">
          <Link to="/" className="primary-button">
            Ana Sayfaya Dön
          </Link>
        </div>
      </section>
    </main>
  );
}
