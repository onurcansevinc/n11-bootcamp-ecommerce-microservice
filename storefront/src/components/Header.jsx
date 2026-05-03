import { useEffect, useRef, useState } from "react";

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

  const [categoryMenuOpen, setCategoryMenuOpen] = useState(false);
  const categoryMenuRef = useRef(null);
  const selectedCategory = (categories ?? []).find((category) => category.id === selectedCategoryId);

  useEffect(() => {
    function handlePointerDown(event) {
      if (!categoryMenuRef.current?.contains(event.target)) {
        setCategoryMenuOpen(false);
      }
    }

    function handleEscape(event) {
      if (event.key === "Escape") {
        setCategoryMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  function handleCategorySelect(categoryId) {
    onSelectCategory(categoryId);
    setCategoryMenuOpen(false);
  }

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

        <div className="category-menu-shell" ref={categoryMenuRef}>
          <button
            type="button"
            className={`category-hub ${categoryMenuOpen ? "open" : ""}`}
            aria-haspopup="menu"
            aria-expanded={categoryMenuOpen}
            onClick={() => setCategoryMenuOpen((open) => !open)}
          >
            <span className="category-hub-copy">
              <strong>Kategoriler</strong>
              <small>{selectedCategory?.name ?? "Tüm kategoriler"}</small>
            </span>
            <span className="category-hub-caret" aria-hidden="true">
              ▾
            </span>
          </button>

          {categoryMenuOpen ? (
            <div className="category-dropdown" role="menu" aria-label="Kategoriler">
              <button
                type="button"
                className={selectedCategoryId ? "category-dropdown-item" : "category-dropdown-item active"}
                onClick={() => handleCategorySelect(null)}
              >
                <strong>Tüm kategoriler</strong>
                <small>Katalogdaki tüm ürünleri göster</small>
              </button>

              {(categories ?? []).map((category) => (
                <button
                  key={category.id}
                  type="button"
                  className={
                    selectedCategoryId === category.id
                      ? "category-dropdown-item active"
                      : "category-dropdown-item"
                  }
                  onClick={() => handleCategorySelect(category.id)}
                >
                  <strong>{category.name}</strong>
                  <small>Kategorideki ürünleri listele</small>
                </button>
              ))}
            </div>
          ) : null}
        </div>

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

    </header>
  );
}
