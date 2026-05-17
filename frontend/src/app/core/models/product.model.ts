export type ProductCategory = 'ELECTRONICS' | 'CLOTHING' | 'FOOD' | 'HOME' | 'SPORTS' | 'OTHER';

export interface Product {
  id: string;
  name: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  category: ProductCategory;
  createdAt: string;
}

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  category: ProductCategory;
}
