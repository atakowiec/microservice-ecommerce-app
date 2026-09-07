import {Component, inject, signal} from '@angular/core';
import {
  AdminProductService,
  AdminProductSortColumn,
  AdminProductSortDirection,
  CreateAdminProduct,
} from "../../data-access/product/admin-product.service";
import {AsyncPipe} from "@angular/common";
import AdminProduct, {AdminProductStatus} from "../../data-access/product/admin-product.model";
import {BehaviorSubject, finalize, map, Observable, shareReplay, startWith, switchMap} from "rxjs";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ModalComponent} from "../../../../shared/components/modal/modal.component";
import {RouterLink} from "@angular/router";

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
    ModalComponent,
    RouterLink,
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
  readonly productStatuses: readonly AdminProductStatus[] = ["ACTIVE", "DRAFT", "ARCHIVED"];
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
  readonly createProductModalOpen = signal(false);
  readonly createProductSubmitting = signal(false);
  readonly createProductServerError = signal<string | null>(null);
  readonly createProductSuccess = signal<string | null>(null);
  readonly createProductForm = new FormGroup({
    productName: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required],
    }),
    description: new FormControl("", {nonNullable: true}),
    price: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    categoryId: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required],
    }),
    stock: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0)],
    }),
    status: new FormControl<AdminProductStatus>("DRAFT", {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  readonly categories$ = this.productService.findCategories().pipe(
    shareReplay({bufferSize: 1, refCount: true}),
  );
  private selectedImages: File[] = [];

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

  openCreateProduct(): void {
    this.resetCreateProductForm();
    this.createProductSuccess.set(null);
    this.createProductModalOpen.set(true);
  }

  selectProductImages(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedImages = input.files ? Array.from(input.files) : [];
  }

  submitCreateProduct(): void {
    this.createProductServerError.set(null);
    this.createProductForm.markAllAsTouched();

    if (this.createProductForm.invalid || this.createProductSubmitting()) {
      return;
    }

    const formValue = this.createProductForm.getRawValue();
    const request: CreateAdminProduct = {
      productName: formValue.productName.trim(),
      description: formValue.description.trim(),
      price: formValue.price!,
      categoryId: formValue.categoryId,
      stock: formValue.stock,
      status: formValue.status,
      images: this.selectedImages,
    };

    this.createProductSubmitting.set(true);
    this.productService.createProduct(request).pipe(
      finalize(() => this.createProductSubmitting.set(false)),
    ).subscribe({
      next: () => {
        this.createProductModalOpen.set(false);
        this.createProductSuccess.set("Product created successfully.");
        this.resetCreateProductForm();
        this.query$.next({...this.query$.value});
      },
      error: error => {
        if (error.status === 409) {
          this.createProductServerError.set(
            error.error?.message ?? "A product with this name already exists",
          );
          return;
        }

        this.createProductServerError.set("The product could not be created. Please try again.");
      },
    });
  }

  resetCreateProductForm(): void {
    this.createProductForm.reset({
      productName: "",
      description: "",
      price: null,
      categoryId: "",
      stock: 0,
      status: "DRAFT",
    });
    this.selectedImages = [];
    this.createProductServerError.set(null);
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
