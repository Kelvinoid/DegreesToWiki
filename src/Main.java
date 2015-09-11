import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;


public class Main extends Application {

    // StringProperty that controls the job status
    StringProperty jobStatusProperty = new SimpleStringProperty();
    // StringProperty that controls the output
    StringProperty outputProperty = new SimpleStringProperty();
    // Global Timer for automated tasks
    Timer timer = new Timer(true);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(Main.class, args);
    }

    @Override
    public void start(Stage stage) {


        // Use a border pane as the root for scene
        BorderPane border = new BorderPane();
        // Add a Tile Pane in the center of the border pane
        border.setCenter(addTilePane());
        // Set the background color to a nice tan
        border.setStyle("-fx-background-color: #FDF3E7");

        /*
        Colors:
        #C63D0F
        #3B3738
        #FDF3E7
        #7E8F7C
         */

        // Initialise the scene and set it to be visible
        Scene scene = new Scene(border);
        stage.setScene(scene);
        stage.setTitle("Degrees To Wikipedia");
        stage.show();
    }


    /*
     * Creates a horizontal (default) tile pane with eight icons in four rows
     */
    private TilePane addTilePane() {

        TilePane tile = new TilePane();
        tile.setPadding(new Insets(5, 5, 5, 5));
        tile.setVgap(5);
        tile.setHgap(5);
        tile.setPrefColumns(2);
        tile.setPrefRows(3);
        tile.setAlignment(Pos.CENTER);
        tile.setStyle("-fx-background-color: #FDF3E7;");

        // Text box 1
        TextField sourcePage = new TextField();
        sourcePage.setFont(Font.font("Helvetica", FontWeight.EXTRA_LIGHT, 30));
        sourcePage.setPromptText("Source Page");

        // Text box 2
        TextField destinationPage = new TextField();
        destinationPage.setFont(Font.font("Helvetica", FontWeight.EXTRA_LIGHT, 30));
        destinationPage.setPromptText("Destination Page");

        // Submit button
        Button submitButton = new Button();
        submitButton.setFont(Font.font("Helvetica", FontWeight.EXTRA_LIGHT, 30));
        submitButton.setText("Submit");
        submitButton.setOnAction(event -> {
            if (!sourcePage.getText().isEmpty() && !destinationPage.getText().isEmpty())
                clickSubmitButton(sourcePage.getText(), destinationPage.getText());
        });

        // Job Progress Label
        Label jobView = new Label();
        jobView.setFont(Font.font("Helvetica", FontWeight.EXTRA_LIGHT, 30));
        jobView.textProperty().bind(jobStatusProperty);

        // Job Output Label
        Label outputView = new Label();
        outputView.setFont(Font.font("Helvetica", FontWeight.EXTRA_LIGHT, 30));
        outputView.textProperty().bind(outputProperty);

        // Add everything to the parent
        tile.getChildren().addAll(sourcePage, destinationPage, submitButton, jobView, outputView);

        return tile;
    }

    private void clickSubmitButton(String source, String destination) {
        String result = null;
        try {
            result = makeJob(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (result != null && !result.isEmpty()) {
            TimerTask timerTask = new UpdateJobStatusTask(result);
            timer.cancel();
            timer.schedule(timerTask, 0, 5000);
        }
    }

    private String makeJob(String sourcePage, String destinationPage) throws IOException {
        // Debug

        URL url = new URL("http://www.wikipathfinder.com/find/?source=" + sourcePage + "&destination=" + destinationPage);
        String json = getResponseFromURL(url);



        JSONObject obj = new JSONObject(json);
        boolean success = obj.getBoolean("success");

        if(success) {

            return obj.getString("result");

        }

        return null;
    }

    private String getResponseFromURL(URL url) throws IOException {
        HttpURLConnection yc =(HttpURLConnection) url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        String allLines = "";
        while ((inputLine = in.readLine()) != null) {
            allLines += inputLine;
            // Debug
        }
        in.close();
        return allLines;
    }

    class UpdateJobStatusTask extends TimerTask {

        String jobID;

        public UpdateJobStatusTask(String jobID) {
            this.jobID = jobID;
        }

        @Override
        public void run() {
            try {
                URL url = new URL("http://www.wikipathfinder.com/check/?job_id=" + jobID);
                String response = getResponseFromURL(url);
                JSONObject obj = new JSONObject(response);
                Platform.runLater(() -> jobStatusProperty.setValue(obj.getJSONObject("result").getString("status")));

                if(obj.getJSONObject("result").getString("status").equals("finished")) {
                    if(obj.getJSONObject("result").getJSONArray("output").length() > 0) {
                        Platform.runLater(() -> outputProperty.setValue(String.valueOf(obj.getJSONObject("result").getJSONArray("output").get(0))));
                    } else {
                        Platform.runLater(() -> outputProperty.setValue("No result"));
                    }
                    this.cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}