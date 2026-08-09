package org.ruru.ffta2editor;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ruru.ffta2editor.PatchesController.PatchStatus;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class CustomPatchesController {
    private static final Logger logger = Logger.getLogger("org.ruru.ffta2editor");
    private static final Path PATCH_DIRECTORY = Path.of("custom-patches");

    @FXML ListView<Patch> appliedPatchList;
    @FXML ListView<Patch> notAppliedPatchList;
    @FXML TextArea patchContentArea;
    @FXML Button applyRevertButton;

    private final ObservableList<Patch> loadedPatches = FXCollections.observableArrayList();
    private final ObservableList<Patch> appliedPatches = FXCollections.observableArrayList();
    private final ObservableList<Patch> notAppliedPatches = FXCollections.observableArrayList();
    private final Map<Patch, String> patchYamlContent = new HashMap<>();
    private final Map<Patch, PatchStatus> patchStatuses = new HashMap<>();
    private Patch selectedPatch;
    private boolean selectedPatchIsApplied;

    @FXML
    public void initialize() {
        appliedPatchList.setItems(appliedPatches);
        notAppliedPatchList.setItems(notAppliedPatches);
        patchContentArea.setEditable(false);
        patchContentArea.setWrapText(true);

        appliedPatchList.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Patch item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.toString());
            }
        });
        notAppliedPatchList.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Patch item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                PatchStatus status = patchStatuses.getOrDefault(item, PatchStatus.NOT_APPLIED);
                if (status == PatchStatus.MIXED) {
                    setText(String.format("[MIXED] %s", item));
                    return;
                }
                setText(item.toString());
            }
        });

        appliedPatchList.getSelectionModel().selectedItemProperty().addListener((obs, oldPatch, newPatch) -> {
            if (newPatch == null) {
                if (notAppliedPatchList.getSelectionModel().getSelectedItem() == null) {
                    selectedPatch = null;
                    refreshSelectedPatchView();
                }
                return;
            }
            notAppliedPatchList.getSelectionModel().clearSelection();
            selectedPatch = newPatch;
            selectedPatchIsApplied = true;
            refreshSelectedPatchView();
        });

        notAppliedPatchList.getSelectionModel().selectedItemProperty().addListener((obs, oldPatch, newPatch) -> {
            if (newPatch == null) {
                if (appliedPatchList.getSelectionModel().getSelectedItem() == null) {
                    selectedPatch = null;
                    refreshSelectedPatchView();
                }
                return;
            }
            appliedPatchList.getSelectionModel().clearSelection();
            selectedPatch = newPatch;
            selectedPatchIsApplied = false;
            refreshSelectedPatchView();
        });

        loadPatchFiles();
    }

    public void loadPatchFiles() {
        loadedPatches.clear();
        patchYamlContent.clear();
        patchStatuses.clear();
        selectedPatch = null;
        try {
            Files.createDirectories(PATCH_DIRECTORY);
            List<Path> yamlFiles = new ArrayList<>();
            try (DirectoryStream<Path> yamlStream = Files.newDirectoryStream(PATCH_DIRECTORY)) {
                for (Path yamlPath : yamlStream) {
                    String fileName = yamlPath.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                        yamlFiles.add(yamlPath);
                    }
                }
            }
            yamlFiles.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()));
            for (Path yamlPath : yamlFiles) {
                loadPatchFile(yamlPath);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load custom patches", e);
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Custom patches");
            alert.setHeaderText("Failed to load custom patches");
            alert.setContentText(e.getMessage());
            alert.show();
        }
        refreshPatchColumns();
        refreshSelectedPatchView();
    }

    private void loadPatchFile(Path yamlPath) {
        try {
            String yamlText = Files.readString(yamlPath);
            Yaml yaml = new Yaml(new Constructor(Patch.class, new LoaderOptions()));
            Patch patch = yaml.load(yamlText);
            if (patch == null) {
                logger.warning(String.format("Ignoring %s: empty patch definition", yamlPath));
                return;
            }
            if (patch.getTitle() == null || patch.getTitle().isBlank()) {
                patch.setTitle(yamlPath.getFileName().toString());
            }
            if (!PatchesController.hasSupportedTargets(patch)) {
                logger.warning(String.format("Ignoring %s: invalid or unsupported targets", yamlPath));
                return;
            }
            loadedPatches.add(patch);
            patchYamlContent.put(patch, yamlText);
        } catch (Exception e) {
            logger.log(Level.WARNING, String.format("Failed to parse custom patch %s", yamlPath), e);
        }
    }

    public void onRomLoaded() {
        refreshPatchColumns();
        refreshSelectedPatchView();
    }

    private void refreshPatchColumns() {
        appliedPatches.clear();
        notAppliedPatches.clear();
        patchStatuses.clear();
        for (Patch patch : loadedPatches) {
            PatchStatus status = PatchesController.checkPatchStatus(patch);
            patchStatuses.put(patch, status);
            if (status == PatchStatus.APPLIED) {
                appliedPatches.add(patch);
            } else {
                notAppliedPatches.add(patch);
            }
        }
        if (selectedPatch != null && !loadedPatches.contains(selectedPatch)) {
            selectedPatch = null;
            appliedPatchList.getSelectionModel().clearSelection();
            notAppliedPatchList.getSelectionModel().clearSelection();
        }
    }

    private void refreshSelectedPatchView() {
        if (selectedPatch == null) {
            patchContentArea.clear();
            applyRevertButton.setDisable(true);
            applyRevertButton.setText("Apply");
            return;
        }

        patchContentArea.setText(patchYamlContent.getOrDefault(selectedPatch, ""));
        PatchStatus status = patchStatuses.getOrDefault(selectedPatch, PatchStatus.NOT_APPLIED);
        boolean canApply = PatchesController.canApplyPatch(selectedPatch);
        applyRevertButton.setDisable(!canApply || App.archive == null);
        if (status == PatchStatus.MIXED) {
            applyRevertButton.setText(selectedPatchIsApplied ? "Revert (force)" : "Apply (force)");
            return;
        }
        applyRevertButton.setText(selectedPatchIsApplied ? "Revert" : "Apply");
    }

    @FXML
    private void applyOrRevertSelectedPatch() {
        if (selectedPatch == null || App.archive == null) {
            return;
        }
        if (!PatchesController.canApplyPatch(selectedPatch)) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Custom patches");
            alert.setHeaderText("Unavailable patch buffers");
            alert.setContentText(String.format("Patch \"%s\" requires ROM buffers that are not currently available.", selectedPatch));
            alert.show();
            return;
        }
        PatchStatus status = patchStatuses.getOrDefault(selectedPatch, PatchStatus.NOT_APPLIED);
        if (status == PatchStatus.MIXED) {
            Alert mixedAlert = new Alert(AlertType.CONFIRMATION);
            mixedAlert.setTitle("Custom patches");
            mixedAlert.setHeaderText("Patch is partially applied");
            mixedAlert.setContentText(selectedPatchIsApplied
                ? "Revert will force all patch bytes back to original values. Continue?"
                : "Apply will force all patch bytes to modified values. Continue?");
            Optional<ButtonType> result = mixedAlert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }
        PatchesController.applyCustomPatch(selectedPatch, !selectedPatchIsApplied);
        refreshPatchColumns();
        if (selectedPatchIsApplied) {
            notAppliedPatchList.getSelectionModel().select(selectedPatch);
            selectedPatchIsApplied = false;
        } else {
            appliedPatchList.getSelectionModel().select(selectedPatch);
            selectedPatchIsApplied = true;
        }
        refreshSelectedPatchView();
    }

    @FXML
    private void reloadPatchFiles() {
        loadPatchFiles();
    }
}
