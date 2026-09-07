import {inject, Injectable} from '@angular/core';
import AdminProduct, {AdminProductCategory, AdminProductStatus} from "./admin-product.model";
import {map, Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {API_ROUTES} from "../../../../core/api/api.routes";
import {ApiResponse} from "../../../../core/api/api-response.model";

export type AdminProductSortColumn = "name" | "ean" | "stock" | "category" | "price";
export type AdminProductSortDirection = "asc" | "desc";

export interface CreateAdminProduct {
  productName: string;
  description: string;
  price: number;
  categoryId: string;
  stock: number;
  status: AdminProductStatus;
  images: File[];
}

export type UpdateAdminProduct = CreateAdminProduct;

@Injectable({
  providedIn: 'root'
})
export class AdminProductService {
  readonly http = inject(HttpClient)

  findProducts(
    page = 0,
    size = 20,
    sortBy: AdminProductSortColumn = "name",
    direction: AdminProductSortDirection = "asc",
    query = "",
  ): Observable<ApiResponse<AdminProduct[]>> {
    return this.http.get<ApiResponse<AdminProduct[]>>(API_ROUTES.admin.products, {
      params: {page, size, sortBy, direction, query},
    });
  }

  findCategories(): Observable<AdminProductCategory[]> {
    return this.http.get<ApiResponse<AdminProductCategory[]>>(API_ROUTES.admin.categories).pipe(
      map(response => response.data),
    );
  }

  findProduct(productId: string): Observable<AdminProduct> {
    return this.http.get<ApiResponse<AdminProduct>>(
      `${API_ROUTES.admin.products}/${productId}`,
    ).pipe(map(response => response.data));
  }

  findProductImage(productId: string, imageIndex: number): Observable<Blob> {
    return this.http.get(
      `${API_ROUTES.admin.products}/${productId}/images/${imageIndex}`,
      {responseType: "blob"},
    );
  }

  createProduct(request: CreateAdminProduct): Observable<AdminProduct> {
    return this.http.post<ApiResponse<AdminProduct>>(
      API_ROUTES.admin.products,
      this.toFormData(request),
    ).pipe(
      map(response => response.data),
    );
  }

  updateProduct(productId: string, request: UpdateAdminProduct): Observable<AdminProduct> {
    return this.http.put<ApiResponse<AdminProduct>>(
      `${API_ROUTES.admin.products}/${productId}`,
      this.toFormData(request),
    ).pipe(
      map(response => response.data),
    );
  }

  private toFormData(request: CreateAdminProduct | UpdateAdminProduct): FormData {
    const {images, ...product} = request;
    const formData = new FormData();
    formData.append(
      "product",
      new Blob([JSON.stringify(product)], {type: "application/json"}),
    );
    images.forEach(image => formData.append("images", image));
    return formData;
  }
}
