package org.ruru.ffta2editor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ruru.ffta2editor.CharacterController.CharacterCell;
import org.ruru.ffta2editor.JobController.AbilitySetCell;
import org.ruru.ffta2editor.JobController.JobCell;
import org.ruru.ffta2editor.model.character.CharacterData;
import org.ruru.ffta2editor.model.job.AbilitySet;
import org.ruru.ffta2editor.model.job.JobData;
import org.ruru.ffta2editor.model.job.JobRequirementData;
import org.ruru.ffta2editor.utility.AutoCompleteComboBox;
import org.ruru.ffta2editor.utility.ByteChangeListener;
import org.ruru.ffta2editor.utility.ShortChangeListener;
import org.ruru.ffta2editor.utility.ByteStringConverter;
import org.ruru.ffta2editor.utility.UnsignedShortStringConverter;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class JobRequirementController {
    
    private static Logger logger = Logger.getLogger("org.ruru.ffta2editor");
    
    public static class JobRequirementCell extends ListCell<JobRequirementData> {
        Label label = new Label();


        public JobRequirementCell() {
            label.setStyle("-fx-text-fill: black");
        }

        @Override protected void updateItem(JobRequirementData jobRequirement, boolean empty) {
            super.updateItem(jobRequirement, empty);
            if (jobRequirement != null) {
                label.setText(String.format("%X: %s", jobRequirement.id , jobRequirement.jobId.getValue().name.getValue()));
            } else {
                label.setText("");
            }
            setGraphic(label);
        }
    }

    @FXML ListView<JobRequirementData> jobRequirementList;

    
    @FXML TextField flag;
    @FXML TextField indexOrder;
    @FXML TextField abilityNum1;
    @FXML TextField abilityNum2;
    @FXML TextField abilityNum3;
    @FXML AutoCompleteComboBox<JobData> jobId;
    @FXML AutoCompleteComboBox<AbilitySet> job1;
    @FXML AutoCompleteComboBox<AbilitySet> job2;
    @FXML AutoCompleteComboBox<AbilitySet> job3;
    @FXML AutoCompleteComboBox<CharacterData> character1;
    @FXML AutoCompleteComboBox<CharacterData> character2;

    private ObjectProperty<JobRequirementData> jobRequirementProperty = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {
        jobRequirementList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) unbindJobRequirementData();
            jobRequirementProperty.setValue(newValue);
            if (newValue != null) bindJobRequirementData();
        });

        // Data validators
        flag.textProperty().addListener(new ShortChangeListener(flag));

        indexOrder.textProperty().addListener(new ByteChangeListener(indexOrder));
        abilityNum1.textProperty().addListener(new ByteChangeListener(abilityNum1));
        abilityNum2.textProperty().addListener(new ByteChangeListener(abilityNum2));
        abilityNum3.textProperty().addListener(new ByteChangeListener(abilityNum3));
    }

    public void unbindJobRequirementData() {
        flag.textProperty().unbindBidirectional(jobRequirementProperty.getValue().flag);

        indexOrder.textProperty().unbindBidirectional(jobRequirementProperty.getValue().indexOrder);
        abilityNum1.textProperty().unbindBidirectional(jobRequirementProperty.getValue().abilityNum1);
        abilityNum2.textProperty().unbindBidirectional(jobRequirementProperty.getValue().abilityNum2);
        abilityNum3.textProperty().unbindBidirectional(jobRequirementProperty.getValue().abilityNum3);

        jobId.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().jobId);
        job1.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().job1);
        job2.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().job2);
        job3.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().job3);
        character1.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().character1);
        character2.valueProperty().unbindBidirectional(jobRequirementProperty.getValue().character2);
    }

    public void bindJobRequirementData() {
        StringConverter<Short> unsignedShortConverter = new UnsignedShortStringConverter();
        Bindings.bindBidirectional(flag.textProperty(), jobRequirementProperty.getValue().flag, unsignedShortConverter);

        StringConverter<Byte> unsignedByteConverter = new ByteStringConverter();
        Bindings.bindBidirectional(indexOrder.textProperty(), jobRequirementProperty.getValue().indexOrder, unsignedByteConverter);
        Bindings.bindBidirectional(abilityNum1.textProperty(), jobRequirementProperty.getValue().abilityNum1, unsignedByteConverter);
        Bindings.bindBidirectional(abilityNum2.textProperty(), jobRequirementProperty.getValue().abilityNum2, unsignedByteConverter);
        Bindings.bindBidirectional(abilityNum3.textProperty(), jobRequirementProperty.getValue().abilityNum3, unsignedByteConverter);

        jobId.valueProperty().bindBidirectional(jobRequirementProperty.getValue().jobId);
        job1.valueProperty().bindBidirectional(jobRequirementProperty.getValue().job1);
        job2.valueProperty().bindBidirectional(jobRequirementProperty.getValue().job2);
        job3.valueProperty().bindBidirectional(jobRequirementProperty.getValue().job3);
        character1.valueProperty().bindBidirectional(jobRequirementProperty.getValue().character1);
        character2.valueProperty().bindBidirectional(jobRequirementProperty.getValue().character2);
    }

    @FXML
    public void addJobRequirement() {
        if (jobRequirementList.getItems() != null) {
            int newIndex = jobRequirementList.getItems().size();
            jobRequirementList.getItems().add(new JobRequirementData(newIndex));
            jobRequirementList.getSelectionModel().selectLast();
        }
    }

    @FXML
    public void removeJobRequirement() {
        if (jobRequirementList.getSelectionModel().getSelectedItem() != null && jobRequirementList.getSelectionModel().getSelectedIndex() > 0) {
            jobRequirementList.getItems().remove(jobRequirementList.getSelectionModel().getSelectedIndex());
        }
    }

    public void loadJobRequirements() throws Exception {
        if (App.archive != null) {

            ByteBuffer jobRequirementBytes = App.sysdata.getFile(19);

            if (jobRequirementBytes == null) {
                System.err.println("IdxAndPak null file error");
                throw new Exception("Job Requirements are null");
            }
            jobRequirementBytes.rewind();

            ObservableList<JobRequirementData> jobRequirementDataList = FXCollections.observableArrayList();


            logger.info("Loading Job Requirements");
            int numJobRequirements = Byte.toUnsignedInt(App.arm9.get(0x000b80b8));
            for (int i = 0; i < numJobRequirements; i++) {
                try {
                    JobRequirementData jobRequirement = new JobRequirementData(jobRequirementBytes, i);
                    jobRequirementDataList.add(jobRequirement);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, String.format("Failed to load Job Requirement %d", i));
                    throw e;
                }
            }
            jobRequirementList.setItems(jobRequirementDataList);
            jobRequirementList.setCellFactory(x -> new JobRequirementCell());

            jobRequirementBytes.rewind();
            
            jobId.setData(App.jobDataList);
            jobId.setButtonCell(new JobCell());
            jobId.setCellFactory(x -> new JobCell());

            job1.setData(App.abilitySetList);
            job1.setButtonCell(new AbilitySetCell());
            job1.setCellFactory(x -> new AbilitySetCell());

            job2.setData(App.abilitySetList);
            job2.setButtonCell(new AbilitySetCell());
            job2.setCellFactory(x -> new AbilitySetCell());
            
            job3.setData(App.abilitySetList);
            job3.setButtonCell(new AbilitySetCell());
            job3.setCellFactory(x -> new AbilitySetCell());

            character1.setData(App.characterList);
            character1.setButtonCell(new CharacterCell());
            character1.setCellFactory(x -> new CharacterCell());

            character2.setData(App.characterList);
            character2.setButtonCell(new CharacterCell());
            character2.setCellFactory(x -> new CharacterCell());
        }
    }

    public void saveJobRequirements() {
        
        List<JobRequirementData> jobRequirements = jobRequirementList.getItems();
        ByteBuffer newJobRequirementBytes = ByteBuffer.allocate(jobRequirements.size()*0xC).order(ByteOrder.LITTLE_ENDIAN);

        logger.info("Saving Job Requirements");
        for (int i = 0; i < jobRequirements.size(); i++) {
            try {
                newJobRequirementBytes.put(jobRequirements.get(i).toBytes());
            } catch (Exception e) {
                logger.log(Level.SEVERE, String.format("Failed to save Job Requirement %d \"%s\"", i, jobRequirements.get(i).jobId.getValue().name.getValue()));
                throw e;
            }
        }
        newJobRequirementBytes.rewind();
        App.sysdata.setFile(19, newJobRequirementBytes);
        // Patch code with new Characters length
        App.arm9.put(0x000cb4bc, (byte)(jobRequirements.size()-1));
        App.arm9.put(0x000cb4c0, (byte)(jobRequirements.size()-1));
        App.arm9.put(0x000b80b8, (byte)(jobRequirements.size()));
        App.arm9.put(0x000b7f60, (byte)(jobRequirements.size()));
        App.arm9.put(0x000b8290, (byte)(jobRequirements.size()));

        App.overlay11.put(0x8CCC, (byte)(jobRequirements.size()));
        App.overlay11.put(0x9138, (byte)(jobRequirements.size()));
    }
}
