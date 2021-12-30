package com.github.wolray.line.io;

/**
 * @author ray
 */
public class Columns {
    public final int[] slots;

    public Columns(int... slots) {
        this.slots = slots;
    }

    public Columns(String excelCols) {
        if (excelCols == null || excelCols.isEmpty()) {
            slots = new int[0];
        } else {
            String[] split = excelCols.split(",");
            slots = new int[split.length];
            char a = 'A';
            for (int i = 0; i < split.length; i++) {
                String col = split[i].trim();
                int j = col.charAt(0) - a;
                if (col.length() > 1) {
                    slots[i] = (j + 1) * 26 + col.charAt(1) - a;
                } else {
                    slots[i] = j;
                }
            }
        }
    }
}
