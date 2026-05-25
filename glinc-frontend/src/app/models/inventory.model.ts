export type InventoryItemType =
  | 'SENSORS'
  | 'INSULIN_FAST'
  | 'INSULIN_SLOW'
  | 'GLUCAGON';

export type InventoryStatus = 'OK' | 'WARN' | 'DANGER';

export interface InventoryItem {
  type: InventoryItemType;
  quantity: string | null;
  status: InventoryStatus;
  updatedAt: string | null;
}

export interface UpdateInventoryRequest {
  quantity: string | null;
  status: InventoryStatus;
}

export function inventoryLabel(t: InventoryItemType): string {
  switch (t) {
    case 'SENSORS':
      return 'Sensores';
    case 'INSULIN_FAST':
      return 'Insulina rápida';
    case 'INSULIN_SLOW':
      return 'Insulina lenta';
    case 'GLUCAGON':
      return 'Glucagón';
  }
}
