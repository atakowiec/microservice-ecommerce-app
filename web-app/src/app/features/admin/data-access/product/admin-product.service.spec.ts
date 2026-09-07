import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {firstValueFrom} from 'rxjs';

import {ApiResponse} from '../../../../core/api/api-response.model';
import {API_ROUTES} from '../../../../core/api/api.routes';
import AdminProduct, {AdminProductCategory} from './admin-product.model';
import {AdminProductService, CreateAdminProduct, UpdateAdminProduct} from './admin-product.service';

describe('AdminProductService', () => {
  let service: AdminProductService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminProductService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AdminProductService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads categories for product assignment', async () => {
    const categories: AdminProductCategory[] = [{id: 'category-1', name: 'Category'}];
    const categoriesPromise = firstValueFrom(service.findCategories());
    const request = http.expectOne({method: 'GET', url: API_ROUTES.admin.categories});
    const response: ApiResponse<AdminProductCategory[]> = {
      status: 200,
      message: 'Categories retrieved successfully',
      data: categories,
    };
    request.flush(response);

    expect(await categoriesPromise).toEqual(categories);
  });

  it('sends product data and images as multipart form data', async () => {
    const image = new File(['image'], 'product.png', {type: 'image/png'});
    const product: CreateAdminProduct = {
      productName: 'Product',
      description: 'Description',
      price: 10,
      categoryId: 'category-1',
      stock: 5,
      status: 'DRAFT',
      images: [image],
    };
    const createdProduct = {
      id: 'product-1',
      productName: product.productName,
      ean: '9000000000001',
      price: product.price,
      stock: product.stock,
      category: {id: 'category-1', name: 'Category'},
    } satisfies AdminProduct;

    const createdPromise = firstValueFrom(service.createProduct(product));
    const request = http.expectOne({method: 'POST', url: API_ROUTES.admin.products});
    const formData = request.request.body as FormData;
    const productPart = formData.get('product') as Blob;

    expect(JSON.parse(await productPart.text())).toEqual({
      productName: product.productName,
      description: product.description,
      price: product.price,
      categoryId: product.categoryId,
      stock: product.stock,
      status: product.status,
    });
    expect(formData.getAll('images')).toEqual([image]);

    request.flush({
      status: 201,
      message: 'Product created successfully',
      data: createdProduct,
    } satisfies ApiResponse<AdminProduct>);
    expect(await createdPromise).toEqual(createdProduct);
  });

  it('loads one product for the separate detail page', async () => {
    const product = productFixture();
    const productPromise = firstValueFrom(service.findProduct(product.id));
    const request = http.expectOne({
      method: 'GET',
      url: `${API_ROUTES.admin.products}/${product.id}`,
    });

    request.flush({
      status: 200,
      message: 'Product retrieved successfully',
      data: product,
    } satisfies ApiResponse<AdminProduct>);

    expect(await productPromise).toEqual(product);
  });

  it('loads the actual product image as a blob', async () => {
    const image = new Blob(['actual-image'], {type: 'image/png'});
    const imagePromise = firstValueFrom(service.findProductImage('product-1', 0));
    const request = http.expectOne({
      method: 'GET',
      url: `${API_ROUTES.admin.products}/product-1/images/0`,
    });

    expect(request.request.responseType).toBe('blob');
    request.flush(image);

    const response = await imagePromise;
    expect(response.type).toBe('image/png');
    expect(await response.text()).toBe('actual-image');
  });

  it('updates product data and a replacement image as multipart form data', async () => {
    const image = new File(['replacement'], 'replacement.webp', {type: 'image/webp'});
    const update: UpdateAdminProduct = {
      productName: 'Updated Product',
      description: 'Updated description',
      price: 22.5,
      categoryId: 'category-2',
      stock: 7,
      status: 'ARCHIVED',
      images: [image],
    };
    const updatedProduct: AdminProduct = {
      ...productFixture(),
      ...update,
      category: {id: update.categoryId, name: 'New Category'},
      images: [{id: 'products/replacement.webp', fileName: image.name, contentType: image.type}],
    };

    const updatedPromise = firstValueFrom(service.updateProduct('product-1', update));
    const request = http.expectOne({
      method: 'PUT',
      url: `${API_ROUTES.admin.products}/product-1`,
    });
    const formData = request.request.body as FormData;
    const productPart = formData.get('product') as Blob;

    expect(JSON.parse(await productPart.text())).toEqual({
      productName: update.productName,
      description: update.description,
      price: update.price,
      categoryId: update.categoryId,
      stock: update.stock,
      status: update.status,
    });
    expect(formData.getAll('images')).toEqual([image]);

    request.flush({
      status: 200,
      message: 'Product updated successfully',
      data: updatedProduct,
    } satisfies ApiResponse<AdminProduct>);
    expect(await updatedPromise).toEqual(updatedProduct);
  });

  function productFixture(): AdminProduct {
    return {
      id: 'product-1',
      productName: 'Product',
      description: 'Description',
      ean: '9000000000001',
      price: 10,
      stock: 5,
      status: 'ACTIVE',
      category: {id: 'category-1', name: 'Category'},
      images: [],
    };
  }
});
