export default function Header({
  auth,
  searchInput,
  onSearchInputChange,
  onSearchSubmit,
  cartItemCount,
  onCartOpen,
  onAccountOpen,
  categories,
  selectedCategoryId,
  onSelectCategory
}) {
  const displayName =
    auth.user?.profile?.given_name ??
    auth.user?.profile?.preferred_username ??
    auth.user?.profile?.name ??
    "Musteri";

  const navigationItems = (categories ?? []).slice(0, 8);

  return (
    <header className="site-header">
      <div className="utility-bar">
        <div className="utility-meta">
          <span>Magazana Ozel</span>
          <span>Kupon ve Kampanyalar</span>
          <span>Hizli Teslimat</span>
        </div>
        <div className="utility-links">
          <button type="button">Yardim</button>
          <button type="button">Siparis Takibi</button>
          <button type="button" onClick={onAccountOpen}>
            Siparislerim
          </button>
          <button type="button" onClick={onCartOpen}>
            Sepetim
          </button>
        </div>
      </div>

      <div className="header-main">
        <button
          type="button"
          className="brand-lockup"
          onClick={() => {
            onSelectCategory(null);
            window.scrollTo({ top: 0, behavior: "smooth" });
          }}
        >
          <span className="brand-badge">n11</span>
          <span className="brand-copy">
            <strong>Bootcamp Storefront</strong>
            <small>Turuncu firsatlar, hizli sepet ve demo checkout</small>
          </span>
        </button>

        <button type="button" className="category-hub" onClick={() => onSelectCategory(null)}>
          <strong>Kategoriler</strong>
          <small>Tum urunler</small>
        </button>

        <form className="search-bar" onSubmit={onSearchSubmit}>
          <input
            type="search"
            placeholder="Telefon, kulaklik, kahve makinesi, hediyelik..."
            value={searchInput}
            onChange={(event) => onSearchInputChange(event.target.value)}
          />
          <button type="submit">Ara</button>
        </form>

        <div className="header-actions">
          <button type="button" className="ghost-chip action-chip" onClick={onAccountOpen}>
            <small>Hesabim</small>
            <strong>{displayName}</strong>
          </button>

          {auth.isAuthenticated ? (
            <>
              <button type="button" className="ghost-chip" onClick={() => auth.signoutRedirect()}>
                Cikis
              </button>
            </>
          ) : (
            <button type="button" className="primary-chip" onClick={() => auth.signinRedirect()}>
              Giris Yap
            </button>
          )}

          <button type="button" className="cart-chip" onClick={onCartOpen}>
            <span>Sepetim</span>
            <strong>{cartItemCount}</strong>
          </button>
        </div>
      </div>

      <div className="nav-strip">
        <button
          type="button"
          className={selectedCategoryId ? "nav-link" : "nav-link active"}
          onClick={() => onSelectCategory(null)}
        >
          Tum Firsatlar
        </button>

        {navigationItems.map((category) => (
          <button
            key={category.id}
            type="button"
            className={selectedCategoryId === category.id ? "nav-link active" : "nav-link"}
            onClick={() => onSelectCategory(category.id)}
          >
            {category.name}
          </button>
        ))}
      </div>
    </header>
  );
}
