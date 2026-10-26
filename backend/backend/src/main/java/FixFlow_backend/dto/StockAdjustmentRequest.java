package FixFlow_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockAdjustmentRequest {
    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @Size(max = 255)
    private String note;

    public Integer getQuantityChange() { return quantityChange; }
    public String getNote() { return note; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public void setNote(String note) { this.note = note; }
}
