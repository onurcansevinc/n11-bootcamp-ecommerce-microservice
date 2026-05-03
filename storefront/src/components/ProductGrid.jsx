import ProductCard from "./ProductCard";

export default function ProductGrid({ products, loading, onQuickView, onAddToCart }) {
  if (loading) {
    return (
      <div className="product-grid loading-grid">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="product-skeleton" />
        ))}
      </div>
    );
  }

  if (!products.length) {
    return (
      <div className="empty-state">
        <strong>Filtreye uygun urun bulunamadi.</strong>
        <p>Aramayi sadeleştir veya kategori secimini sifirla.</p>
      </div>
    );
  }

  return (
    <div className="product-grid">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          onQuickView={onQuickView}
          onAddToCart={onAddToCart}
        />
      ))}
    </div>
  );
}
