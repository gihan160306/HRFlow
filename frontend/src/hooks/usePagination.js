import { useState, useMemo } from 'react';

export function usePagination(items = [], initialPageSize = 10) {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);

  const totalItems = items.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  // Auto-correct page if items change
  const validPage = Math.min(Math.max(1, currentPage), totalPages);

  const paginatedItems = useMemo(() => {
    const startIndex = (validPage - 1) * pageSize;
    return items.slice(startIndex, startIndex + pageSize);
  }, [items, validPage, pageSize]);

  const goToPage = (page) => {
    const pageNum = Math.min(Math.max(1, page), totalPages);
    setCurrentPage(pageNum);
  };

  const nextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage((prev) => prev + 1);
    }
  };

  const prevPage = () => {
    if (currentPage > 1) {
      setCurrentPage((prev) => prev - 1);
    }
  };

  return {
    currentPage: validPage,
    pageSize,
    setPageSize,
    totalItems,
    totalPages,
    paginatedItems,
    goToPage,
    nextPage,
    prevPage,
    hasNextPage: validPage < totalPages,
    hasPrevPage: validPage > 1,
  };
}

export default usePagination;
