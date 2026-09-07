import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, throwError} from 'rxjs';

import AdminProduct, {
  AdminProductCategory,
} from '../../data-access/product/admin-product.model';
import {AdminProductService} from '../../data-access/product/admin-product.service';
import {AdminProductDetailPageComponent} from './admin-product-detail-page.component';

describe('AdminProductDetailPageComponent', () => {
  let component: AdminProductDetailPageComponent;
  let fixture: ComponentFixture<AdminProductDetailPageComponent>;
  let productService: jasmine.SpyObj<AdminProductService>;
  let product: AdminProduct;
  let categories: AdminProductCategory[];

  beforeEach(async () => {
    categories = [
      {id: 'category-1', name: 'Electronics'},
      {id: 'category-2', name: 'Home'},
    ];
    product = productFixture();
    productService = jasmine.createSpyObj<AdminProductService>('AdminProductService', [
      'findProduct',
      'findProductImage',
      'findCategories',
      'updateProduct',
      'deleteProduct'
    ]);
    productService.findProduct.and.returnValue(of(product));
    productService.findProductImage.and.returnValue(
      of(new Blob(['actual-image'], {type: 'image/png'})),
    );
    productService.findCategories.and.returnValue(of(categories));
    productService.updateProduct.and.callFake((_productId, update) => of({
      ...product,
      productName: update.productName,
      description: update.description,
      price: update.price,
      stock: update.stock,
      status: update.status,
      category: categories.find(category => category.id === update.categoryId)!,
      images: update.images.length > 0
        ? update.images.map((image, index) => ({
          id: `products/replacement-${index}`,
          fileName: image.name,
          contentType: image.type,
        }))
        : product.images,
    }));

    await TestBed.configureTestingModule({
      imports: [AdminProductDetailPageComponent],
      providers: [
        {provide: AdminProductService, useValue: productService},
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {paramMap: convertToParamMap({productId: product.id})},
          },
        },
      ],
    }).compileComponents();

    createComponent();
  });

  it("Clicking the remove button displays a confirmation dialog before deletion.", fakeAsync(() => {
    getElement<HTMLButtonElement>("delete-button").click()

    fixture.detectChanges()

    const dialog = getElement<HTMLDialogElement>('delete-product--modal');

    expect(dialog.open).toBeTrue();
    expect(dialog.textContent).toContain(product.productName);
  }))

  it("Admin can cancel the removal from the confirmation dialog without any changes being made.", () => {
    getElement<HTMLButtonElement>("delete-button").click()
    fixture.detectChanges()

    getElement<HTMLButtonElement>("delete-product--cancel").click()

    expect(productService.deleteProduct).toHaveBeenCalledTimes(0);
  })

  it("After delete confirmation product should be deleted", () => {
    getElement<HTMLButtonElement>("delete-button").click()
    fixture.detectChanges()

    getElement<HTMLButtonElement>("delete-product--confirm").click()

    expect(productService.deleteProduct).toHaveBeenCalledTimes(1);

    expect(TestBed.inject(Router).url).toBe('/admin/products');
  })

  it('shows the complete product on a separate detail page', () => {
    expect(productService.findProduct).toHaveBeenCalledOnceWith(product.id);
    expect(getText('product-detail-name')).toBe(product.productName);
    expect(getText('product-detail-description')).toBe(product.description ?? '');
    expect(getText('product-detail-ean')).toBe(product.ean);
    expect(getText('product-detail-price')).toBe('$49.99');
    expect(getText('product-detail-category')).toBe(product.category.name);
    expect(getText('product-detail-stock')).toBe('8');
    expect(getText('product-detail-status')).toBe('ACTIVE');
    const image = getElement<HTMLImageElement>('product-detail-image');
    expect(productService.findProductImage).toHaveBeenCalledWith(product.id, 0);
    expect(image.getAttribute('src')).toContain('blob:');
    expect(image.alt).toBe('current.png');
    expect(getElement<HTMLAnchorElement>('product-detail-back').getAttribute('href'))
      .toBe('/admin/products');
  });

  it('populates edit mode and immediately displays every saved change', fakeAsync(() => {
    getElement<HTMLButtonElement>('product-edit-button').click();
    fixture.detectChanges();

    expect(getElement<HTMLInputElement>('product-update-name').value).toBe(product.productName);
    expect(getElement<HTMLTextAreaElement>('product-update-description').value).toBe(product.description ?? '');
    expect(getElement<HTMLInputElement>('product-update-price').value).toBe('49.99');
    expect(getElement<HTMLSelectElement>('product-update-category').value).toBe('category-1');
    expect(getElement<HTMLInputElement>('product-update-stock').value).toBe('8');
    expect(getElement<HTMLSelectElement>('product-update-status').value).toBe('ACTIVE');

    setFormValue('product-update-name', 'Updated Product');
    setFormValue('product-update-description', 'Updated description');
    setFormValue('product-update-price', '79.99');
    setFormValue('product-update-category', 'category-2');
    setFormValue('product-update-stock', '17');
    setFormValue('product-update-status', 'ARCHIVED');
    const image = selectImage('replacement.webp', 'image/webp');

    getElement<HTMLButtonElement>('product-update-submit').click();
    tick();
    fixture.detectChanges();

    expect(productService.updateProduct).toHaveBeenCalledOnceWith(product.id, {
      productName: 'Updated Product',
      description: 'Updated description',
      price: 79.99,
      categoryId: 'category-2',
      stock: 17,
      status: 'ARCHIVED',
      images: [image],
    });
    expect(getText('product-update-success')).toBe('Product updated successfully.');
    expect(getText('product-detail-name')).toBe('Updated Product');
    expect(getText('product-detail-description')).toBe('Updated description');
    expect(getText('product-detail-price')).toBe('$79.99');
    expect(getText('product-detail-category')).toBe('Home');
    expect(getText('product-detail-stock')).toBe('17');
    expect(getText('product-detail-status')).toBe('ARCHIVED');
    expect(getElement<HTMLImageElement>('product-detail-image').alt).toBe('replacement.webp');
    expect(fixture.nativeElement.querySelector('[data-testid="product-update-name"]')).toBeNull();
  }));

  it('validates required, positive, and whole-number fields before saving', () => {
    getElement<HTMLButtonElement>('product-edit-button').click();
    fixture.detectChanges();
    setFormValue('product-update-name', ' ');
    setFormValue('product-update-price', '0');
    setFormValue('product-update-category', '');
    setFormValue('product-update-stock', '-1');
    getElement<HTMLButtonElement>('product-update-submit').click();
    fixture.detectChanges();

    expect(getText('product-update-name-error')).toBe('Product name is required.');
    expect(getText('product-update-price-error')).toBe('Price must be greater than 0.');
    expect(getText('product-update-category-error')).toBe('Category is required.');
    expect(getText('product-update-stock-error')).toBe('Stock cannot be negative.');
    expect(productService.updateProduct).not.toHaveBeenCalled();

    setFormValue('product-update-stock', '1.5');
    fixture.detectChanges();
    expect(getText('product-update-stock-error')).toBe('Stock must be a whole number.');
  });

  it('cancels editing without saving and restores the persisted values', () => {
    getElement<HTMLButtonElement>('product-edit-button').click();
    fixture.detectChanges();
    setFormValue('product-update-name', 'Unsaved name');
    selectImage('unsaved.png', 'image/png');

    getElement<HTMLButtonElement>('product-update-cancel').click();
    fixture.detectChanges();

    expect(productService.updateProduct).not.toHaveBeenCalled();
    expect(getText('product-detail-name')).toBe(product.productName);
    getElement<HTMLButtonElement>('product-edit-button').click();
    fixture.detectChanges();
    expect(getElement<HTMLInputElement>('product-update-name').value).toBe(product.productName);
  });

  it('shows a duplicate-name conflict without leaving edit mode', fakeAsync(() => {
    productService.updateProduct.and.returnValue(throwError(() => ({
      status: 409,
      error: {message: 'A product with this name already exists'},
    })));
    getElement<HTMLButtonElement>('product-edit-button').click();
    fixture.detectChanges();

    getElement<HTMLButtonElement>('product-update-submit').click();
    tick();
    fixture.detectChanges();

    expect(getText('product-update-server-error'))
      .toBe('A product with this name already exists');
    expect(getElement<HTMLInputElement>('product-update-name')).toBeTruthy();
  }));

  it('shows an appropriate state when the product no longer exists', () => {
    fixture.destroy();
    productService.findProduct.and.returnValue(throwError(() => ({status: 404})));
    createComponent();

    expect(getText('product-detail-not-found')).toBe('Product not found.');
    expect(fixture.nativeElement.querySelector('[data-testid="product-edit-button"]')).toBeNull();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(AdminProductDetailPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function setFormValue(testId: string, value: string): void {
    const control = getElement<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(testId);
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
  }

  function selectImage(fileName: string, contentType: string): File {
    const image = new File(['image'], fileName, {type: contentType});
    const input = getElement<HTMLInputElement>('product-update-image');
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(image);
    Object.defineProperty(input, 'files', {value: dataTransfer.files});
    input.dispatchEvent(new Event('change'));
    return image;
  }

  function getText(testId: string): string {
    return getElement<HTMLElement>(testId).innerText.trim();
  }

  function getElement<T extends Element>(testId: string): T {
    const element = fixture.nativeElement.querySelector(`[data-testid="${testId}"]`) as T | null;
    if (!element) {
      throw new Error(`Expected element with [data-testid="${testId}"]`);
    }
    return element;
  }

  function productFixture(): AdminProduct {
    return {
      id: 'product-1',
      productName: 'Current Product',
      description: 'Current description',
      ean: '9000000000001',
      price: 49.99,
      stock: 8,
      status: 'ACTIVE',
      category: categories[0],
      images: [{
        id: 'products/current.png',
        fileName: 'current.png',
        contentType: 'image/png',
      }],
    };
  }
});
