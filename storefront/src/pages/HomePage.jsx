import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import AccountPanel from "../components/AccountPanel";
import CartPanel from "../components/CartPanel";
import CheckoutModal from "../components/CheckoutModal";
import Header from "../components/Header";
import HeroSection from "../components/HeroSection";
import Pagination from "../components/Pagination";
import ProductGrid from "../components/ProductGrid";
import {
  addCartItem,
  createOrder,
  createPayment,
  deleteCartItem,
  ensureCart,
  fetchCart,
  fetchCategories,
  fetchOrders,
  fetchPayments,
  fetchProducts,
  simulatePaymentFailure,
  simulatePaymentSuccess,
  updateCartItem
} from "../lib/api";

function createCheckoutDraft(profile) {
  const givenName = profile?.given_name ?? profile?.name?.split(" ")?.[0] ?? "Misafir";
  const surname = profile?.family_name ?? profile?.name?.split(" ").slice(1).join(" ") ?? "Kullanıcı";
  const email = profile?.email ?? "";

  return {
    provider: "IYZICO",
    checkout: {
      locale: "tr",
      buyer: {
        name: givenName,
        surname,
        email,
        gsmNumber: "905555555555",
        identityNumber: "11111111110",
        registrationAddress: "Maslak Mahallesi Buyukdere Caddesi No:1",
        city: "İstanbul",
        country: "Türkiye",
        zipCode: "34000"
      },
      billingAddress: {
        contactName: `${givenName} ${surname}`.trim(),
        city: "İstanbul",
        country: "Türkiye",
        address: "Maslak Mahallesi Buyukdere Caddesi No:1",
        zipCode: "34000"
      },
      shippingAddress: {
        contactName: `${givenName} ${surname}`.trim(),
        city: "İstanbul",
        country: "Türkiye",
        address: "Maslak Mahallesi Buyukdere Caddesi No:1",
        zipCode: "34000"
      }
    }
  };
}

export default function HomePage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const accessToken = auth.user?.access_token;
  const isAuthenticated = auth.isAuthenticated;

  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [meta, setMeta] = useState(null);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategoryId, setSelectedCategoryId] = useState(null);
  const [page, setPage] = useState(0);
  const [bannerMessage, setBannerMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [cart, setCart] = useState(null);
  const [cartOpen, setCartOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [orders, setOrders] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loadingAccount, setLoadingAccount] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [checkoutDraft, setCheckoutDraft] = useState(createCheckoutDraft(auth.user?.profile));
  const [submittingCheckout, setSubmittingCheckout] = useState(false);

  const cartItemCount = useMemo(
    () => (cart?.items ?? []).reduce((total, item) => total + item.quantity, 0),
    [cart]
  );

  useEffect(() => {
    setCheckoutDraft(createCheckoutDraft(auth.user?.profile));
  }, [auth.user?.profile]);

  useEffect(() => {
    let ignore = false;

    async function loadCategories() {
      try {
        const response = await fetchCategories();
        if (!ignore) {
          setCategories(response.data ?? []);
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error.message);
        }
      }
    }

    loadCategories();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadProducts() {
      setLoadingProducts(true);
      try {
        const response = await fetchProducts({
          page,
          size: 12,
          search: searchQuery,
          categoryId: selectedCategoryId
        });

        if (!ignore) {
          setProducts(response.data ?? []);
          setMeta(response.meta ?? null);
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error.message);
        }
      } finally {
        if (!ignore) {
          setLoadingProducts(false);
        }
      }
    }

    loadProducts();

    return () => {
      ignore = true;
    };
  }, [page, searchQuery, selectedCategoryId]);

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      setCart(null);
      return;
    }

    refreshCart();
  }, [isAuthenticated, accessToken]);

  useEffect(() => {
    if (!accountOpen || !accessToken || !isAuthenticated) {
      return;
    }

    loadAccountData();
  }, [accountOpen, accessToken, isAuthenticated]);

  async function refreshCart() {
    if (!accessToken) {
      return null;
    }

    try {
      const created = await ensureCart(accessToken);
      const currentCart = created?.data;

      if (!currentCart?.id) {
        return null;
      }

      const refreshed = await fetchCart(currentCart.id, accessToken);
      setCart(refreshed?.data ?? currentCart);
      return refreshed?.data ?? currentCart;
    } catch (error) {
      setErrorMessage(error.message);
      return null;
    }
  }

  async function loadAccountData() {
    if (!accessToken) {
      return;
    }

    setLoadingAccount(true);
    try {
      const [ordersResponse, paymentsResponse] = await Promise.all([
        fetchOrders(accessToken),
        fetchPayments(accessToken)
      ]);

      setOrders(ordersResponse.data ?? []);
      setPayments(paymentsResponse.data ?? []);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setLoadingAccount(false);
    }
  }

  function handleQuickView(productId) {
    navigate(`/products/${productId}`);
  }

  async function handleAddToCart(product) {
    if (!isAuthenticated) {
      auth.signinRedirect();
      return;
    }

    const activeCart = cart ?? (await refreshCart());
    if (!activeCart?.id || !accessToken) {
      return;
    }

    try {
      const response = await addCartItem(activeCart.id, product.id, 1, accessToken);
      setCart(response.data);
      setCartOpen(true);
      setBannerMessage(`${product.name} sepete eklendi.`);
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  async function handleQuantityChange(itemId, quantity) {
    if (!cart?.id || !accessToken || Number.isNaN(quantity) || quantity < 1) {
      return;
    }

    try {
      const response = await updateCartItem(cart.id, itemId, quantity, accessToken);
      setCart(response.data);
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  async function handleDeleteItem(itemId) {
    if (!cart?.id || !accessToken) {
      return;
    }

    try {
      await deleteCartItem(cart.id, itemId, accessToken);
      await refreshCart();
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  async function handleCheckoutSubmit() {
    if (!cart?.id || !accessToken) {
      return;
    }

    setSubmittingCheckout(true);
    setErrorMessage("");

    try {
      const orderResponse = await createOrder(cart.id, accessToken);
      const order = orderResponse.data;

      const paymentResponse = await createPayment(
        {
          orderId: order.id,
          provider: checkoutDraft.provider,
          checkout: checkoutDraft.checkout
        },
        accessToken
      );

      const payment = paymentResponse.data;
      setBannerMessage(`Ödeme kaydı oluşturuldu. Ödeme No: ${payment.id}`);
      setCheckoutOpen(false);
      await loadAccountData();

      if (payment.provider === "IYZICO" && payment.checkoutUrl) {
        window.location.assign(payment.checkoutUrl);
        return;
      }

      setAccountOpen(true);
      setBannerMessage("PAYTR ödemesi hazır. Sonucu Hesabım alanından yönetebilirsin.");
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setSubmittingCheckout(false);
    }
  }

  async function handleContinuePayment(payment) {
    if (!payment?.checkoutUrl) {
      setErrorMessage("Bu ödeme için ödeme linki bulunamadı.");
      return;
    }

    if (payment.provider === "PAYTR") {
      setBannerMessage("PAYTR ödemesini Hesabım alanındaki butonlarla sonuçlandırabilirsin.");
      return;
    }

    window.open(payment.checkoutUrl, "_blank", "noopener,noreferrer");
  }

  async function handleSandboxSuccess(paymentId) {
    if (!accessToken) {
      return;
    }

    try {
      await simulatePaymentSuccess(paymentId, accessToken);
      setBannerMessage("Ödeme başarılı olarak güncellendi.");
      await loadAccountData();
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  async function handleSandboxFailure(paymentId) {
    if (!accessToken) {
      return;
    }

    try {
      await simulatePaymentFailure(paymentId, accessToken);
      setBannerMessage("Ödeme başarısız olarak güncellendi.");
      await loadAccountData();
    } catch (error) {
      setErrorMessage(error.message);
    }
  }

  function handleSearchSubmit(event) {
    event.preventDefault();
    setPage(0);
    setSearchQuery(searchInput.trim());
  }

  return (
    <div className="app-shell">
      <Header
        auth={auth}
        searchInput={searchInput}
        onSearchInputChange={setSearchInput}
        onSearchSubmit={handleSearchSubmit}
        cartItemCount={cartItemCount}
        categories={categories}
        selectedCategoryId={selectedCategoryId}
        onSelectCategory={(categoryId) => {
          setSelectedCategoryId(categoryId);
          setPage(0);
        }}
        onCartOpen={() => setCartOpen(true)}
        onAccountOpen={() => {
          if (!isAuthenticated) {
            auth.signinRedirect();
            return;
          }
          setAccountOpen(true);
        }}
      />

      <main className="page-layout">
        {bannerMessage ? (
          <div className="flash-banner success-banner">
            <span>{bannerMessage}</span>
            <button type="button" onClick={() => setBannerMessage("")}>
              Kapat
            </button>
          </div>
        ) : null}

        {errorMessage ? (
          <div className="flash-banner error-banner">
            <span>{errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage("")}>
              Kapat
            </button>
          </div>
        ) : null}

        <HeroSection products={products} onQuickView={handleQuickView} />

        <section className="section-block">
          <div className="section-head">
            <div>
              <small>Ürünler</small>
              <h2>Öne çıkan ürünler</h2>
            </div>
            <p>
              Arama, kategori ve sayfalama ile canlı katalog deneyimi.
            </p>
          </div>

          <ProductGrid
            products={products}
            loading={loadingProducts}
            onQuickView={handleQuickView}
            onAddToCart={handleAddToCart}
          />

          <Pagination meta={meta} onPageChange={setPage} />
        </section>
      </main>

      <CartPanel
        open={cartOpen}
        cart={cart}
        onClose={() => setCartOpen(false)}
        onQuantityChange={handleQuantityChange}
        onDelete={handleDeleteItem}
        onCheckout={() => {
          if (!isAuthenticated) {
            auth.signinRedirect();
            return;
          }
          setCheckoutOpen(true);
        }}
      />

      <AccountPanel
        open={accountOpen}
        onClose={() => setAccountOpen(false)}
        orders={orders}
        payments={payments}
        loading={loadingAccount}
        onContinuePayment={handleContinuePayment}
        onSandboxSuccess={handleSandboxSuccess}
        onSandboxFailure={handleSandboxFailure}
      />

      <CheckoutModal
        open={checkoutOpen}
        checkoutDraft={checkoutDraft}
        onDraftChange={setCheckoutDraft}
        onClose={() => setCheckoutOpen(false)}
        onSubmit={handleCheckoutSubmit}
        submitting={submittingCheckout}
      />
    </div>
  );
}
