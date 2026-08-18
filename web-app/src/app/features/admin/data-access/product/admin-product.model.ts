export default interface AdminProduct {
  id: string,
  productName: string,
  ean: string,
  stock: number,
  category: AdminProductCategory,
  price: number
}

export interface AdminProductCategory {
  id: string,
  name: string
}