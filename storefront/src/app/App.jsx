import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import HomePage from "../pages/HomePage";
import PaymentResultPage from "../pages/PaymentResultPage";
import ProductDetailPage from "../pages/ProductDetailPage";

export default function App() {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <div className="app-shell loading-shell">
        <div className="loading-orb" />
        <p>Magaza yukleniyor...</p>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/products/:productId" element={<ProductDetailPage />} />
      <Route path="/payment/result" element={<PaymentResultPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
