package com.example.javafxmysqltemplate;

import com.example.database.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class RecipeCatalogController {
    public TextField searchBar;
    public ListView<Button> recipeListView;
    public TextField userNameBar;
    public TextField passwordBar;
    public Label errorText;
    @FXML
    private Label welcomeText;
    private final Connection connection = Database.newConnection();
    private String username;

    public RecipeCatalogController() throws SQLException {
    }

    @FXML
    protected void onSignInClick() throws IOException {
        try (Connection connection = Database.newConnection()) {

            ResultSet signIn;
            username = userNameBar.getText().trim();
            PreparedStatement login = connection.prepareStatement("SELECT * FROM user WHERE BINARY Username='" + username + "' AND BINARY password='" + passwordBar.getText().trim() + "'");
            signIn = login.executeQuery();
            if (!signIn.next()) {
                errorText.setText("Username or Password incorrect.");
                return;
            }


            ((Stage) welcomeText.getScene().getWindow()).close();
            Stage mainStage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogApplication.class.getResource("Catalog-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 320, 240);
            RecipeCatalogController controller = fxmlLoader.getController();
            controller.setUserName(username);
            mainStage.setTitle("Recipe Catalog");
            mainStage.setScene(scene);
            mainStage.show();
        } catch (SQLException e) {
            welcomeText.setText("Could not Connect\nERROR: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    protected void recipeSearch() throws SQLException {

        ObservableList<Button> recipeNames = FXCollections.observableArrayList();
        String search = searchBar.getText().trim();
        String sql = "SELECT Name FROM Recipe WHERE Name LIKE ?";

        PreparedStatement stmt = connection.prepareStatement(sql);

        // Set the search parameter with wildcards for partial matches
        stmt.setString(1, "%" + search + "%");
        ResultSet rs = stmt.executeQuery();

        if(rs == null){
            System.out.println("null");
        }
        while (rs.next()) {
            String recipe = rs.getString("name");
            Button button = new Button(recipe);

            EventHandler<ActionEvent> e = _ -> {
                try {
                    onRecipeClick(recipe);
                } catch (SQLException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            };

            button.setOnAction(e);
            recipeNames.add(button);
        }
        recipeListView.setItems(recipeNames);
    }

    @FXML
    public void onRecipeClick(String recipe) throws SQLException, IOException {
        PreparedStatement fullRecipe = connection.prepareStatement("SELECT * FROM recipe WHERE Name='" + recipe + "';");
        ResultSet rs = fullRecipe.executeQuery();

        Stage recipeOptions = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogApplication.class.getResource("Recipe-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        RecipeController controller = fxmlLoader.getController();
        controller.createPage(rs);
        recipeOptions.setTitle(recipe);
        recipeOptions.setScene(scene);
        recipeOptions.show();

    }

    public void setUserName(String username) {
        this.username = username;
    }

    public void addRecipe() throws IOException {
        Stage addRecipeWindow = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(RecipeCatalogApplication.class.getResource("EditRecipe-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        RecipeEditorController controller = fxmlLoader.getController();
        controller.setUpPage();
        System.out.println(username);
        controller.setUsername(username);
        addRecipeWindow.setTitle("New Recipe");
        addRecipeWindow.setScene(scene);
        addRecipeWindow.show();
    }
}