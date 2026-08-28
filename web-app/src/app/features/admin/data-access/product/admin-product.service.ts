import {inject, Injectable} from '@angular/core';
import AdminProduct from "./admin-product.model";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {API_ROUTES} from "../../../../core/api/api.routes";
import {ApiResponse} from "../../../../core/api/api-response.model";

export type AdminProductSortColumn = "name" | "ean" | "stock" | "category" | "price";
export type AdminProductSortDirection = "asc" | "desc";

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
}
