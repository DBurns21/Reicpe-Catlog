package com.example.javafxmysqltemplate;

import com.example.database.Database;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RecipeController {
    @FXML
    private Button yesButton;
    @FXML
    private Button noButton;
    @FXML
    private ListView<String> ingredients;
    @FXML
    private VBox recipeInfo;
    @FXML
    private Button deleteButton;
    @FXML
    private Button editButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Label mealType;
    @FXML
    private Label cookTime;
    @FXML
    private Label recipeName;
    @FXML
    private Label addedBy;
    private final Connection connection = Database.newConnection();
    private ResultSet rs;


    public RecipeController() throws SQLException {
    }

    public void createPage(ResultSet rs) throws SQLException {
        this.rs = rs;
        rs.next();
        recipeName.setText(rs.getString("Name"));
        addedBy.setText("Added by: " + rs.getString("AddedBy"));
        mealType.setText("Meal Type: " + rs.getString("Type"));
        cookTime.setText("Cook Time: " + rs.getString("CookingTime"));

        PreparedStatement ingredientQuery = connection.prepareStatement("SELECT * FROM recipeingredients WHERE RecipeName='" + rs.getString(1) +"';");
        ResultSet ingredientSet = ingredientQuery.executeQuery();

        if(ingredientSet == null){
            System.out.println("null");
        }
        ObservableList<String> ingredientList = FXCollections.observableArrayList();
        String ingredientFull;
        while (ingredientSet.next()) {
            if (ingredientSet.getString("IngredientName").equals("Egg")){
                ingredientFull = ingredientSet.getString("Amount") + " " + ingredientSet.getString("Unit");

            } else{
                ingredientFull = ingredientSet.getString("Amount") + " " + ingredientSet.getString("Unit") + " " + ingredientSet.getString("IngredientName");
            }
            ingredientList.add(ingredientFull);

        }
        ingredients.prefHeightProperty().bind(Bindings.size(ingredientList).multiply(24));
        ingredients.setItems(ingredientList);

        String directions = rs.getString("Steps");
        int start = 0;
        int currentStep = 2;
        for(int i = 2; i < directions.length() - 1; ++i){
            if ((directions.substring(i,i+2)).equals(currentStep + ".")){
                recipeInfo.getChildren().add(new Text(directions.substring(start, i-1)));
                currentStep++;
                start = i;
            }

        }
        recipeInfo.getChildren().add(new Text(directions.substring(start)));

    }

    public void onDeleteClick() throws IOException {
        Stage confirmationWindow = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogApplication.class.getResource("Confirmation-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        confirmationWindow.setTitle("Are you Sure?");
        confirmationWindow.initModality(Modality.APPLICATION_MODAL);
        confirmationWindow.initOwner(recipeName.getScene().getWindow());
        confirmationWindow.setScene(scene);
        confirmationWindow.show();


    }

    public void onEditClick() throws IOException, SQLException {
        PreparedStatement ingredientQuery = connection.prepareStatement("SELECT * FROM recipeingredients WHERE RecipeName='" + rs.getString(1) +"';");
        ResultSet ingredientSet = ingredientQuery.executeQuery();

        Stage editRecipeWindow = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogApplication.class.getResource("EditRecipe-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        RecipeEditorController controller = fxmlLoader.getController();
        controller.setUpPage(rs, ingredientSet);
        editRecipeWindow.setTitle("Edit " + recipeName.getText());
        editRecipeWindow.setScene(scene);
        editRecipeWindow.show();
    }

    public void onDownloadClick() throws IOException, SQLException {
        String home = System.getProperty("user.home") + "/Downloads/";
        File file = new File(home +  recipeName.getText() + ".txt");

        int fileNumber = 1;
        while (!file.createNewFile()) {
            file= new File(home + recipeName.getText() + fileNumber + ".txt");
            ++fileNumber;
        }
        FileWriter writer = new FileWriter(file);
        writer.write(rs.getString("Name") + "\n\n");
        writer.write("Added By: " + rs.getString("AddedBy") + "\n");
        writer.write("Meal Type: " + rs.getString("Type") + "\n");
        writer.write("Cooking Time" + rs.getString("CookingTime") + "\n");
        writer.write("\nIngredients:\n");

        for (int i = 0; i < ingredients.getItems().size(); i++) {
            writer.write(ingredients.getItems().get(i) + "\n");
        }

        writer.write("\nDirections: \n");
        String directions = rs.getString("Steps");
        int start = 0;
        int currentStep = 2;
        for(int i = 2; i < directions.length() - 1; ++i){
            if ((directions.substring(i,i+2)).equals(currentStep + ".")){
                writer.write((directions.substring(start, i-1)) + "\n");
                currentStep++;
                start = i;
            }

        }
        writer.write(directions.substring(start) + "\n");
        writer.close();
    }


    public void onConfirmationClick(ActionEvent event) throws SQLException {
        if (event.getSource() == yesButton){
            ((Stage) yesButton.getScene().getWindow()).close();
            Stage s = ((Stage)((Stage) yesButton.getScene().getWindow()).getOwner());
            Label lookup = (Label) s.getScene().lookup("#recipeName");
            PreparedStatement deletion = connection.prepareStatement("DELETE FROM recipeingredients WHERE recipename='" + lookup.getText() +"';"); //Deletion from RecipeIngredients table
            deletion.execute();
            deletion = connection.prepareStatement("DELETE FROM recipe WHERE Name='" + lookup.getText() + "';"); //Deletion from recipe table
            deletion.execute();
            s.close();
        }   else {
            ((Stage) yesButton.getScene().getWindow()).close();
        }
    }
}
