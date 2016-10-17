package controller;

import apptemplate.AppTemplate;
import com.sun.tools.javac.comp.Flow;
import data.GameData;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.shape.Rectangle;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.YesNoCancelDialogSingleton;
import javafx.geometry.Insets;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import java.util.List;
import java.util.ArrayList;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static settings.AppPropertyType.*;
import static settings.InitializationParameters.APP_WORKDIR_PATH;

/**
 * @author Ritwik Banerjee,Peter Chou
 */
public class HangmanController implements FileController {

    public enum GameState {
        UNINITIALIZED,
        INITIALIZED_UNMODIFIED,
        INITIALIZED_MODIFIED,
        ENDED
    }

    private AppTemplate appTemplate; // shared reference to the application
    private GameData    gamedata;    // shared reference to the game being played, loaded or saved
    private GameState   gamestate;   // the state of the game being shown in the workspace
    private Text[]      progress;    // reference to the text area for the word
    private boolean     success;     // whether or not player was successful
    private int         discovered;  // the number of letters already discovered
    private Button      gameButton;  // shared reference to the "start game" button
    private Label       remains;     // dynamically updated label that indicates the number of remaining guesses
    private Path        workFile;
    private Button      hintButton;
    private boolean     hintUseState = true;
    private FlowPane BoxOfGuesses;
    FlowPane          GuessedLettersFlowPane;
    Pane                canvas;
    private List<Shape>     hangmanProgress;

    String alphabetString = "abcdefghijklmnopqrstuvwxyz";
    char[] alphabetCharArray = alphabetString.toCharArray();


    public HangmanController(AppTemplate appTemplate, Button gameButton) {
        this(appTemplate);
        this.gameButton = gameButton;
    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
        this.gamestate = GameState.UNINITIALIZED;
    }

    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(false);
    }

    public void disableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(true);
    }

    public void disableHintButton()
    {
        if(hintButton == null)
        {
            Workspace workspace = (Workspace)appTemplate.getWorkspaceComponent();
            hintButton = workspace.getHintButton();
        }
        hintButton.setDisable(true);
    }

    public void enableHintButton()
    {
        if(hintButton == null)
        {
            Workspace workspace = (Workspace)appTemplate.getWorkspaceComponent();
            hintButton = workspace.getHintButton();
        }
        hintButton.setDisable(false);
    }

    public void setGameState(GameState gamestate) {
        this.gamestate = gamestate;
    }

    public GameState getGamestate() {
        return this.gamestate;
    }

    /**
     * In the homework code given to you, we had the line
     * gamedata = new GameData(appTemplate, true);
     * This meant that the 'gamedata' variable had access to the app, but the data component of the app was still
     * the empty game data! What we need is to change this so that our 'gamedata' refers to the data component of
     * the app, instead of being a new object of type GameData. There are several ways of doing this. One of which
     * is to write (and use) the GameData#init() method.
     */
    public void start() {
        gamedata = (GameData) appTemplate.getDataComponent();
        success = false;
        discovered = 0;
        hintUseState = true;


        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();

        gamedata.init();
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        HBox guessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);


        makeHangmanCanvas();
        makeGuessedLetters();

        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        initWordGraphics(guessedLetters);
        play();
        if((int)gamedata.getTargetWord().chars().distinct().count()>=7)
            enableHintButton();
        else
            disableHintButton();

    }

    public void makeGuessedLetters()
    {
        Workspace gameWorkspace = (Workspace)appTemplate.getWorkspaceComponent();
        GuessedLettersFlowPane = (FlowPane) gameWorkspace.getGameTextsPane().getChildren().get(2);
        GuessedLettersFlowPane.setPrefWrapLength(200.0);
        GuessedLettersFlowPane.setStyle("-fx-background-color: Transparent;");


        for(int i = 0;i< alphabetCharArray.length;i++)
        {
            StackPane stack = new StackPane();
            /*stack.setPrefSize(Region.USE_PREF_SIZE,Region.USE_PREF_SIZE);*/
            stack.setStyle("-fx-padding: 5;");
            Rectangle rect = new Rectangle(30.0,30.0);
            /*stack.setMargin(rect,new Insets(5.0,5.0,5.0,5.0));*/
            rect.setFill(Color.ORANGE);
            Text letter = new Text(Character.toString(alphabetCharArray[i]));
            letter.setFill(Color.MIDNIGHTBLUE);
            stack.getChildren().addAll(rect,letter);
            GuessedLettersFlowPane.getChildren().add(i,stack);
        }
    }
    public void makeHangmanCanvas()
    {
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();

        canvas = new Pane();
        canvas.setStyle("-fx-border-color: black;");
        canvas.setMinHeight(400.0);
        canvas.setMinWidth(300.0);

        Circle circle = new Circle(50.0);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.BLACK);
        circle.setCenterX(canvas.getMinWidth()/2);
        circle.setCenterY(canvas.getMinHeight()/4);
        //circle.setVisible(false);
        Line body = new Line(circle.getCenterX(),circle.getCenterY()+50.0,circle.getCenterX(),circle.getCenterY()+150.0);
        //body.setVisible(false);
        Line rightLeg = new Line(body.getEndX(),body.getEndY(),body.getEndX()+50.0,body.getEndY()+50.0);
       // rightLeg.setVisible(false);
        Line leftLeg = new Line(body.getEndX(),body.getEndY(),body.getEndX()-50.0,body.getEndY()+50.0);
        //leftLeg.setVisible(false);
        Line rightArm = new Line(body.getStartX(),body.getStartY()+10.0,body.getStartX()+50.0,body.getStartY()+50.0);
        //rightArm.setVisible(false);
        Line leftArm = new Line(body.getStartX(),body.getStartY()+10.0,body.getStartX()-50.0,body.getStartY()+50.0);
        //leftArm.setVisible(false);
        Line hang1 = new Line(circle.getCenterX(),circle.getCenterY()-50.0,circle.getCenterX(),circle.getCenterY()-70.0);
        //hang1.setVisible(false);
        Line hang2 = new Line(hang1.getEndX(),hang1.getEndY(),hang1.getEndX()-70.0,hang1.getEndY());
        //hang2.setVisible(false);
        Line hang3 = new Line(hang2.getEndX(),hang2.getEndY(),hang2.getEndX(),hang2.getEndY()+300.0);
        //hang3.setVisible(false);
        Line hang4 = new Line(hang3.getEndX()-150.0,hang3.getEndY(),hang3.getEndX()+300.0,hang3.getEndY());
        //hang4.setVisible(false);
        hangmanProgress = new ArrayList();
        hangmanProgress.add(circle);
        hangmanProgress.add(body);
        hangmanProgress.add(rightLeg);
        hangmanProgress.add(leftLeg);
        hangmanProgress.add(rightArm);
        hangmanProgress.add(leftArm);
        hangmanProgress.add(hang1);
        hangmanProgress.add(hang2);
        hangmanProgress.add(hang3);
        hangmanProgress.add(hang4);

        for(int i =0;i<hangmanProgress.size();i++)
        {
            hangmanProgress.get(i).setVisible(false);
        }
        canvas.getChildren().addAll(circle,body,rightLeg,leftLeg,rightArm,leftArm,hang1,hang2,hang3,hang4);
        gameWorkspace.getFigurePane().setCenter(canvas);
    }

    public void getHint()
    {
        disableHintButton();
        hintUseState = false;
        String targetWord = gamedata.getTargetWord();

        boolean notUsedLetter = false;
        char[] charTarget = targetWord.toCharArray();
        while(notUsedLetter == false) {

                int index = (int) (Math.random() * targetWord.length());

                if (!gamedata.getGoodGuesses().contains(charTarget[index])) {
                    notUsedLetter = true;
                    for(int i = 0;i<progress.length;i++)
                    {
                        if(gamedata.getTargetWord().charAt(i) == charTarget[index]) {
                            progress[i].setVisible(true);
                            discovered++;
                        }
                    }
                    int letterIndex = (int)charTarget[index] - 97;
                    StackPane fill = new StackPane();
                    Rectangle recta = new Rectangle(30.0,30.0,Color.RED);

                    fill.setStyle("-fx-padding: 5;");
                    fill.getChildren().addAll(recta,new Text(Character.toString(charTarget[index])));
                    GuessedLettersFlowPane.getChildren().remove(letterIndex);
                    GuessedLettersFlowPane.getChildren().add(letterIndex,fill);
                    hangmanProgress.get(0).setVisible(true);
                    hangmanProgress.remove(0);
                    gamedata.decreaseRemains();
                }
            }
        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
    }

    private void end() {
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameButton.setDisable(true);
        disableHintButton();
        for(int i =0;i<gamedata.getTargetWord().length();i++)
        {
            progress[i].setVisible(true);
            /*if(!gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)))*/
            if(!alreadyGuessed(progress[i].getText().charAt(0)))
            progress[i].setFill(Color.RED);
        }
        setGameState(GameState.ENDED);
        appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
        Platform.runLater(() -> {
            PropertyManager           manager    = PropertyManager.getManager();
            AppMessageDialogSingleton dialog     = AppMessageDialogSingleton.getSingleton();
            String                    endMessage = manager.getPropertyValue(success ? GAME_WON_MESSAGE : GAME_LOST_MESSAGE);

            if (dialog.isShowing())
                dialog.toFront();
            else
                dialog.show(manager.getPropertyValue(GAME_OVER_TITLE), endMessage);
        });
    }

    private void initWordGraphics(HBox guessedLetters) {
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];

        for (int i = 0; i < progress.length; i++) {
            StackPane stackPane = new StackPane();
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
            Rectangle rectangle = new Rectangle(20.0,20.0);
            rectangle.setFill(Paint.valueOf("white"));
            StackPane.setMargin(progress[i],new Insets(10.0,10.0,10.0,10.0));
            stackPane.getChildren().addAll(rectangle,progress[i]);
            guessedLetters.getChildren().add(i,stackPane);
        }

    }

    public void play() {
        disableGameButton();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {

                appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {

                    char guess = event.getCharacter().charAt(0);

                    if(Character.isLetter(guess)&& Character.isLowerCase(guess))
                    {
                    if (!alreadyGuessed(guess)) {
                        boolean goodguess = false;
                        for (int i = 0; i < progress.length; i++) {
                            if (gamedata.getTargetWord().charAt(i) == guess) {
                                progress[i].setVisible(true);
                                gamedata.addGoodGuess(guess);
                                goodguess = true;
                                discovered++;

                            }
                        }
                        if (!goodguess) {
                                hangmanProgress.get(0).setVisible(true);
                                hangmanProgress.remove(0);
                                gamedata.addBadGuess(guess);

                        }

                        int index = (int)guess - 97;

                        StackPane fill = new StackPane();
                        Rectangle recta = new Rectangle(30.0,30.0,Color.RED);

                        fill.setStyle("-fx-padding: 5;");
                        fill.getChildren().addAll(recta,new Text(Character.toString(guess)));
                        GuessedLettersFlowPane.getChildren().remove(index);
                        GuessedLettersFlowPane.getChildren().add(index,fill);

                        success = (discovered == progress.length);
                        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                        if(gamedata.getRemainingGuesses() == 1 || gamedata.getTargetWord().length()-discovered == 1)
                            disableHintButton();
                    }}
                    setGameState(GameState.INITIALIZED_MODIFIED);
                });
                if (gamedata.getRemainingGuesses() <= 0 || success)
                    stop();
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }

    private void restoreGUI() {
        disableGameButton();
        if(hintUseState = true)
            enableHintButton();
        else
            disableHintButton();

        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gameWorkspace.reinitialize();

        HBox guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        restoreWordGraphics(guessedLetters);

        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        makeHangmanCanvas();
        restoreHangmanGraphics();
        makeGuessedLetters();
        restoreGuessedLetters();

        remains = new Label(Integer.toString(gamedata.getRemainingGuesses()));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);

        success = false;
        play();
    }

    private void restoreWordGraphics(HBox guessedLetters) {
        discovered = 0;
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            StackPane stackPane = new StackPane();
            Rectangle rectangle = new Rectangle(20.0,20.0);
            rectangle.setFill(Paint.valueOf("white"));
            StackPane.setMargin(progress[i],new Insets(10.0,10.0,10.0,10.0));
            stackPane.getChildren().addAll(rectangle,progress[i]);
            guessedLetters.getChildren().add(i,stackPane);
            if (progress[i].isVisible())
                discovered++;
        }
    }

    public void restoreHangmanGraphics()
    {
       int badGuesses = gamedata.getBadGuesses().size();
        for(int i = 0;i<badGuesses;i++)
        {
            hangmanProgress.get(i).setVisible(true);
        }
    }

    public void restoreGuessedLetters()
    {
        for(int i =0;i<alphabetCharArray.length;i++) {
            if(alreadyGuessed(alphabetCharArray[i])) {
                char letterGuessed = alphabetCharArray[i];
                StackPane fill = new StackPane();
                Rectangle recta = new Rectangle(30.0, 30.0, Color.RED);

                fill.setStyle("-fx-padding: 5;");
                fill.getChildren().addAll(recta, new Text(Character.toString(letterGuessed)));
                int index = letterGuessed - 97;
                GuessedLettersFlowPane.getChildren().remove(index);
                GuessedLettersFlowPane.getChildren().add(index, fill);
            }
        }
    }

    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
        PropertyManager           propertyManager = PropertyManager.getManager();
        boolean                   makenew         = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file
            ((Workspace) appTemplate.getWorkspaceComponent()).reinitialize();
            enableGameButton();
            disableHintButton();
        }
        if (gamestate.equals(GameState.ENDED)) {
            appTemplate.getGUI().updateWorkspaceToolbar(false);
            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
        }

    }

    @Override
    public void handleSaveRequest() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        if (workFile == null) {
            FileChooser filechooser = new FileChooser();
            Path        appDirPath  = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path        targetPath  = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null)
                save(selectedFile.toPath());
        } else
            save(workFile);
    }

    @Override
    public void handleLoadRequest() throws IOException {
        boolean load = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            load = promptToSave();
        if (load) {
            PropertyManager propertyManager = PropertyManager.getManager();
            FileChooser     filechooser     = new FileChooser();
            Path            appDirPath      = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path            targetPath      = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(LOAD_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showOpenDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null && selectedFile.exists())
                load(selectedFile.toPath());
            else
                return;
            restoreGUI(); // restores the GUI to reflect the state in which the loaded game was last saved
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException {
        PropertyManager            propertyManager   = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();

        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                               propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES))
            handleSaveRequest();

        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {
        appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), target);
        workFile = target;
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(SAVE_COMPLETED_TITLE), props.getPropertyValue(SAVE_COMPLETED_MESSAGE));
    }

    /**
     * A helper method to load saved game data. It loads the game data, notified the user, and then updates the GUI to
     * reflect the correct state of the game.
     *
     * @param source The source data file from which the game is loaded.
     * @throws IOException
     */
    private void load(Path source) throws IOException {
        // load game data
        appTemplate.getFileComponent().loadData(appTemplate.getDataComponent(), source);

        // set the work file as the file from which the game was loaded
        workFile = source;

        // notify the user that load was successful
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(LOAD_COMPLETED_TITLE), props.getPropertyValue(LOAD_COMPLETED_MESSAGE));

        setGameState(GameState.INITIALIZED_UNMODIFIED);
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
        ensureActivatedWorkspace();
        gameworkspace.reinitialize();
        gamedata = (GameData) appTemplate.getDataComponent();
    }


}
