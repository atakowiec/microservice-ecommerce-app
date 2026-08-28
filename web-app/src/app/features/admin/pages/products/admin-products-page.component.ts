import {Component, inject, signal} from '@angular/core';
import {
  AdminProductService,
  AdminProductSortColumn,
  AdminProductSortDirection,
} from "../../data-access/product/admin-product.service";
import {AsyncPipe} from "@angular/common";
import AdminProduct from "../../data-access/product/admin-product.model";
import {BehaviorSubject, map, Observable, shareReplay, startWith, switchMap} from "rxjs";
import {FormControl, ReactiveFormsModule} from "@angular/forms";

type ProductsState =
  { status: "loading" } |
  { status: "loaded", data: AdminProduct[] } |
  { status: "error", message: string }

type ProductSortDirection = "ascending" | "descending";
type ProductSort = {
  column: AdminProductSortColumn,
  direction: ProductSortDirection,
};

type ProductQuery = {
  page: number,
  size: number,
  sort: ProductSort,
  query: string,
};

@Component({
  selector: 'app-admin-products-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
  ],
  templateUrl: './admin-products-page.component.html',
  styleUrl: './admin-products-page.component.scss'
})
export class AdminProductsPageComponent {
  private readonly productService = inject(AdminProductService);
  private readonly query$ = new BehaviorSubject<ProductQuery>({
    page: 0,
    size: 20,
    sort: {column: "name", direction: "ascending"},
    query: "",
  });

  readonly productsPerPageOptions = [2, 5, 10, 20];
  readonly sortableColumns: ReadonlyArray<{ key: AdminProductSortColumn, label: string }> = [
    {key: "name", label: "Name"},
    {key: "ean", label: "EAN"},
    {key: "stock", label: "Stock"},
    {key: "category", label: "Category name"},
    {key: "price", label: "Price"},
  ];
  readonly currentPage = signal(0);
  readonly productsPerPage = signal(20);
  readonly searchControl = new FormControl("", {nonNullable: true});

  readonly state$: Observable<ProductsState> = this.query$.pipe(
    switchMap(({page, size, sort, query}) =>
      this.productService.findProducts(
        page,
        size,
        sort.column,
        this.apiDirection(sort.direction),
        query,
      ).pipe(
        map((response): ProductsState => {
          if (response.status !== 200) {
            return {
              status: "error" as const,
              message: response.message
            }
          }

          return {
            status: "loaded" as const,
            data: response.data
          }
        }),
        startWith({status: "loading" as const}),
      )
    ),
    shareReplay({
      bufferSize: 1,
      refCount: true,
    }),
  )

  sortBy(column: AdminProductSortColumn): void {
    const currentQuery = this.query$.value;
    const currentSort = currentQuery.sort;
    const direction: ProductSortDirection =
      currentSort.column === column && currentSort.direction === "ascending"
        ? "descending"
        : "ascending";

    this.currentPage.set(0);
    this.query$.next({
      ...currentQuery,
      page: 0,
      sort: {column, direction},
    });
  }

  searchProducts(): void {
    this.currentPage.set(0);
    this.query$.next({
      ...this.query$.value,
      page: 0,
      query: this.searchControl.value.trim(),
    });
  }

  ariaSort(column: AdminProductSortColumn): ProductSortDirection | "none" {
    const currentSort = this.query$.value.sort;
    return currentSort.column === column ? currentSort.direction : "none";
  }

  sortIndicator(column: AdminProductSortColumn): string {
    const direction = this.ariaSort(column);

    if (direction === "ascending") {
      return "↑";
    }

    if (direction === "descending") {
      return "↓";
    }

    return "↕";
  }

  changeProductsPerPage(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);

    if (!this.productsPerPageOptions.includes(size)) {
      return;
    }

    this.productsPerPage.set(size);
    this.loadPage(0);
  }

  goToPreviousPage(): void {
    this.loadPage(Math.max(0, this.currentPage() - 1));
  }

  goToNextPage(): void {
    this.loadPage(this.currentPage() + 1);
  }

  private loadPage(page: number): void {
    this.currentPage.set(page);
    this.query$.next({
      ...this.query$.value,
      page,
      size: this.productsPerPage(),
    });
  }

  private apiDirection(direction: ProductSortDirection): AdminProductSortDirection {
    return direction === "ascending" ? "asc" : "desc";
  }
}
