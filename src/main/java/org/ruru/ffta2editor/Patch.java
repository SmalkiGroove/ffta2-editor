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
        public int address;
        public int originalBytes;
        public int modifiedBytes;

        public PatchElement() {
        }

        public PatchElement(int address, int originalBytes, int modifiedBytes) {
            this.address = address;
            this.originalBytes = originalBytes;
            this.modifiedBytes = modifiedBytes;
        }

        public int getAddress() {
            return address;
        }

        public void setAddress(int address) {
            this.address = address;
        }

        public int getOriginalBytes() {
            return originalBytes;
        }

        public void setOriginalBytes(int originalBytes) {
            this.originalBytes = originalBytes;
        }

        public int getModifiedBytes() {
            return modifiedBytes;
        }

        public void setModifiedBytes(int modifiedBytes) {
            this.modifiedBytes = modifiedBytes;
        }
    }
}
