package com.smartpark.exception;

/**
 * Custom Exception: ô đỗ đã có xe, không thể check-in thêm.
 */
public class SlotAlreadyOccupiedException extends RuntimeException {

    private final String slotId;

    public SlotAlreadyOccupiedException(String slotId) {
        super("Ô đỗ " + slotId + " đã có xe");
        this.slotId = slotId;
    }

    public String getSlotId() { return slotId; }
}
