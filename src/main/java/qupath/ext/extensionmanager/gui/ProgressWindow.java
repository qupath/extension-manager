package qupath.ext.extensionmanager.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * A window that displays the progress of a long-running and cancellable operation.
 */
public class ProgressWindow extends Stage {

    private final Runnable onCancelClicked;
    @FXML
    private Label label;
    @FXML
    private ProgressBar progressBar;

    /**
     * Create the window.
     *
     * @param label a text describing the operation
     * @param onCancelClicked a function that will be called when the user cancel the operation. This window is
     *                        already automatically closed when this happens
     * @throws IOException when an error occurs while creating the window
     */
    public ProgressWindow(String label, Runnable onCancelClicked) throws IOException {
        this.onCancelClicked = onCancelClicked;

        UiUtils.loadFXML(this, ProgressWindow.class.getResource("progress_window.fxml"));

        initModality(Modality.APPLICATION_MODAL);

        setTitle(label);
    }

    @FXML
    private void onCancelClicked(ActionEvent ignored) {
        close();
        onCancelClicked.run();
    }

    /**
     * Set the progress displayed by the window.
     *
     * @param progress a number between 0 and 1, where 0 means the beginning and 1 the end of
     *                 the operation
     */
    public void setProgress(float progress) {
        progressBar.setProgress(progress);
    }

    /**
     * Set a text describing the current step.
     *
     * @param status the current step of the operation
     */
    public void setStatus(String status) {
        this.label.setText(status);
    }
}
