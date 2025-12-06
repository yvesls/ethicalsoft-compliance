export interface Sort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface Pageable {
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  sort: Sort;
}

export interface Page<T> {
  content: T[];
  pageable: Pageable;
  size: number;
  totalElements: number;
  totalPages: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
}

export function sort(): Sort {
  return {
    empty: false,
    sorted: false,
    unsorted: false,
  };
}
export function pageable(): Pageable {
  return {
    offset: 0,
    pageNumber: 0,
    pageSize: 0,
    paged: false,
    sort: sort(),
  };
}

export function Page<T>(): Page<T> {
  return {
    content: [],
    pageable: pageable(),
    size: 0,
    totalElements: 0,
    totalPages: 0,
    number: 0,
    first: true,
    last: true,
    empty: true,
    numberOfElements: 0,
  };
}
