package com.example.javafxmysqltemplate;

import com.example.database.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class RecipeEditorController {
    @FXML
    private TextField cookingTime;
    @FXML
    private ComboBox<String> mealTypeComboBox = new ComboBox<>();
    @FXML
    private ListView<HBox> directionsListView;
    @FXML
    private Button saveButton;
    @FXML
    private TextField recipeNameBar;
    @FXML
    private ListView<HBox> ingredientListView;

    private String recipeName;
    private final ObservableList<Ingredient> ingredients = FXCollections.observableArrayList();
    private final ObservableList<String> directionsList = FXCollections.observableArrayList();
    private final Connection connection = Database.newConnection();
    private final ArrayList<String> ingredientsOriginal = new ArrayList<>();
    private boolean isNewRecipe;
    private String username;

    public RecipeEditorController() throws SQLException {
        isNewRecipe = true;
    }

    public void setUpPage() {
        mealTypeComboBox.getItems().addAll("Breakfast", "Lunch", "Dinner", "Dessert", "Drink");

    }

    //takes the recipe ResultSet and adds all the info into the page
    public void setUpPage(ResultSet recipe, ResultSet ingredientSet) throws SQLException {
        setUpPage();
        isNewRecipe = false;
        recipeName = recipe.getString("Name");
        recipeNameBar.setText(recipeName);
        recipeNameBar.setEditable(false);

        mealTypeComboBox.getSelectionModel().select(mealTypeComboBox.getItems().indexOf(recipe.getString("Type")));
        cookingTime.setText(recipe.getString("CookingTime"));

        while (ingredientSet.next()) {
            /*if (ingredientSet.getString("IngredientName").equals("Egg")){
                ingredients.add(new Ingredient(ingredientSet.getString("Amount"), ingredientSet.getString("Unit")));
            } else{*/
                ingredients.add(new Ingredient(ingredientSet.getString("Amount") + " " + ingredientSet.getString("Unit"), ingredientSet.getString("IngredientName")));
            //J}
            ingredientsOriginal.add(ingredientSet.getString("IngredientName"));
        }
        updateIngredientListView();

        String directions = recipe.getString("Steps");
        int offset = 3;
        int start = 0;
        int currentStep = 2;
        for(int i = 2; i < directions.length() - 1; ++i){
            if ((directions.substring(i,i+2)).equals(currentStep + ".")){
                if (currentStep > 9){
                    offset = 4;
                } else {
                    offset = 3;
                }
                directionsList.add(directions.substring(start+offset, i-1));
                currentStep++;
                start = i;
            }
        }
        directionsList.add(directions.substring(start+offset));
        updateDirectionsListView();
    }

    private void updateDirectionsListView() {
        directionsListView.getItems().clear();

        for (int i = 0; i < directionsList.size(); i++) {
            String step = directionsList.get(i);

            Label stepNumber = new Label("" + (i + 1));

            TextField amountField = new TextField(step);
            amountField.setPrefWidth(225);
            amountField.setPromptText("Add next step");

            Button deleteButton = new Button("Delete");
            int index = i; // Capture the index for use in the event handler
            deleteButton.setOnAction(_ -> {
                directionsList.remove(index);
                updateDirectionsListView();
            });

            // Update the step when fields are edited
            amountField.textProperty().addListener((_, _, newValue) -> directionsList.set(index, newValue));

            HBox itemBox = new HBox(10, stepNumber, amountField, deleteButton);
            directionsListView.getItems().add(itemBox);
        }
    }

    @FXML
    private void addIngredient() {
        // Add a blank ingredient and refresh the ListView
        ingredients.add(new Ingredient("", ""));
        updateIngredientListView();
    }

    private void updateIngredientListView() {
        ingredientListView.getItems().clear();

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);

            // TextField for the amount
            TextField amountField = new TextField(ingredient.getAmount());
            amountField.setPrefWidth(125);
            amountField.setPromptText("e.g. 2 cups, 1.5 tbsp,...");

            // TextField for the ingredient name
            TextField nameField = new TextField(ingredient.getName());
            nameField.setPrefWidth(150);
            nameField.setPromptText("e.g. Flour, Eggs, Sugar,...");

            Button deleteButton = new Button("Delete");
            int index = i; // Capture the index for use in the event handler
            deleteButton.setOnAction(_ -> {
                ingredients.remove(index);
                updateIngredientListView();
            });

            // Update the ingredient when fields are edited
            amountField.textProperty().addListener((_, _, newValue) -> ingredient.setAmount(newValue));

            nameField.textProperty().addListener((_, _, newValue) -> ingredient.setName(newValue));

            // Layout for each ingredient (Amount + Name + Delete Button)
            HBox itemBox = new HBox(10, amountField, nameField, deleteButton);
            ingredientListView.getItems().add(itemBox);
        }
    }
    //This updates the tables in the database that this data was pulled from
    @FXML
    private void onSaveClick() throws SQLException {
        PreparedStatement changes;
        recipeName = recipeNameBar.getText().trim();
        int stepCounter = 1;
        StringBuilder directionsString = new StringBuilder();
        for (String s : directionsList) {
            directionsString.append(stepCounter).append(". ").append(s).append(" ");
            stepCounter++;
        }

        if (isNewRecipe) {
            changes = connection.prepareStatement("INSERT INTO recipe VALUES (?, ?, ?, ?, ?);");
            changes.setString(1, recipeName);
            changes.setString(2, mealTypeComboBox.getValue());
            changes.setString(3, directionsString.toString());
            changes.setString(4, cookingTime.getText());
            changes.setString(5, username);
            changes.executeUpdate();
        } else {
            changes = connection.prepareStatement("UPDATE recipe SET Type=?, Steps=?, CookingTime=? WHERE name=?;");
            changes.setString(1, mealTypeComboBox.getValue());
            changes.setString(2, directionsString.toString());
            changes.setString(3, cookingTime.getText());
            changes.setString(4, username);
            changes.executeUpdate();
        }
        // Extract names from the new list
        ArrayList<String> addedIngredients = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            addedIngredients.add(ingredient.getName());
        }

        // Find ingredients no longer in the new list
        ArrayList<String> ingredientsForDeletion = new ArrayList<>(ingredientsOriginal);
        ingredientsForDeletion.removeAll(addedIngredients);

        // Find additional ingredients (full Ingredient objects)
        ArrayList<Ingredient> ingredientsToAdd = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (!ingredientsOriginal.contains(ingredient.getName())) {
                ingredientsToAdd.add(ingredient);
            }
        }

        // Find common ingredients (full Ingredient objects)
        ArrayList<Ingredient> ingredientsToUpdate = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredientsOriginal.contains(ingredient.getName())) {
                ingredientsToUpdate.add(ingredient);
            }
        }

        for(String ingredient : ingredientsForDeletion) {
            changes = connection.prepareStatement("DELETE FROM recipeIngredients WHERE recipeName=? AND ingredientName=?;");
            changes.setString(1, recipeName);
            changes.setString(2, ingredient);
            changes.executeUpdate();
        }

        for(Ingredient ingredient : ingredientsToUpdate) {
            String[] parts = ingredient.getAmount().split(" ", 2);
            if (parts.length < 2) {
                changes = connection.prepareStatement("UPDATE recipeIngredients SET Amount=?, Unit=? WHERE recipeName=? AND ingredientName=?;");
                changes.setString(1, parts[0]);
                changes.setString(2, "");
                changes.setString(3, recipeName);
                changes.setString(4, ingredient.getName());
                changes.executeUpdate();
            } else {
                changes = connection.prepareStatement("UPDATE recipeIngredients SET Amount=?, Unit=? WHERE recipeName=? AND ingredientName=?;");
                changes.setString(1, parts[0]);
                changes.setString(2, parts[1]);
                changes.setString(3, recipeName);
                changes.setString(4, ingredient.getName());}
        }

        for(Ingredient ingredient : ingredientsToAdd) {
            PreparedStatement addingIngredients = connection.prepareStatement("SELECT 1 FROM Ingredient WHERE Name= ? ");
            addingIngredients.setString(1, ingredient.getName());
            ResultSet checkForIngredient = addingIngredients.executeQuery();

            if (!checkForIngredient.next()) {
                addingIngredients = connection.prepareStatement("INSERT INTO Ingredient VALUES (?)");
                addingIngredients.setString(1, ingredient.getName());
                addingIngredients.executeUpdate();
            }

            String[] parts = ingredient.getAmount().split(" ", 2);
            if (parts.length < 2) {
                changes = connection.prepareStatement("INSERT INTO recipeIngredients VALUES (?, ?, ?, ?);");
                changes.setString(1, ingredient.getName());
                changes.setString(2, recipeName);
                changes.setString(3, parts[0]);
                changes.setString(4, "");
                changes.executeUpdate();
            } else {
                changes = connection.prepareStatement("INSERT INTO recipeIngredients VALUES (?, ?, ?, ?);");
                changes.setString(1, ingredient.getName());
                changes.setString(2, recipeName);
                changes.setString(3, parts[0]);
                changes.setString(4, parts[2]);
                changes.executeUpdate();}
        }

        Stage stage = (Stage) recipeNameBar.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void addStep() {
        // Add a blank step and refresh the ListView
        directionsList.add("");
        updateDirectionsListView();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Inner class to represent an ingredient
    public static class Ingredient {
        private String amount;
        private String name;

        public Ingredient(String amount, String name) {
            this.amount = amount;
            this.name = name;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
