export default interface AdminProduct {
  id: string,
  productName: string,
  description?: string,
  ean: string,
  stock: number,
  category: AdminProductCategory,
  price: number,
  status?: AdminProductStatus,
  images?: AdminProductImage[],
}

export interface AdminProductCategory {
  id: string,
  name: string
}

export type AdminProductStatus = "ACTIVE" | "DRAFT" | "ARCHIVED";

export interface AdminProductImage {
  id: string,
  fileName: string,
  contentType: string,
}
