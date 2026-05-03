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
    "Misafir";

  const navigationItems = (categories ?? []).slice(0, 8);

  return (
    <header className="site-header">
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
            <small>Ürün, sepet, sipariş ve ödeme akışı</small>
          </span>
        </button>

        <button type="button" className="category-hub" onClick={() => onSelectCategory(null)}>
          <strong>Kategoriler</strong>
          <small>Tüm kategoriler</small>
        </button>

        <form className="search-bar" onSubmit={onSearchSubmit}>
          <input
            type="search"
            placeholder="Telefon, kulaklık, kahve makinesi, hediyelik..."
            value={searchInput}
            onChange={(event) => onSearchInputChange(event.target.value)}
          />
          <button type="submit">Ara</button>
        </form>

        <div className="header-actions">
          <button type="button" className="ghost-chip action-chip" onClick={onAccountOpen}>
            <small>Hesabım</small>
            <strong>{displayName}</strong>
          </button>

          {auth.isAuthenticated ? (
            <>
              <button type="button" className="ghost-chip" onClick={() => auth.signoutRedirect()}>
                Çıkış
              </button>
            </>
          ) : (
            <button type="button" className="primary-chip" onClick={() => auth.signinRedirect()}>
              Giriş Yap
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
          Tüm Kategoriler
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
