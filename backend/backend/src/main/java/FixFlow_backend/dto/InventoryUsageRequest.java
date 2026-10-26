package FixFlow_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InventoryUsageRequest {
    @NotNull(message = "Inventory item is required")
    private Long itemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Size(max = 255)
    private String note;

    public Long getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setNote(String note) { this.note = note; }
}
