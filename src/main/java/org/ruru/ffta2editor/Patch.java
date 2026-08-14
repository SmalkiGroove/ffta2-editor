package org.ruru.ffta2editor;

import java.util.ArrayList;
import java.util.List;

public class Patch {
    private String title;
    private String description;
    private List<PatchElement> arm9Patches = new ArrayList<>();
    private List<PatchElement> overlay8Patches = new ArrayList<>();
    private List<PatchElement> overlay11Patches = new ArrayList<>();

    public Patch() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PatchElement> getArm9Patches() {
        return arm9Patches;
    }

    public void setArm9Patches(List<PatchElement> arm9Patches) {
        this.arm9Patches = arm9Patches;
    }

    public List<PatchElement> getOverlay8Patches() {
        return overlay8Patches;
    }

    public void setOverlay8Patches(List<PatchElement> overlay8Patches) {
        this.overlay8Patches = overlay8Patches;
    }

    public List<PatchElement> getOverlay11Patches() {
        return overlay11Patches;
    }

    public void setOverlay11Patches(List<PatchElement> overlay11Patches) {
        this.overlay11Patches = overlay11Patches;
    }

    @Override
    public String toString() {
        return title != null && !title.isBlank() ? title : "Untitled patch";
    }

    public static class PatchElement {
        // Package-private so PatchesController can use fields, while SnakeYAML
        // uses the Object setters (public fields would be bound as int and reject hex).
        int address;
        int originalBytes;
        int modifiedBytes;

        public PatchElement() {
        }

        public PatchElement(int address, int originalBytes, int modifiedBytes) {
            this.address = address;
            this.originalBytes = originalBytes;
            this.modifiedBytes = modifiedBytes;
        }

        // SnakeYAML setters: accept hex strings or numeric values, store as int32 words.
        // Getters intentionally omitted so SnakeYAML does not treat these as int properties
        // (hex like 0xe59f1058 does not fit in Integer during YAML construction).
        public void setAddress(Object address) {
            this.address = parseWord(address);
        }

        public void setOriginalBytes(Object originalBytes) {
            this.originalBytes = parseWord(originalBytes);
        }

        public void setModifiedBytes(Object modifiedBytes) {
            this.modifiedBytes = parseWord(modifiedBytes);
        }

        private static int parseWord(Object value) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text) {
                return (int) Long.decode(text.trim());
            }
            throw new IllegalArgumentException("Expected hex string or number, got: " + value);
        }
    }
}
