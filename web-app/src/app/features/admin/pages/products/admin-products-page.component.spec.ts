import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {AdminProductsPageComponent} from './admin-products-page.component';
import {
  AdminProductService,
  AdminProductSortColumn,
} from "../../data-access/product/admin-product.service";
import AdminProduct, {AdminProductCategory} from "../../data-access/product/admin-product.model";
import {of, throwError} from "rxjs";
import {provideRouter} from "@angular/router";

describe('AdminProductsPageComponent', () => {
  let categories: AdminProductCategory[];
  let products: AdminProduct[];

  let component: AdminProductsPageComponent;
  let fixture: ComponentFixture<AdminProductsPageComponent>;
  let productsService: jasmine.SpyObj<AdminProductService>;

  beforeEach(async () => {
    categories = getInitCategories();
    products = getInitProducts();
    productsService = jasmine.createSpyObj<AdminProductService>("AdminProductService", [
      "findProducts",
      "findCategories",
      "createProduct",
    ])

    productsService.findProducts.and.callFake((
      page = 0,
      size = products.length,
      sortBy = "name",
      direction = "asc",
      query = "",
    ) => {
      const valueFor: Record<string, (product: AdminProduct) => string | number> = {
        name: product => product.productName,
        ean: product => product.ean,
        stock: product => product.stock,
        category: product => product.category.name,
        price: product => product.price,
      };
      const normalizedQuery = query.trim().toLocaleLowerCase();
      const filteredProducts = products.filter(product =>
        product.productName.toLocaleLowerCase().includes(normalizedQuery)
        || product.ean.toLocaleLowerCase().includes(normalizedQuery)
        || product.category.name.toLocaleLowerCase().includes(normalizedQuery)
      );
      const sortedProducts = [...filteredProducts].sort((first, second) => {
        const firstValue = valueFor[sortBy](first);
        const secondValue = valueFor[sortBy](second);
        const comparison = firstValue < secondValue ? -1 : firstValue > secondValue ? 1 : 0;
        return direction === "asc" ? comparison : -comparison;
      });

      return of({
        status: 200,
        message: "",
        data: sortedProducts.slice(page * size, (page + 1) * size)
      });
    })
    productsService.findCategories.and.returnValue(of(categories));
    productsService.createProduct.and.callFake(request => {
      const createdProduct: AdminProduct = {
        id: "created-product",
        productName: request.productName,
        ean: "9000000000001",
        stock: request.stock,
        price: request.price,
        category: categories.find(category => category.id === request.categoryId)!,
      };
      products.push(createdProduct);
      return of(createdProduct);
    });

    await TestBed.configureTestingModule({
      imports: [AdminProductsPageComponent],
      providers: [
        {provide: AdminProductService, useValue: productsService},
        provideRouter([]),
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminProductsPageComponent,);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should display all products', () => {
    expect(component).toBeTruthy();

    const renderedProducts = getRenderedProducts()

    expect(productsService.findProducts).toHaveBeenCalledTimes(1)
    expect(renderedProducts.length).toBe(products.length)

    for (let i = 0; i < products.length; i++) {
      const product = products[i];
      const row = renderedProducts.find(row => row.includes(product.ean))


      expect(row).toBeTruthy()
      expect(row).toContain(product.ean)
      expect(row).toContain(product.productName)
      expect(row).toContain(product.price.toFixed(2))
      expect(row).toContain(product.category.name)
      expect(row).toContain(product.stock)
    }
  });

  it('paginates the products list using the selected number of products per page', () => {
    const productsPerPage = getElement<HTMLSelectElement>('products-per-page');

    productsPerPage.value = '2';
    productsPerPage.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(productsService.findProducts).toHaveBeenCalledWith(0, 2, "name", "asc", "");
    expect(getRenderedProducts()).toEqual([
      'Kebab|0000000000004|0|Food|$330.00',
      'Keyboard|0000000000002|0|Electronics|$130.00',
    ]);

    getElement<HTMLButtonElement>('products-next-page').click();
    fixture.detectChanges();

    expect(productsService.findProducts).toHaveBeenCalledWith(1, 2, "name", "asc", "");
    expect(getRenderedProducts()).toEqual([
      'Phone|0000000000001|100|Electronics|$30.00',
      'Pizza|0000000000003|200|Food|$230.00',
    ]);
  });

  it('requests server sorting in both directions for every visible column', () => {
    const visibleColumns: Array<{
      column: AdminProductSortColumn,
      ascending: string[],
      descending: string[],
    }> = [
      {
        column: "ean",
        ascending: ["Phone", "Keyboard", "Pizza", "Kebab"],
        descending: ["Kebab", "Pizza", "Keyboard", "Phone"],
      },
      {
        column: "stock",
        ascending: ["Keyboard", "Kebab", "Phone", "Pizza"],
        descending: ["Pizza", "Phone", "Keyboard", "Kebab"],
      },
      {
        column: "category",
        ascending: ["Phone", "Keyboard", "Pizza", "Kebab"],
        descending: ["Pizza", "Kebab", "Phone", "Keyboard"],
      },
      {
        column: "price",
        ascending: ["Phone", "Keyboard", "Pizza", "Kebab"],
        descending: ["Kebab", "Pizza", "Keyboard", "Phone"],
      },
      {
        column: "name",
        ascending: ["Kebab", "Keyboard", "Phone", "Pizza"],
        descending: ["Pizza", "Phone", "Keyboard", "Kebab"],
      },
    ];

    for (const expectation of visibleColumns) {
      const sortButton = getElement<HTMLButtonElement>(`products-sort-${expectation.column}`);

      sortButton.click();
      fixture.detectChanges();
      expect(productsService.findProducts).toHaveBeenCalledWith(0, 20, expectation.column, "asc", "");
      expect(sortButton.closest('th')?.getAttribute('aria-sort')).toBe('ascending');
      expect(getRenderedProductNames())
        .withContext(`${expectation.column} ascending`)
        .toEqual(expectation.ascending);

      sortButton.click();
      fixture.detectChanges();
      expect(productsService.findProducts).toHaveBeenCalledWith(0, 20, expectation.column, "desc", "");
      expect(sortButton.closest('th')?.getAttribute('aria-sort')).toBe('descending');
      expect(getRenderedProductNames())
        .withContext(`${expectation.column} descending`)
        .toEqual(expectation.descending);
    }
  });

  it('searches by name, EAN, or category using one input', () => {
    expectSearchResults("phone", ["Phone"]);
    expectSearchResults("0000000000002", ["Keyboard"]);
    expectSearchResults("food", ["Kebab", "Pizza"]);
  });

  it('displays an empty state when no products exist', () => {
    products = [];

    component.searchProducts();
    fixture.detectChanges();

    expect(getElement<HTMLElement>('products-empty-state').innerText.trim())
      .toBe('No products found.');
    expect(fixture.nativeElement.querySelector('.admin-table')).toBeNull();
  });

  it('allows selecting an existing product on its separate detail page', () => {
    const link = getElement<HTMLAnchorElement>('product-detail-link-1');

    expect(link.getAttribute('href')).toBe('/admin/products/1');
    expect(link.innerText.trim()).toBe('View / edit');
  });

  it('shows a Create Product action and all product fields', () => {
    getElement<HTMLButtonElement>('create-product-button').click();
    fixture.detectChanges();

    expect(getElement<HTMLInputElement>('product-form-name')).toBeTruthy();
    expect(getElement<HTMLTextAreaElement>('product-form-description')).toBeTruthy();
    expect(getElement<HTMLInputElement>('product-form-price')).toBeTruthy();
    expect(getElement<HTMLInputElement>('product-form-images')).toBeTruthy();
    expect(getElement<HTMLSelectElement>('product-form-category')).toBeTruthy();
    expect(getElement<HTMLInputElement>('product-form-stock')).toBeTruthy();
    expect(getElement<HTMLSelectElement>('product-form-status')).toBeTruthy();
  });

  it('shows validation errors and does not submit invalid product data', () => {
    getElement<HTMLButtonElement>('create-product-button').click();
    getElement<HTMLButtonElement>('product-form-submit').click();
    fixture.detectChanges();

    expect(getElement<HTMLElement>('product-form-name-error').innerText.trim())
      .toBe('Product name is required.');
    expect(getElement<HTMLElement>('product-form-price-error').innerText.trim())
      .toBe('Price is required.');
    expect(getElement<HTMLElement>('product-form-category-error').innerText.trim())
      .toBe('Category is required.');

    setFormValue('product-form-name', 'Invalid Product');
    setFormValue('product-form-price', '0');
    setFormValue('product-form-stock', '-1');
    fixture.detectChanges();

    expect(getElement<HTMLElement>('product-form-price-error').innerText.trim())
      .toBe('Price must be greater than 0.');
    expect(getElement<HTMLElement>('product-form-stock-error').innerText.trim())
      .toBe('Stock cannot be negative.');
    expect(productsService.createProduct).not.toHaveBeenCalled();
  });

  it('creates a complete product, refreshes the list, and shows success', fakeAsync(() => {
    getElement<HTMLButtonElement>('create-product-button').click();
    const image = new File(["image-content"], "product.png", {type: "image/png"});
    fillValidProductForm(image);

    getElement<HTMLButtonElement>('product-form-submit').click();
    tick();
    fixture.detectChanges();

    const request = productsService.createProduct.calls.mostRecent().args[0];
    expect(request.productName).toBe('Z Product');
    expect(request.description).toBe('Product description');
    expect(request.price).toBe(99.99);
    expect(request.categoryId).toBe(categories[1].id);
    expect(request.stock).toBe(12);
    expect(request.status).toBe('ACTIVE');
    expect(request.images).toEqual([image]);
    expect(productsService.findProducts).toHaveBeenCalledTimes(2);
    expect(getRenderedProductNames()).toContain('Z Product');
    expect(getElement<HTMLElement>('product-create-success').innerText.trim())
      .toBe('Product created successfully.');
  }));

  it('shows the duplicate-name error returned by the backend', fakeAsync(() => {
    productsService.createProduct.and.returnValue(throwError(() => ({
      status: 409,
      error: {message: 'A product with this name already exists'},
    })));
    getElement<HTMLButtonElement>('create-product-button').click();
    fillValidProductForm();

    getElement<HTMLButtonElement>('product-form-submit').click();
    tick();
    fixture.detectChanges();

    expect(getElement<HTMLElement>('product-form-server-error').innerText.trim())
      .toBe('A product with this name already exists');
  }));

  function getRenderedProducts(): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll("[data-testid='product-list-item']") as NodeListOf<HTMLTableRowElement>)
      .map(row =>
        Array.from(row.querySelectorAll("td") as NodeListOf<HTMLElement>)
          .filter(td => !td.querySelector("button, a"))
          .map(cell => cell.innerText.trim())
          .join("|")
      )
  }

  function getRenderedProductNames(): string[] {
    return Array.from(
      fixture.nativeElement.querySelectorAll("[data-testid='product-list-item'] td:first-child") as NodeListOf<HTMLElement>
    ).map(cell => cell.innerText.trim());
  }

  function getElement<T extends Element>(dataTestId: string): T {
    const element = fixture.nativeElement.querySelector(`[data-testid="${dataTestId}"]`) as T | null;

    if (!element) {
      throw new Error(`Expected element with [data-testid="${dataTestId}"]`);
    }

    return element;
  }

  function expectSearchResults(query: string, expectedNames: string[]): void {
    const searchInput = getElement<HTMLInputElement>('products-search');
    searchInput.value = query;
    searchInput.dispatchEvent(new Event('input', {bubbles: true}));
    fixture.detectChanges();
    getElement<HTMLButtonElement>('products-search-submit').click();
    fixture.detectChanges();

    expect(productsService.findProducts).toHaveBeenCalledWith(0, 20, "name", "asc", query);
    expect(getRenderedProductNames()).toEqual(expectedNames);
  }

  function setFormValue(testId: string, value: string): void {
    const control = getElement<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(testId);
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
  }

  function fillValidProductForm(image?: File): void {
    setFormValue('product-form-name', 'Z Product');
    setFormValue('product-form-description', 'Product description');
    setFormValue('product-form-price', '99.99');
    setFormValue('product-form-category', categories[1].id);
    setFormValue('product-form-stock', '12');
    setFormValue('product-form-status', 'ACTIVE');

    if (image) {
      const imageInput = getElement<HTMLInputElement>('product-form-images');
      const files = new DataTransfer();
      files.items.add(image);
      Object.defineProperty(imageInput, 'files', {value: files.files});
      imageInput.dispatchEvent(new Event('change'));
    }
    fixture.detectChanges();
  }

  function getInitProducts(): AdminProduct[] {
    return [
      {
        id: "1",
        productName: "Phone",
        ean: "0000000000001",
        stock: 100,
        price: 30.0,
        category: categories[0]
      },
      {
        id: "2",
        productName: "Keyboard",
        ean: "0000000000002",
        stock: 0,
        price: 130.0,
        category: categories[0]
      },
      {
        id: "3",
        productName: "Pizza",
        ean: "0000000000003",
        stock: 200,
        price: 230.0,
        category: categories[1]
      },
      {
        id: "4",
        productName: "Kebab",
        ean: "0000000000004",
        stock: 0,
        price: 330.0,
        category: categories[1]
      },
    ]
  }

  function getInitCategories(): AdminProductCategory[] {
    return [
      {
        id: "1",
        name: "Electronics",
      },
      {
        id: "2",
        name: "Food",
      }
    ];
  }
});
