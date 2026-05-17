export interface Stock {
  productId: string;
  productName: string;
  available: number;
  reserved: number;
  actualAvailable: number;
  lastUpdated: string;
}
