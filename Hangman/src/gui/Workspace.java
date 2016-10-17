package gui;

import apptemplate.AppTemplate;
import com.sun.tools.javac.comp.Flow;
import components.AppWorkspaceComponent;
import controller.HangmanController;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontWeight;
import propertymanager.PropertyManager;
import ui.AppGUI;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.io.IOException;

import static hangman.HangmanProperties.*;

/**
 * This class serves as the GUI component for the Hangman game.
 *
 * @author Ritwik Banerjee
 */
public class Workspace extends AppWorkspaceComponent {

    AppTemplate app; // the actual application
    AppGUI      gui; // the GUI inside which the application sits

    Label             guiHeadingLabel;   // workspace (GUI) heading label
    HBox              headPane;          // conatainer to display the heading
    HBox              bodyPane;          // container for the main game displays
    ToolBar           footToolbar;       // toolbar for game buttons
    BorderPane        figurePane;        // container to display the namesake graphic of the (potentially) hanging person
    VBox              gameTextsPane;     // container to display the text-related parts of the game
    HBox              guessedLetters;    // text area displaying all the letters guessed so far
    HBox              remainingGuessBox; // container to display the number of remaining guesses
    Button            startGame;         // the button to start playing a game of Hangman
    HangmanController controller;
    Button            hintButton;
    FlowPane          GuessedLettersFlowPane;

    String alphabetString = "abcdefghijklmnopqrstuvwxyz";
    char[] alphabetCharArray = alphabetString.toCharArray();

    /**
     * Constructor for initializing the workspace, note that this constructor
     * will fully setup the workspace user interface for use.
     *
     * @param initApp The application this workspace is part of.
     * @throws IOException Thrown should there be an error loading application
     *                     data for setting up the user interface.
     */
    public Workspace(AppTemplate initApp) throws IOException {
        app = initApp;
        gui = app.getGUI();
        controller = (HangmanController) gui.getFileController();    //new HangmanController(app, startGame); <-- THIS WAS A MAJOR BUG!??
        layoutGUI();     // initialize all the workspace (GUI) components including the containers and their layout
        setupHandlers(); // ... and set up event handling
    }

    private void layoutGUI() {
        PropertyManager propertyManager = PropertyManager.getManager();
        guiHeadingLabel = new Label(propertyManager.getPropertyValue(WORKSPACE_HEADING_LABEL));

        headPane = new HBox();
        headPane.getChildren().add(guiHeadingLabel);
        headPane.setAlignment(Pos.CENTER);

        figurePane = new BorderPane();
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        gameTextsPane = new VBox();
        gameTextsPane.setStyle("-fx-padding: 20;");

        GuessedLettersFlowPane = new FlowPane();


//        for(int i = 0;i< alphabetCharArray.length;i++)
//        {
//            StackPane stack = new StackPane();
//            Rectangle rect = new Rectangle(50.0,50.0);
//            rect.setFill(Paint.valueOf("green"));
//            Text letter = new Text(Character.toString(alphabetCharArray[i]));
//            stack.getChildren().add(letter);
//            GuessedLettersFlowPane.getChildren().add(i,stack);
//        }
       /*out:
        for(int row = 0;row < 7;row++)
        {
            int count = 0;

            for(int column = 0;column< 4;column++){
                StackPane stack = new StackPane();
                Rectangle rect = new Rectangle(50.0,50.0);
                rect.setFill(Paint.valueOf("green"));
                Text letter = new Text(Character.toString(alphabetCharArray[count]));
                letter.setFont(Font.font(null,FontWeight.BOLD,20.0));
                stack.getChildren().addAll(rect,letter);
                GuessedLettersFlowPane.getChildren().addAll(stack);
                count++;
                if(count > 26)
                    break out;
            }

        }*/

        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters,GuessedLettersFlowPane);

        bodyPane = new HBox();
        bodyPane.getChildren().addAll(figurePane, gameTextsPane);

        startGame = new Button("Start Playing");
        hintButton = new Button("Hint");
        HBox blankBoxLeft  = new HBox();
        HBox blankBoxRight = new HBox();
        HBox.setHgrow(blankBoxLeft, Priority.ALWAYS);
        HBox.setHgrow(blankBoxRight, Priority.ALWAYS);
        footToolbar = new ToolBar(blankBoxLeft, startGame, blankBoxRight,hintButton);

        workspace = new VBox();
        workspace.getChildren().addAll(headPane, bodyPane, footToolbar);
    }

    public BorderPane getFigurePane()
    {
        return figurePane;
    }
    private void setupHandlers() {
        startGame.setOnMouseClicked(e -> controller.start());
        hintButton.setOnMouseClicked(e -> controller.getHint());
    }

    /**
     * This function specifies the CSS for all the UI components known at the time the workspace is initially
     * constructed. Components added and/or removed dynamically as the application runs need to be set up separately.
     */
    @Override
    public void initStyle() {
        PropertyManager propertyManager = PropertyManager.getManager();

        gui.getAppPane().setId(propertyManager.getPropertyValue(ROOT_BORDERPANE_ID));
        gui.getToolbarPane().getStyleClass().setAll(propertyManager.getPropertyValue(SEGMENTED_BUTTON_BAR));
        gui.getToolbarPane().setId(propertyManager.getPropertyValue(TOP_TOOLBAR_ID));

        ObservableList<Node> toolbarChildren = gui.getToolbarPane().getChildren();
        toolbarChildren.get(0).getStyleClass().add(propertyManager.getPropertyValue(FIRST_TOOLBAR_BUTTON));
        toolbarChildren.get(toolbarChildren.size() - 1).getStyleClass().add(propertyManager.getPropertyValue(LAST_TOOLBAR_BUTTON));

        workspace.getStyleClass().add(CLASS_BORDERED_PANE);
        guiHeadingLabel.getStyleClass().setAll(propertyManager.getPropertyValue(HEADING_LABEL));

    }

    /** This function reloads the entire workspace */
    @Override
    public void reloadWorkspace() {
        /* does nothing; use reinitialize() instead */
    }

    public VBox getGameTextsPane() {
        return gameTextsPane;
    }

    public HBox getRemainingGuessBox() {
        return remainingGuessBox;
    }

    public Button getStartGame() {
        return startGame;
    }

    public Button getHintButton(){return hintButton;}

    public void reinitialize() {
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        GuessedLettersFlowPane = new FlowPane();
        gameTextsPane = new VBox();
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters,GuessedLettersFlowPane);
        figurePane = new BorderPane();
        bodyPane.getChildren().setAll(figurePane, gameTextsPane);
    }
}
