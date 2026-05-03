export default function CategoryRail({ categories, selectedCategoryId, onSelectCategory }) {
  return (
    <section className="category-rail">
      <button
        type="button"
        className={selectedCategoryId ? "category-pill" : "category-pill active"}
        onClick={() => onSelectCategory(null)}
      >
        Tüm Fırsatlar
      </button>

      {categories.map((category) => (
        <button
          key={category.id}
          type="button"
          className={selectedCategoryId === category.id ? "category-pill active" : "category-pill"}
          onClick={() => onSelectCategory(category.id)}
        >
          {category.name}
        </button>
      ))}
    </section>
  );
}
