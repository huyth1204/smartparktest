package com.smartpark.exception;

/**
 * Custom Exception: ô đỗ đang trống, không thể check-out.
 */
public class SlotNotOccupiedException extends RuntimeException {

    private final String slotId;

    public SlotNotOccupiedException(String slotId) {
        super("Ô đỗ " + slotId + " đang trống");
        this.slotId = slotId;
    }

    public String getSlotId() { return slotId; }
}
