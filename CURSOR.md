# FFTA2 Editor Repository Guide

## Purpose
- JavaFX desktop application for editing **Final Fantasy Tactics A2** Nintendo DS ROM data.
- Workflow: extract `.nds` ROM -> decode game archives and sub-archives -> edit in UI tabs -> repack and save a new ROM.
- The editor reads and writes both high-level game records (jobs, abilities, items, text, etc.) and low-level binary/code patches.

## Tech Stack
- Java 25 (Maven project).
- JavaFX (`controls`, `fxml`, `swing`).
- Module name: `org.ruru.ffta2editor`.
- External runtime dependency: `ndstool` executable available in PATH (or next to the packaged app on Windows).

## Run and Build
- Run app in dev:
  - `mvn clean javafx:run`
- Compile:
  - `mvn clean compile`
- Packaging notes are in comments in `src/main/java/org/ruru/ffta2editor/App.java` (jlink/jpackage examples).

## High-Level Architecture
- `src/main/java/org/ruru/ffta2editor/App.java`
  - JavaFX entrypoint.
  - Holds shared static state (loaded archives, binary buffers, and observable data lists used by controllers).
  - Loads `main.fxml`.
- `src/main/resources/org/ruru/ffta2editor/main.fxml`
  - Main UI shell with tabbed editors.
  - Wires each tab to a controller (abilities, jobs, text, patches, sprites, etc.).
- `src/main/java/org/ruru/ffta2editor/MainController.java`
  - Orchestrates ROM load/save and delegates per-domain parsing/saving to tab controllers.
  - Handles extraction/repacking using `ndstool`.
  - Loads and saves all tab data in a fixed order.

## ROM and Data Pipeline
1. User opens a ROM in `MainController.openFileSelector()`.
2. `ndstool -x` unpacks the ROM into a temp directory.
3. `Archive` parses `pc.idx`/`pc.bin` (master archive under unpacked `data/master`).
4. `IdxAndPak` wrappers parse many game sub-archives (`*.idx` + `*.pak` / `*.dat`) into editable file lists.
5. Controllers decode these binary files into model objects bound to JavaFX UI.
6. On save:
   - Controllers write model changes back to binary buffers.
   - `IdxAndPak` objects are repacked.
   - Master `Archive` is repacked and written back into unpacked ROM files.
   - `ndstool -c` rebuilds final `.nds`.

## Core Binary Utility Classes
- `src/main/java/org/ruru/ffta2editor/utility/Archive.java`
  - Parser/repacker for game `pc.idx` + `pc.bin`.
  - Supports direct file entries and list-based entries (multiple candidates sharing a hash slot).
  - Uses game filename CRC routing (`filename_crc`) for lookups.
- `src/main/java/org/ruru/ffta2editor/utility/IdxAndPak.java`
  - Parser/repacker for sub-archives (`idx` + `pak/dat`).
  - Maintains list of files (`ByteBuffer` or `null`) and handles alignment/padding.

## Controllers and Resources
- Java controllers: `src/main/java/org/ruru/ffta2editor/*Controller.java`
- FXML views: `src/main/resources/org/ruru/ffta2editor/*.fxml`
- Pattern: controller `loadX()` decodes bytes into models; `saveX()` encodes models back to buffers.
- `MainController` is the integration point that calls each controller's load/save methods.

## Important: `PatchesController`
`src/main/java/org/ruru/ffta2editor/PatchesController.java` is a key low-level component.

- It contains predefined ROM/gameplay patches with hardcoded ARM instruction addresses and opcodes.
- It directly modifies:
  - `App.arm9` (ARM9 executable bytes),
  - `App.overlay11` (overlay binary),
  - specific archive resources (example: `battle/btlprocess.sbn` for MP gain behavior).
- It exposes patch toggles/actions in `patches.fxml`, including:
  - expanded top sprite index limit,
  - signed equipment stats (support negative modifiers),
  - starting with max MP,
  - sequencer/peytral stat growth change,
  - all consumables buyable,
  - max level value,
  - custom MP gain per turn,
  - animation fix (irreversible content injection behavior).

### Patch Implementation Pattern
- `PatchElement(address, originalBytes, modifiedBytes)` records expected and replacement instructions.
- `applyPatchElements(...)` can apply or revert a patch set.
- Safety check: logs warnings if current bytes at an address are not what is expected.
- `loadPatches()` infers patch status by inspecting known bytes and binds state to toggle buttons.

### Risks and Care Points
- Any address/opcode change in `PatchesController` is **binary-sensitive**:
  - A wrong address or instruction can corrupt ROM behavior.
  - Always verify expected original bytes before writing new values.
- Keep byte order assumptions explicit (`ByteOrder.LITTLE_ENDIAN` throughout binary work).
- Preserve 4-byte alignment where required when replacing or injecting data.

## Practical Development Notes
- `App` stores global mutable state; changing load/save order can introduce subtle data dependencies.
- Many edits are `ByteBuffer`-based and assume `rewind()`/position handling is correct.
- Archive and sub-archive repacking logic relies on alignment; size mismatches can break loading in-game.
- The project currently favors functional/manual validation over formal tests.

## Manual Validation Checklist
- Load an unmodified ROM successfully.
- Open key tabs (Text, Jobs, Items, Patches) and verify data appears sane.
- Apply/revert one toggle patch and confirm status reflects correctly after reload.
- Save ROM to new file and confirm repack completes.
- Reopen saved ROM and verify edited values persist.
- If changing binary patch code, test in emulator/hardware to confirm runtime behavior.

## Known External Constraints
- Editor depends on `ndstool` from devkitPro tooling.
- As documented in `README.md`, ROMs modified by this editor may not remain compatible with other editors.
- Keep backups of original ROMs and intermediate saves when iterating on binary patches.
