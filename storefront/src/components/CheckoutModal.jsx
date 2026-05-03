const addressFields = [
  ["contactName", "Iletisim Adi"],
  ["address", "Adres"],
  ["city", "Sehir"],
  ["country", "Ulke"],
  ["zipCode", "Posta Kodu"]
];

export default function CheckoutModal({
  open,
  checkoutDraft,
  onDraftChange,
  onClose,
  onSubmit,
  submitting
}) {
  if (!open) {
    return null;
  }

  const updateBuyerField = (field, value) => {
    onDraftChange({
      ...checkoutDraft,
      checkout: {
        ...checkoutDraft.checkout,
        buyer: {
          ...checkoutDraft.checkout.buyer,
          [field]: value
        }
      }
    });
  };

  const updateAddressField = (section, field, value) => {
    onDraftChange({
      ...checkoutDraft,
      checkout: {
        ...checkoutDraft.checkout,
        [section]: {
          ...checkoutDraft.checkout[section],
          [field]: value
        }
      }
    });
  };

  return (
    <div className="overlay-shell" onClick={onClose}>
      <section className="modal-shell checkout-modal" onClick={(event) => event.stopPropagation()}>
        <header className="sheet-header">
          <div>
            <small>Checkout</small>
            <h2>Siparis ve Odeme Bilgileri</h2>
          </div>
          <button type="button" onClick={onClose}>
            Kapat
          </button>
        </header>

        <div className="checkout-layout">
          <section className="form-card">
            <div className="section-head">
              <h3>Odeme Saglayicisi</h3>
            </div>

            <div className="provider-switch">
              {["IYZICO", "PAYTR"].map((provider) => (
                <button
                  key={provider}
                  type="button"
                  className={checkoutDraft.provider === provider ? "provider-option active" : "provider-option"}
                  onClick={() => onDraftChange({ ...checkoutDraft, provider })}
                >
                  {provider === "IYZICO" ? "Iyzico Sandbox" : "PAYTR Demo"}
                </button>
              ))}
            </div>
          </section>

          <section className="form-card">
            <div className="section-head">
              <h3>Alici Bilgileri</h3>
            </div>

            <div className="form-grid">
              <label>
                Ad
                <input value={checkoutDraft.checkout.buyer.name} onChange={(event) => updateBuyerField("name", event.target.value)} />
              </label>
              <label>
                Soyad
                <input value={checkoutDraft.checkout.buyer.surname} onChange={(event) => updateBuyerField("surname", event.target.value)} />
              </label>
              <label>
                E-posta
                <input value={checkoutDraft.checkout.buyer.email} onChange={(event) => updateBuyerField("email", event.target.value)} />
              </label>
              <label>
                GSM
                <input value={checkoutDraft.checkout.buyer.gsmNumber} onChange={(event) => updateBuyerField("gsmNumber", event.target.value)} />
              </label>
              <label>
                T.C. Kimlik
                <input value={checkoutDraft.checkout.buyer.identityNumber} onChange={(event) => updateBuyerField("identityNumber", event.target.value)} />
              </label>
              <label>
                Kayit Adresi
                <input
                  value={checkoutDraft.checkout.buyer.registrationAddress}
                  onChange={(event) => updateBuyerField("registrationAddress", event.target.value)}
                />
              </label>
            </div>
          </section>

          {["billingAddress", "shippingAddress"].map((section) => (
            <section key={section} className="form-card">
              <div className="section-head">
                <h3>{section === "billingAddress" ? "Fatura Adresi" : "Teslimat Adresi"}</h3>
              </div>

              <div className="form-grid">
                {addressFields.map(([field, label]) => (
                  <label key={field}>
                    {label}
                    <input
                      value={checkoutDraft.checkout[section][field]}
                      onChange={(event) => updateAddressField(section, field, event.target.value)}
                    />
                  </label>
                ))}
              </div>
            </section>
          ))}
        </div>

        <footer className="sheet-footer">
          <button type="button" className="secondary-button" onClick={onClose}>
            Vazgec
          </button>
          <button type="button" className="primary-button" disabled={submitting} onClick={onSubmit}>
            {submitting ? "Hazirlaniyor..." : "Siparis Olustur ve Odeme Baslat"}
          </button>
        </footer>
      </section>
    </div>
  );
}
