import { useState } from "react";
import { createPortal } from "react-dom";
import { demoCredentials, signinWithDemo } from "../lib/auth";

export default function DemoLoginHint({ auth, compact = false }) {
  const [open, setOpen] = useState(false);

  async function handleCopyPassword() {
    try {
      await navigator.clipboard?.writeText(demoCredentials.password);
    } catch {
      // Best-effort only.
    }
  }

  async function handleContinue() {
    await signinWithDemo(auth);
  }

  return (
    <>
      <div className={compact ? "demo-login-hint compact" : "demo-login-hint"}>
        <button
          type="button"
          className={compact ? "demo-login-chip compact" : "demo-login-chip"}
          onClick={() => setOpen(true)}
        >
          Demo Giriş
        </button>
      </div>

      {open
        ? createPortal(
            <div className="overlay-shell" onClick={() => setOpen(false)}>
              <section className="modal-shell demo-login-modal" onClick={(event) => event.stopPropagation()}>
                <header className="sheet-header">
                  <div>
                    <small>Demo Hesap</small>
                    <h2>Hazır kullanıcı ile giriş yap</h2>
                  </div>
                  <button type="button" onClick={() => setOpen(false)}>
                    Kapat
                  </button>
                </header>

                <div className="demo-login-modal-body">
                  <p>
                    Demo giriş seni Keycloak oturum ekranına yönlendirir. Güvenlik nedeniyle şifre alanı
                    otomatik dolmaz; aşağıdaki şifreyi kopyalayıp giriş ekranında yapıştırman gerekir.
                  </p>

                  <div className="demo-credentials-card">
                    <div className="demo-credential-row">
                      <span>Kullanıcı Adı</span>
                      <strong>{demoCredentials.username}</strong>
                    </div>
                    <div className="demo-credential-row">
                      <span>Şifre</span>
                      <code>{demoCredentials.password}</code>
                    </div>
                  </div>

                  <div className="detail-note">
                    `Devam Et` butonu şifreyi panoya kopyalamayı dener ve ardından giriş sayfasını açar.
                  </div>
                </div>

                <footer className="sheet-footer demo-login-footer">
                  <button type="button" className="secondary-button" onClick={handleCopyPassword}>
                    Şifreyi Kopyala
                  </button>
                  <button type="button" className="primary-button" onClick={handleContinue}>
                    Devam Et
                  </button>
                </footer>
              </section>
            </div>,
            document.body
          )
        : null}
    </>
  );
}
