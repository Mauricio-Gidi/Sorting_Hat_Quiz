// SortingHatApp.java

import config.Config;
import controller.MainController;
import model.SortingHatModel;
import view.MainWindow;
import javax.swing.SwingUtilities;

/**
 * Application entry point for the Sorting Hat quiz.
 * 
 * This class wires together configuration, the domain model, and the Swing UI, and starts the
 * application on the Swing Event Dispatch Thread (EDT).
 * 
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class SortingHatApp
{
    /**
     * Prevents instantiation of this utility-style entry point class.
     */
    private SortingHatApp(){}

    /**
     * Launches the Sorting Hat application.
     * 
     * The UI is initialized on the Swing Event Dispatch Thread (EDT) using
     * SwingUtilities.invokeLater(Runnable).
     */
    public static void main(String[] args)
    {
        // Launch UI initialization on the Swing Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() ->
        {
            SortingHatModel model = new SortingHatModel(
                    Config.TRAIT_TO_HOUSE_WEIGHTS_PATH,
                    Config.LIKERT_ITEMS_PATH,
                    Config.FORCED_CHOICE_ITEMS_PATH
            );

            MainWindow window = new MainWindow();
            MainController controller = new MainController(model, window);
            controller.start();
        });
    }
}
