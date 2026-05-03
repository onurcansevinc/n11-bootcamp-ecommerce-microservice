export default function Pagination({ meta, onPageChange }) {
  if (!meta || meta.totalPages <= 1) {
    return null;
  }

  const currentPage = meta.page + 1;

  return (
    <nav className="pagination-bar" aria-label="Sayfalama">
      <button type="button" disabled={!meta.hasPrevious} onClick={() => onPageChange(meta.page - 1)}>
        Önceki
      </button>

      <div className="pagination-summary">
        <strong>{currentPage}</strong>
        <span>/ {meta.totalPages}</span>
      </div>

      <button type="button" disabled={!meta.hasNext} onClick={() => onPageChange(meta.page + 1)}>
        Sonraki
      </button>
    </nav>
  );
}
