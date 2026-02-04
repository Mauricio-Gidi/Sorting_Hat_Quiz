// Screens.java

package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;


/**
 * Provides shared UI helpers used by screen panels.
 * This utility class centralizes small Swing helper methods used by the
 * card-based screens in the main window.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class Screens
{
    /**
     * Prevents instantiation of this utility class.
     */
    private Screens(){}

    static final Runnable NO_OP = () -> {};
    static final ActionListener NO_OP_ACTION = e -> {};

    /**
     * Creates a transparent vertical content column for card screens.
     * The returned panel uses a Y-axis BoxLayout and theme padding.
     *
     * @return A JPanel configured for card content layout.
     */
    static JPanel createCardContentColumn()
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(
                SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_32_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_32_PIXELS
        ));
        return panel;
    }

    /**
     * Creates default GridBagConstraints that fill available space.
     * The constraints are configured to expand in both directions with theme insets.
     *
     * @return A GridBagConstraints instance suitable for full-panel fill.
     */
    static GridBagConstraints createFillConstraints()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS
        );
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        return c;
    }

    /**
     * Requests focus for the given component on the Swing event queue.
     * If the component is null, this method does nothing.
     *
     * @param component Component to receive focus, or null to skip.
     */
    static void focusLater(JComponent component)
    {
        if (component == null) return;
        SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    /**
     * Ensures the provided text is returned as centered HTML.
     * If the input already starts with an <html> tag (case-insensitive), it is
     * returned unchanged. Otherwise, the text is HTML-escaped, newlines are
     * converted to <br>, and the result is wrapped in a centered HTML container.
     *
     * @param htmlOrPlainText Text to normalize into centered HTML.
     * @return A centered HTML string suitable for Swing label rendering.
     * @throws NullPointerException If htmlOrPlainText is null.
     */
    static String ensureCenteredHtml(String htmlOrPlainText)
    {
        String text = Objects.requireNonNull(htmlOrPlainText, "htmlOrPlainText").trim();
        if (text.toLowerCase(Locale.ROOT).startsWith("<html")) return text;
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String escaped = escapeHtml(normalized).replace("\n", "<br>");
        return "<html><div style='text-align:center;'>" + escaped + "</div></html>";
    }

    /**
     * Escapes a subset of characters for safe HTML display.
     * This method replaces ampersands and angle brackets with their HTML entities.
     *
     * @param plainText Raw text to escape for HTML.
     * @return A string with basic HTML escaping applied.
     */
    private static String escapeHtml(String plainText)
    {
        return plainText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Creates a read-only, word-wrapped text area for display-only content.
     * The text area is transparent, non-editable, and configured to wrap at word
     * boundaries. The provided font and color may be null.
     *
     * @param font Optional font to apply, or null to keep the default.
     * @param color Optional foreground color to apply, or null to keep the default.
     * @return A configured JTextArea suitable for read-only display.
     */
    static JTextArea createReadOnlyWrappedTextArea(Font font, Color color)
    {
        JTextArea area = new JTextArea();
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        if (font != null) area.setFont(font);
        if (color != null) area.setForeground(color);
        return area;
    }
}

/**
 * Defines a common base panel for card-style screens.
 * Subclasses render their content inside a decorated card container and
 * share a consistent layout and padding.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
abstract class CardScreenPanel extends JPanel
{
    protected final DecoratedCardPanel card;
    protected final JPanel content;

    /**
     * Initializes the base card screen container.
     * Creates the decorated card panel, a content column, and adds the card
     * using fill constraints so it expands within this panel.
     */
    protected CardScreenPanel()
    {
        setOpaque(false);
        setLayout(new GridBagLayout());
        this.card = new DecoratedCardPanel();
        this.card.setOpaque(false);
        this.card.setLayout(new BorderLayout());
        this.content = Screens.createCardContentColumn();
        this.card.add(this.content, BorderLayout.CENTER);
        add(this.card, Screens.createFillConstraints());
    }
}

/**
 * Displays the welcome screen before the quiz begins.
 * This panel shows the hat icon, a title/subtitle, and a Continue button.
 * It can notify a listener exactly once when the user proceeds.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class WelcomeScreenPanel extends CardScreenPanel
{
    private final ImagePanel hatIconPanel;
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final ThemedPrimaryButton continueButton;
    private Runnable finishedListener = Screens.NO_OP;
    private boolean finishedAlready = false;

    /**
     * Constructs the welcome screen with default text and actions.
     * Initializes labels and the Continue button, assembles the layout, and installs
     * an Enter key binding to advance.
     */
    WelcomeScreenPanel()
    {
        this.hatIconPanel = new ImagePanel(SortingHatTheme.ImageAssets.SORTING_HAT_ICON_IMAGE);
        this.hatIconPanel.setPreferredSize(new Dimension(140, 140));
        this.hatIconPanel.setMaximumSize(new Dimension(160, 160));
        this.hatIconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.titleLabel = new JLabel("", SwingConstants.CENTER);
        this.titleLabel.setOpaque(false);
        this.titleLabel.setFont(SortingHatTheme.Fonts.TITLE_FONT);
        this.titleLabel.setForeground(SortingHatTheme.Colors.TEXT_PRIMARY);
        this.titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.subtitleLabel = new JLabel("", SwingConstants.CENTER);
        this.subtitleLabel.setOpaque(false);
        this.subtitleLabel.setFont(SortingHatTheme.Fonts.BODY_FONT);
        this.subtitleLabel.setForeground(SortingHatTheme.Colors.TEXT_SECONDARY);
        this.subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.continueButton = SortingHatTheme.createPrimaryButton("Continue");
        this.continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.continueButton.addActionListener(e -> finishNow());
        assemble();
        displayWelcomeTitle("<html><div style='text-align:center;'>Welcome to the Sorting Hat</div></html>");
        displaySubtitleText("The Hat sees beyond mere words.");
        resetState();
        SortingHatTheme.installKeyBinding(
                this,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "welcomeContinue",
                this::finishNow
        );
    }

    /**
     * Sets the callback invoked when the user finishes the welcome screen.
     * If the provided listener is null, a no-op callback is used.
     *
     * @param welcomeScreenFinishedListener Runnable invoked after the user clicks Continue.
     */
    public void setWelcomeScreenFinishedListener(Runnable welcomeScreenFinishedListener)
    {
        this.finishedListener = (welcomeScreenFinishedListener != null) ? welcomeScreenFinishedListener : Screens.NO_OP;
    }

    /**
     * Updates the welcome title text.
     * The value is normalized to centered HTML before being applied.
     *
     * @param welcomeTitleHtmlOrPlainText Title text as HTML or plain text.
     * @throws NullPointerException If welcomeTitleHtmlOrPlainText is null.
     */
    public void displayWelcomeTitle(String welcomeTitleHtmlOrPlainText)
    {
        this.titleLabel.setText(Screens.ensureCenteredHtml(welcomeTitleHtmlOrPlainText));
    }
    
    /**
     * Updates the subtitle text shown under the welcome title.
     * The value is normalized to centered HTML before being applied.
     *
     * @param subtitlePlainText Subtitle text as plain text or HTML.
     * @throws NullPointerException If subtitlePlainText is null.
     */
    public void displaySubtitleText(String subtitlePlainText)
    {
        this.subtitleLabel.setText(Screens.ensureCenteredHtml(subtitlePlainText));
    }

    /**
     * Resets the screen state and focuses the Continue button.
     * This is used when the welcome screen is shown to the user.
     */
    public void startEntranceAnimation()
    {
        resetState();
        Screens.focusLater(continueButton);
    }

    /**
     * Assembles the Swing component layout for this screen.
     */
    private void assemble()
    {
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        content.add(hatIconPanel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS));
        content.add(subtitleLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(continueButton);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
    }

    /**
     * Restores the initial enabled/visible state for the Continue button.
     */
    private void resetState()
    {
        finishedAlready = false;
        continueButton.setEnabled(true);
        continueButton.setVisible(true);
        revalidate();
        repaint();
    }

    /**
     * Notifies the finished listener once and disables further interaction.
     * Repeated calls are ignored after the first completion.
     */
    private void finishNow()
    {
        if (finishedAlready) return;
        finishedAlready = true;
        continueButton.setEnabled(false);
        finishedListener.run();
    }

    /**
     * Renders a buffered image centered and scaled to fit its bounds.
     * The image is painted with high-quality rendering hints and preserves
     * its aspect ratio while fitting within the component.
     *
     * @author Mauricio Gidi
     * @version Last modified 13_Dec_2025
     */
    private static final class ImagePanel extends JComponent
    {
        private final BufferedImage image;

        /**
         * Creates an image panel for the provided buffered image.
         *
         * @param image Image to paint within the component.
         * @throws NullPointerException If image is null.
         */
        ImagePanel(BufferedImage image)
        {
            this.image = Objects.requireNonNull(image, "image");
            setOpaque(false);
        }

        /**
         * Paints the image scaled and centered within the component bounds.
         *
         * @param graphics Graphics context provided by Swing for painting.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            try
            {
                SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);
                int w = getWidth();
                int h = getHeight();
                int iw = image.getWidth();
                int ih = image.getHeight();
                double scale = Math.min(w / (double) iw, h / (double) ih);
                int dw = (int) Math.round(iw * scale);
                int dh = (int) Math.round(ih * scale);
                int x = (w - dw) / 2;
                int y = (h - dh) / 2;
                g2.drawImage(image, x, y, dw, dh, null);
            }
            finally
            {
                g2.dispose();
            }
        }
    }
}

/**
 * Displays quiz-length mode options and emits the selected mode.
 * This screen presents three selectable tiles (Quick, Standard, Thorough)
 * and notifies a listener when the user confirms a selection.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ModeSelectionScreenPanel extends CardScreenPanel
{
    private final JLabel headerLabel;
    private final JLabel instructionLabel;
    private final JPanel optionsRow;
    private final ModeOptionToggleButton quickButton;
    private final ModeOptionToggleButton standardButton;
    private final ModeOptionToggleButton thoroughButton;
    private final ButtonGroup group;
    private final ThemedPrimaryButton beginButton;
    private ActionListener modeChosenListener = Screens.NO_OP_ACTION;

    /**
     * Constructs the mode selection screen with three selectable options.
     * Initializes the option tiles, selects the Standard mode by default, and installs
     * an Enter key binding to begin.
     */
    ModeSelectionScreenPanel()
    {
        this.headerLabel = SortingHatTheme.createHeaderLabel("Choose your quiz length");
        this.headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.instructionLabel = SortingHatTheme.createCaptionLabel("Pick one. Longer modes ask more questions.");
        this.instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.optionsRow = new JPanel(new GridLayout(
                1,
                3,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS
        ));
        this.optionsRow.setOpaque(false);
        this.optionsRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.quickButton = new ModeOptionToggleButton(
                "Quick",
                "A short sorting. Fast and focused.",
                SortingHatTheme.ImageAssets.QUICK_MODE_ICON,
                MainWindow.QuizLengthMode.QUICK
        );
        this.standardButton = new ModeOptionToggleButton(
                "Standard",
                "Balanced length for most people.",
                SortingHatTheme.ImageAssets.STANDARD_MODE_ICON,
                MainWindow.QuizLengthMode.STANDARD
        );
        this.thoroughButton = new ModeOptionToggleButton(
                "Thorough",
                "More questions for a deeper profile.",
                SortingHatTheme.ImageAssets.THOROUGH_MODE_ICON,
                MainWindow.QuizLengthMode.THOROUGH
        );
        this.group = new ButtonGroup();
        group.add(quickButton);
        group.add(standardButton);
        group.add(thoroughButton);
        standardButton.setSelected(true);
        this.beginButton = SortingHatTheme.createPrimaryButton("Begin");
        this.beginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.beginButton.addActionListener(e -> notifySelection());
        wireSelectionVisualRefresh();
        assemble();
        SortingHatTheme.installKeyBinding(
                this,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "beginQuiz",
                this::notifySelection
        );
    }

    /**
     * Sets the listener invoked when the user confirms a quiz mode selection.
     * If the provided listener is null, a no-op listener is used.
     *
     * @param quizModeSelectionListener Listener receiving an ActionEvent with the selected mode name.
     */
    public void setQuizModeSelectionListener(ActionListener quizModeSelectionListener)
    {
        this.modeChosenListener = (quizModeSelectionListener != null) ? quizModeSelectionListener : Screens.NO_OP_ACTION;
    }

    /**
     * Requests focus for the default mode control on the event queue.
     */
    public void requestFocusForDefaultControl()
    {
        Screens.focusLater(standardButton);
    }

    /**
     * Assembles the component layout for the selection screen.
     */
    private void assemble()
    {
        content.add(headerLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        content.add(instructionLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        optionsRow.add(quickButton);
        optionsRow.add(standardButton);
        optionsRow.add(thoroughButton);
        content.add(optionsRow);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(beginButton);
    }

    /**
     * Installs UI listeners that refresh painting and handle double-click selection.
     */
    private void wireSelectionVisualRefresh()
    {
        ActionListener repaintOnChange = e -> repaint();
        quickButton.addActionListener(repaintOnChange);
        standardButton.addActionListener(repaintOnChange);
        thoroughButton.addActionListener(repaintOnChange);
        installDoubleClickToBegin(quickButton);
        installDoubleClickToBegin(standardButton);
        installDoubleClickToBegin(thoroughButton);
    }

    /**
     * Adds a mouse listener that triggers selection on a left-button double-click.
     *
     * @param b Button to attach the double-click handler to.
     */
    private void installDoubleClickToBegin(AbstractButton b)
    {
        b.addMouseListener(new MouseAdapter()
        {
            /**
             * Handles mouse clicks and triggers selection on a left-button double-click.
             *
             * @param e Mouse event received from Swing.
             */

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2)
                {
                    notifySelection();
                }
            }
        });
    }

    /**
     * Emits the currently selected mode to the configured listener.
     * If no mode is selected, this method returns without notifying.
     */
    private void notifySelection()
    {
        MainWindow.QuizLengthMode selected = getSelectedModeOrNull();
        if (selected == null) return;
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected.name());
        modeChosenListener.actionPerformed(event);
    }

    /**
     * Returns the selected quiz length mode, or null if nothing is selected.
     *
     * @return The selected mode, or null when no button is selected.
     */
    private MainWindow.QuizLengthMode getSelectedModeOrNull()
    {
        if (quickButton.isSelected()) return quickButton.getQuizLengthMode();
        if (standardButton.isSelected()) return standardButton.getQuizLengthMode();
        if (thoroughButton.isSelected()) return thoroughButton.getQuizLengthMode();
        return null;
    }

    /**
     * Implements a custom painted toggle tile for a quiz-length mode.
     * The tile draws its background, border, icon, and text using the app theme
     * and shows a badge when selected.
     *
     * @author Mauricio Gidi
     * @version Last modified 13_Dec_2025
     */
    private static final class ModeOptionToggleButton extends JToggleButton
    {
        private static final int TILE_CORNER_RADIUS_PIXELS = 18;
        private static final int TILE_INTERNAL_PADDING_PIXELS = 16;
        private static final int ICON_MAXIMUM_SIZE_PIXELS = 72;
        private static final int CHECK_BADGE_DIAMETER_PIXELS = 18;
        private final String titleText;
        private final String descriptionText;
        private final ImageIcon modeIcon;
        private final MainWindow.QuizLengthMode quizLengthMode;
        private boolean hover = false;

        /**
         * Creates a toggle tile representing a quiz-length mode option.
         *
         * @param titleText Title shown at the top of the tile.
         * @param descriptionText Secondary description text shown under the title.
         * @param modeIcon Optional icon to draw above the title, or null for none.
         * @param quizLengthMode Mode value associated with this tile.
         * @throws NullPointerException If titleText, descriptionText, or quizLengthMode is null.
         */
        ModeOptionToggleButton(
                String titleText,
                String descriptionText,
                ImageIcon modeIcon,
                MainWindow.QuizLengthMode quizLengthMode
        )
        {
            this.titleText = Objects.requireNonNull(titleText, "titleText");
            this.descriptionText = Objects.requireNonNull(descriptionText, "descriptionText");
            this.modeIcon = modeIcon;
            this.quizLengthMode = Objects.requireNonNull(quizLengthMode, "quizLengthMode");
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            getAccessibleContext().setAccessibleName(titleText + " mode");
            getAccessibleContext().setAccessibleDescription(descriptionText);
            setPreferredSize(new Dimension(260, 220));
            addMouseListener(new MouseAdapter()
            {

                /**
                 * Marks the tile as hovered and repaints it.
                 *
                 * @param e Mouse event received from Swing.
                 */
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    hover = true;
                    repaint();
                }

                /**
                 * Clears the hover state and repaints the tile.
                 *
                 * @param e Mouse event received from Swing.
                 */
                @Override
                public void mouseExited(MouseEvent e)
                {
                    hover = false;
                    repaint();
                }
            });
        }

        /**
         * Returns the mode associated with this tile.
         *
         * @return The quiz length mode represented by this button.
         */
        public MainWindow.QuizLengthMode getQuizLengthMode()
        {
            return quizLengthMode;
        }

        /**
         * Paints the tile using custom background, border, and content rendering.
         *
         * @param graphics Graphics context provided by Swing for painting.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try
            {
                SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);
                int w = getWidth();
                int h = getHeight();
                paintBackground(g2, w, h);
                paintBorder(g2, w, h);
                paintContents(g2, w, h);
            }
            finally
            {
                g2.dispose();
            }
        }

        /**
         * Paints the rounded background gradient for the tile.
         *
         * @param g2 Graphics2D context used for rendering.
         * @param w Tile width in pixels.
         * @param h Tile height in pixels.
         */
        private void paintBackground(Graphics2D g2, int w, int h)
        {
            Shape tile = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, TILE_CORNER_RADIUS_PIXELS, TILE_CORNER_RADIUS_PIXELS);
            Color top = new Color(18, 18, 18, 180);
            Color bottom = new Color(10, 10, 10, 210);
            if (isSelected())
            {
                top = new Color(35, 30, 20, 200);
                bottom = new Color(18, 14, 10, 230);
            }
            else if (hover)
            {
                top = new Color(25, 25, 25, 190);
                bottom = new Color(12, 12, 12, 220);
            }
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fill(tile);
            g2.setColor(new Color(255, 255, 255, isSelected() ? 35 : 22));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(
                    2.5f, 2.5f, w - 5f, h - 5f,
                    TILE_CORNER_RADIUS_PIXELS - 2, TILE_CORNER_RADIUS_PIXELS - 2
            ));
        }

        /**
         * Paints the tile border, emphasizing selection and hover state.
         *
         * @param g2 Graphics2D context used for rendering.
         * @param w Tile width in pixels.
         * @param h Tile height in pixels.
         */
        private void paintBorder(Graphics2D g2, int w, int h)
        {
            Shape tile = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, TILE_CORNER_RADIUS_PIXELS, TILE_CORNER_RADIUS_PIXELS);
            if (isSelected())
            {
                g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(tile);
                g2.setColor(new Color(
                        SortingHatTheme.Colors.ACCENT_GOLD.getRed(),
                        SortingHatTheme.Colors.ACCENT_GOLD.getGreen(),
                        SortingHatTheme.Colors.ACCENT_GOLD.getBlue(),
                        55
                ));
                g2.setStroke(new BasicStroke(6f));
                g2.draw(tile);
            }
            else
            {
                g2.setColor(new Color(255, 255, 255, hover ? 55 : 35));
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(tile);
            }
            if (isFocusOwner())
            {
                g2.setColor(SortingHatTheme.Colors.FOCUS_RING_SOFT);
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new RoundRectangle2D.Float(
                        2.5f, 2.5f, w - 5f, h - 5f,
                        TILE_CORNER_RADIUS_PIXELS + 6, TILE_CORNER_RADIUS_PIXELS + 6
                ));
            }
        }

        /**
         * Paints the tile icon, title, description, and selection badge.
         *
         * @param g2 Graphics2D context used for rendering.
         * @param w Tile width in pixels.
         * @param h Tile height in pixels.
         */
        private void paintContents(Graphics2D g2, int w, int h)
        {
            int left = TILE_INTERNAL_PADDING_PIXELS;
            int right = w - TILE_INTERNAL_PADDING_PIXELS;
            int top = TILE_INTERNAL_PADDING_PIXELS;
            int bottom = h - TILE_INTERNAL_PADDING_PIXELS;
            int contentW = Math.max(1, right - left);
            paintIcon(g2, left, top, contentW);
            paintSelectedBadge(g2, w);
            int titleBaselineY = top + ICON_MAXIMUM_SIZE_PIXELS + 18;
            g2.setFont(SortingHatTheme.Fonts.HEADER_FONT);
            g2.setColor(SortingHatTheme.Colors.TEXT_PRIMARY);
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleX = left + (contentW - fmTitle.stringWidth(titleText)) / 2;
            g2.drawString(titleText, titleX, titleBaselineY);
            g2.setFont(SortingHatTheme.Fonts.CAPTION_FONT);
            g2.setColor(SortingHatTheme.Colors.TEXT_SECONDARY);
            int descTop = titleBaselineY + 14;
            int descHeight = bottom - descTop;
            SortingHatTheme.drawWrappedTextCentered(
                    g2,
                    descriptionText,
                    new Rectangle(left, descTop, contentW, Math.max(1, descHeight)),
                    SortingHatTheme.SpacingAndSizing.CAPTION_FONT_SIZE_PIXELS + 4
            );
        }

        /**
         * Paints the mode icon centered within the available content width.
         *
         * @param g2 Graphics2D context used for rendering.
         * @param left Left pixel coordinate of the content area.
         * @param top Top pixel coordinate where the icon should be drawn.
         * @param contentW Width of the content area in pixels.
         */
        private void paintIcon(Graphics2D g2, int left, int top, int contentW)
        {
            if (modeIcon == null) return;
            Image img = modeIcon.getImage();
            if (img == null) return;
            int drawW = Math.min(ICON_MAXIMUM_SIZE_PIXELS, contentW);
            int drawH = ICON_MAXIMUM_SIZE_PIXELS;
            int x = left + (contentW - drawW) / 2;
            int y = top;
            g2.drawImage(img, x, y, drawW, drawH, null);
        }

        /**
         * Paints a check-mark badge when the tile is selected.
         *
         * @param g2 Graphics2D context used for rendering.
         * @param widthPixels Total tile width in pixels.
         */
        private void paintSelectedBadge(Graphics2D g2, int widthPixels)
        {
            if (!isSelected()) return;
            int pad = 12;
            int x = widthPixels - pad - CHECK_BADGE_DIAMETER_PIXELS;
            int y = pad;
            g2.setColor(new Color(
                    SortingHatTheme.Colors.ACCENT_CYAN.getRed(),
                    SortingHatTheme.Colors.ACCENT_CYAN.getGreen(),
                    SortingHatTheme.Colors.ACCENT_CYAN.getBlue(),
                    60
            ));
            g2.fillOval(x - 4, y - 4, CHECK_BADGE_DIAMETER_PIXELS + 8, CHECK_BADGE_DIAMETER_PIXELS + 8);
            g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
            g2.fillOval(x, y, CHECK_BADGE_DIAMETER_PIXELS, CHECK_BADGE_DIAMETER_PIXELS);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(x + 5, y + 9,  x + 8,  y + 13);
            g2.drawLine(x + 8, y + 13, x + 14, y + 6);
        }
    }
}

/**
 * Displays a Likert-style question with a slider-based response.
 * The panel shows progress, renders the current question text, and submits
 * a numeric score when the user clicks Next or presses Enter.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class LikertQuestionScreenPanel extends CardScreenPanel
{
    private final JLabel headerLabel;
    private final JLabel progressTextLabel;
    private final ThemedProgressIndicator progressIndicator;
    private final JTextArea questionTextArea;
    private final JSlider likertSlider;
    private final ThemedPrimaryButton nextButton;
    private ActionListener submitListener = Screens.NO_OP_ACTION;

    /**
     * Constructs the Likert question screen and initializes controls.
     * Initializes the progress header, question text area, slider, and Next button,
     * then resets the screen for the first question.
     */
    LikertQuestionScreenPanel()
    {
        this.headerLabel = SortingHatTheme.createHeaderLabel("Question");
        this.headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.progressTextLabel = SortingHatTheme.createCaptionLabel("Question 1 of 1");
        this.progressTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.progressIndicator = new ThemedProgressIndicator();
        this.progressIndicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.questionTextArea = Screens.createReadOnlyWrappedTextArea(
                SortingHatTheme.Fonts.BODY_FONT.deriveFont(22f),
                SortingHatTheme.Colors.TEXT_PRIMARY
        );
        this.questionTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.likertSlider = SortingHatTheme.createLikertSlider();
        this.nextButton = SortingHatTheme.createPrimaryButton("Next");
        this.nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.nextButton.addActionListener(e -> submitNow());
        assemble();
        installEnterToSubmit();
        resetForNextQuestion();
    }

    /**
     * Sets the listener invoked when the user submits a Likert answer.
     * If the provided listener is null, a no-op listener is used.
     *
     * @param likertAnswerSubmissionListener Listener receiving an ActionEvent whose command is the score.
     */
    public void setLikertAnswerSubmissionListener(ActionListener likertAnswerSubmissionListener)
    {
        this.submitListener = (likertAnswerSubmissionListener != null) ? likertAnswerSubmissionListener : Screens.NO_OP_ACTION;
    }

    /**
     * Displays the given Likert question and progress information.
     * This updates the question text, the progress label, and the progress indicator
     * and then resets the controls for answering.
     *
     * @param questionText Question prompt to display.
     * @param questionIndexOneBased 1-based index of the current question.
     * @param totalQuestionCount Total number of questions in the quiz.
     * @throws NullPointerException If questionText is null.
     * @throws IllegalArgumentException If totalQuestionCount is < 1 or the index is out of range.
     */
    public void displayQuestion(String questionText, int questionIndexOneBased, int totalQuestionCount)
    {
        Objects.requireNonNull(questionText, "questionText");
        if (totalQuestionCount < 1) throw new IllegalArgumentException("totalQuestionCount must be >= 1");
        if (questionIndexOneBased < 1 || questionIndexOneBased > totalQuestionCount)
        {
            throw new IllegalArgumentException("questionIndexOneBased must be within [1, totalQuestionCount]");
        }
        questionTextArea.setText(questionText);
        progressTextLabel.setText("Question " + questionIndexOneBased + " of " + totalQuestionCount);
        progressIndicator.setProgressSteps(questionIndexOneBased, totalQuestionCount);
        resetForNextQuestion();
    }

    /**
     * Requests focus for the slider control on the event queue.
     */
    public void requestFocusForDefaultControl()
    {
        Screens.focusLater(likertSlider);
    }

    /**
     * Resets the slider and enables the Next button for a new response.
     */
    public void resetForNextQuestion()
    {
        likertSlider.setValue(30);
        nextButton.setEnabled(true);
        revalidate();
        repaint();
    }

    /**
     * Assembles the Swing component layout for this question screen.
     */
    private void assemble()
    {
        JPanel headerBlock = new JPanel();
        headerBlock.setOpaque(false);
        headerBlock.setLayout(new BoxLayout(headerBlock, BoxLayout.Y_AXIS));
        headerBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerBlock.add(headerLabel);
        headerBlock.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        headerBlock.add(progressTextLabel);
        headerBlock.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        headerBlock.add(progressIndicator);
        content.add(headerBlock);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(questionTextArea);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(likertSlider);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(nextButton);
    }

    /**
     * Installs an Enter key binding that submits the current answer when enabled.
     */
    private void installEnterToSubmit()
    {
        SortingHatTheme.installKeyBinding(
                this,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "submitLikert",
                () -> {
                    if (nextButton.isEnabled()) submitNow();
                }
        );
    }

    /**
     * Computes the current Likert score and notifies the submission listener.
     */
    private void submitNow()
    {
        nextButton.setEnabled(false);
        double score = SortingHatTheme.convertLikertSliderValueToLikertScore(likertSlider);
        submitListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Double.toString(score)));
    }
}

/**
 * Displays a forced-choice scenario with two selectable options.
 * The panel can switch between question and interlude modes and submits the
 * selected option key when the user chooses an option.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ForcedChoiceQuestionScreenPanel extends CardScreenPanel
{
    private static final String CARD_PROMPT = "prompt";
    private static final String CARD_INTERLUDE = "interlude";
    private final JLabel headerLabel;
    private final JLabel progressTextLabel;
    private final ThemedProgressIndicator progressIndicator;
    private final JTextArea promptTextArea;
    private final JLabel interludeLabel;
    private final CardLayout messageCardLayout = new CardLayout();
    private final JPanel messageCardPanel = new JPanel(messageCardLayout);
    private final JLabel instructionLabel;
    private final OptionTileButton leftTile;
    private final OptionTileButton rightTile;
    private final JComponent tilesRow;
    private ActionListener submitListener = Screens.NO_OP_ACTION;

    /**
     * Constructs the forced-choice question screen and initializes controls.
     * Initializes the prompt/interlude card area, option tiles, and layout. An internal
     * listener forwards tile selections to the configured submission listener.
     */
    ForcedChoiceQuestionScreenPanel()
    {
        this.headerLabel = SortingHatTheme.createHeaderLabel("Scenario");
        this.headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.progressTextLabel = SortingHatTheme.createCaptionLabel("Scenario 1 of 1");
        this.progressTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.progressIndicator = new ThemedProgressIndicator();
        this.progressIndicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.promptTextArea = Screens.createReadOnlyWrappedTextArea(
                SortingHatTheme.Fonts.BODY_FONT.deriveFont(22f),
                SortingHatTheme.Colors.TEXT_PRIMARY
        );
        this.promptTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.interludeLabel = new JLabel("", SwingConstants.CENTER);
        this.interludeLabel.setOpaque(false);
        this.interludeLabel.setForeground(SortingHatTheme.Colors.TEXT_PRIMARY);
        this.interludeLabel.setFont(SortingHatTheme.Fonts.TITLE_FONT.deriveFont(Font.BOLD, 30f));
        this.interludeLabel.setVerticalAlignment(SwingConstants.CENTER);
        this.messageCardPanel.setOpaque(false);
        this.messageCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.messageCardPanel.add(this.promptTextArea, CARD_PROMPT);
        this.messageCardPanel.add(this.interludeLabel, CARD_INTERLUDE);
        this.instructionLabel = SortingHatTheme.createCaptionLabel("Choose the option that feels more like you.");
        this.instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.leftTile = new OptionTileButton();
        this.rightTile = new OptionTileButton();
        ActionListener optionChosen = new ActionListener()
        {

            /**
             * Handles a tile selection by validating the action command and submitting it.
             *
             * @param e Action event fired by an option tile.
             */
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String key = (e == null) ? null : e.getActionCommand();
                if (key == null || key.trim().isEmpty()) return;
                submitNow(key);
            }
        };
        this.leftTile.setOptionSelectedListener(optionChosen);
        this.rightTile.setOptionSelectedListener(optionChosen);
        this.tilesRow = createTilesRow();
        assemble();
        setInterludeMode(false);
    }

    /**
     * Sets the listener invoked when the user submits a forced-choice answer.
     * If the provided listener is null, a no-op listener is used.
     *
     * @param forcedChoiceAnswerSubmissionListener Listener receiving an ActionEvent whose command is the selected option key.
     */
    public void setForcedChoiceAnswerSubmissionListener(ActionListener forcedChoiceAnswerSubmissionListener)
    {
        this.submitListener = (forcedChoiceAnswerSubmissionListener != null)
                ? forcedChoiceAnswerSubmissionListener
                : Screens.NO_OP_ACTION;
    }

    /**
     * Displays a forced-choice scenario and its two options.
     * This method updates the prompt text, progress indicators, option tiles, and
     * ensures the screen is in question mode before enabling interaction.
     *
     * @param promptText Scenario prompt to display.
     * @param optionKeyLeft Key identifying the left option.
     * @param optionTextLeft Display text for the left option.
     * @param optionKeyRight Key identifying the right option.
     * @param optionTextRight Display text for the right option.
     * @param questionIndexOneBased 1-based index of the current scenario.
     * @param totalQuestionCount Total number of scenarios in the quiz.
     * @throws NullPointerException If any prompt/option key/option text parameter is null.
     * @throws IllegalArgumentException If totalQuestionCount is < 1 or the index is out of range.
     */
    public void displayQuestion(
            String promptText,
            String optionKeyLeft,
            String optionTextLeft,
            String optionKeyRight,
            String optionTextRight,
            int questionIndexOneBased,
            int totalQuestionCount
    )
    {
        Objects.requireNonNull(promptText, "promptText");
        Objects.requireNonNull(optionKeyLeft, "optionKeyLeft");
        Objects.requireNonNull(optionTextLeft, "optionTextLeft");
        Objects.requireNonNull(optionKeyRight, "optionKeyRight");
        Objects.requireNonNull(optionTextRight, "optionTextRight");
        if (totalQuestionCount < 1) throw new IllegalArgumentException("totalQuestionCount must be >= 1");
        if (questionIndexOneBased < 1 || questionIndexOneBased > totalQuestionCount)
        {
            throw new IllegalArgumentException("questionIndexOneBased must be within [1, totalQuestionCount]");
        }
        setInterludeMode(false);
        headerLabel.setText("Scenario");
        promptTextArea.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(22f));
        promptTextArea.setText(promptText);
        progressTextLabel.setText("Scenario " + questionIndexOneBased + " of " + totalQuestionCount);
        progressIndicator.setProgressSteps(questionIndexOneBased, totalQuestionCount);
        leftTile.setOption(optionKeyLeft, optionTextLeft);
        rightTile.setOption(optionKeyRight, optionTextRight);
        resetForNextQuestion();
        messageCardLayout.show(messageCardPanel, CARD_PROMPT);
        requestFocusForDefaultControl();
    }

    /**
     * Shows an interlude message in place of the question UI.
     *
     * @param messageText Message text to display during the interlude.
     * @throws NullPointerException If messageText is null.
     */
    public void displayInterlude(String messageText)
    {
        setInterludeMode(true);
        interludeLabel.setText(Screens.ensureCenteredHtml(messageText));
        messageCardLayout.show(messageCardPanel, CARD_INTERLUDE);
        revalidate();
        repaint();
    }

    /**
     * Requests focus for the left option tile on the event queue.
     */
    public void requestFocusForDefaultControl()
    {
        Screens.focusLater(leftTile);
    }

    /**
     * Enables both option tiles and refreshes the layout for a new scenario.
     */
    public void resetForNextQuestion()
    {
        leftTile.setEnabled(true);
        rightTile.setEnabled(true);
        revalidate();
        repaint();
    }

    /**
     * Assembles the Swing component layout for this screen.
     */
    private void assemble()
    {
        JPanel headerBlock = new JPanel();
        headerBlock.setOpaque(false);
        headerBlock.setLayout(new BoxLayout(headerBlock, BoxLayout.Y_AXIS));
        headerBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerBlock.add(headerLabel);
        headerBlock.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        headerBlock.add(progressTextLabel);
        headerBlock.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        headerBlock.add(progressIndicator);
        content.add(headerBlock);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(messageCardPanel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS));
        content.add(instructionLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS));
        content.add(tilesRow);
    }

    /**
     * Creates the row container that holds the left and right option tiles.
     *
     * @return A component containing the two option tiles laid out side-by-side.
     */
    private JComponent createTilesRow()
    {
        JPanel row = new JPanel(new GridLayout(
                1,
                2,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                0
        ));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(leftTile);
        row.add(rightTile);
        return row;
    }

    /**
     * Disables option tiles and notifies the submission listener with the chosen key.
     *
     * @param selectedOptionKey Option key chosen by the user.
     */
    private void submitNow(String selectedOptionKey)
    {
        leftTile.setEnabled(false);
        rightTile.setEnabled(false);
        submitListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selectedOptionKey));
    }

    /**
     * Toggles visibility and enablement for interlude versus question mode.
     *
     * @param interlude True to show the interlude view; false to show the question UI.
     */
    private void setInterludeMode(boolean interlude)
    {
        headerLabel.setVisible(!interlude);
        progressTextLabel.setVisible(!interlude);
        progressIndicator.setVisible(!interlude);
        instructionLabel.setVisible(!interlude);
        tilesRow.setVisible(!interlude);
        if (interlude)
        {
            leftTile.setEnabled(false);
            rightTile.setEnabled(false);
        }
    }
}

/**
 * Displays the quiz outcome and supporting probability chart.
 * This screen shows the winning house, renders a probability bar chart,
 * and provides actions to view the trait profile or quit.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ResultsScreenPanel extends CardScreenPanel
{
    private final JLabel headerLabel;
    private final JLabel winningHouseLabel;
    private final HouseProbabilityBarChartPanel chartPanel;
    private final ThemedPrimaryButton viewTraitProfileButton;
    private final ThemedPrimaryButton quitButton;
    private Runnable traitProfileListener = Screens.NO_OP;
    private Runnable quitListener = Screens.NO_OP;

    /**
     * Constructs the results screen and initializes disabled action buttons.
     * Buttons are enabled once results are displayed.
     */
    ResultsScreenPanel()
    {
        this.headerLabel = SortingHatTheme.createHeaderLabel("You belong to\u2026");
        this.headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.winningHouseLabel = SortingHatTheme.createTitleLabel("House");
        this.winningHouseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.chartPanel = new HouseProbabilityBarChartPanel();
        this.chartPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.viewTraitProfileButton = SortingHatTheme.createPrimaryButton("View trait profile");
        this.viewTraitProfileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.viewTraitProfileButton.addActionListener(e -> traitProfileListener.run());
        this.quitButton = SortingHatTheme.createPrimaryButton("Quit");
        this.quitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.quitButton.addActionListener(e -> quitListener.run());
        assemble();
        viewTraitProfileButton.setEnabled(false);
        quitButton.setEnabled(false);
    }

    /**
     * Sets the callback invoked when the user requests the trait profile view.
     * If the provided listener is null, a no-op callback is used.
     *
     * @param traitProfileRequestListener Runnable invoked when the user clicks View trait profile.
     */
    public void setTraitProfileRequestListener(Runnable traitProfileRequestListener)
    {
        this.traitProfileListener = (traitProfileRequestListener != null) ? traitProfileRequestListener : Screens.NO_OP;
    }

    /**
     * Sets the callback invoked when the user requests to quit.
     * If the provided listener is null, a no-op callback is used.
     *
     * @param quitRequestListener Runnable invoked when the user clicks Quit.
     */
    public void setQuitRequestListener(Runnable quitRequestListener)
    {
        this.quitListener = (quitRequestListener != null) ? quitRequestListener : Screens.NO_OP;
    }
    
    /**
     * Requests focus for the primary action button on the event queue.
     */
    public void requestFocusForDefaultControl()
    {
        Screens.focusLater(viewTraitProfileButton);
    }

    /**
     * Displays the winning house and updates the probability chart.
     * Enables action buttons after updating the UI.
     *
     * @param winningHouseName Name of the winning house to display.
     * @param houseProbabilitiesByHouseName Mapping of house names to probability values.
     * @throws NullPointerException If winningHouseName or houseProbabilitiesByHouseName is null.
     */
    public void displayResults(String winningHouseName, Map<String, Double> houseProbabilitiesByHouseName)
    {
        Objects.requireNonNull(winningHouseName, "winningHouseName");
        Objects.requireNonNull(houseProbabilitiesByHouseName, "houseProbabilitiesByHouseName");
        winningHouseLabel.setText(winningHouseName);
        Color accent = SortingHatTheme.Colors.getHouseAccentColorByHouseName(winningHouseName);
        winningHouseLabel.setForeground(accent.brighter());
        chartPanel.setHouseProbabilities(houseProbabilitiesByHouseName);
        viewTraitProfileButton.setEnabled(true);
        quitButton.setEnabled(true);
        revalidate();
        repaint();
    }

    /**
     * Opens a modal trait profile dialog for the provided trait scores.
     *
     * @param parentFrame Parent frame used for dialog ownership and centering.
     * @param traitScoresByTraitName Mapping of trait names to trait score values.
     * @throws NullPointerException If parentFrame or traitScoresByTraitName is null.
     */
    public void showTraitProfileDialog(JFrame parentFrame, Map<String, Double> traitScoresByTraitName)
    {
        Objects.requireNonNull(parentFrame, "parentFrame");
        Objects.requireNonNull(traitScoresByTraitName, "traitScoresByTraitName");
        TraitProfileDialog dialog = new TraitProfileDialog(parentFrame, traitScoresByTraitName);
        dialog.setVisible(true);
    }

    /**
     * Assembles the Swing component layout for the results screen.
     */
    private void assemble()
    {
        content.add(headerLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_8_PIXELS));
        content.add(winningHouseLabel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_6_PIXELS));
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_20_PIXELS));
        content.add(chartPanel);
        content.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS));
        content.add(createButtonsRow());
    }

    /**
     * Creates the row of action buttons shown under the chart.
     *
     * @return A component containing the View trait profile and Quit buttons.
     */
    private JComponent createButtonsRow()
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyCompactActionButtonStyle(viewTraitProfileButton);
        applyCompactActionButtonStyle(quitButton);
        row.add(viewTraitProfileButton);
        row.add(quitButton);
        return row;
    }

    /**
     * Applies compact font and margin styling to an action button.
     *
     * @param button Button to update with compact styling.
     */
    private static void applyCompactActionButtonStyle(AbstractButton button)
    {
        button.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(16f));
        button.setMargin(new Insets(6, 14, 6, 14));
    }
}

/**
 * Shows trait scores in a modal dialog.
 * The dialog hosts a scrollable trait profile panel and a Close button.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class TraitProfileDialog extends JDialog
{

    /**
     * Creates and lays out a modal dialog showing trait scores.
     *
     * @param parentFrame Owner frame used for modality and centering.
     * @param traitScoresByTraitName Mapping of trait names to trait score values.
     * @throws NullPointerException If parentFrame or traitScoresByTraitName is null.
     */
    TraitProfileDialog(JFrame parentFrame, Map<String, Double> traitScoresByTraitName)
    {
        super(parentFrame, "Trait Profile", true);
        Objects.requireNonNull(parentFrame, "parentFrame");
        Objects.requireNonNull(traitScoresByTraitName, "traitScoresByTraitName");
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(true);
        root.setBackground(SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM);
        setContentPane(root);
        TraitProfilePanel traitProfilePanel = new TraitProfilePanel(traitScoresByTraitName);
        JScrollPane scroll = new JScrollPane(traitProfilePanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM);
        scroll.setOpaque(true);
        scroll.setBackground(SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM);
        JPanel outerPadding = new JPanel(new BorderLayout());
        outerPadding.setOpaque(false);
        outerPadding.setBorder(new EmptyBorder(
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS
        ));
        outerPadding.add(scroll, BorderLayout.CENTER);
        ThemedPrimaryButton closeButton = SortingHatTheme.createPrimaryButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(closeButton);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(
                SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS,
                SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS
        ));
        south.add(buttonRow, BorderLayout.EAST);
        root.add(outerPadding, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(760, 520));
        setSize(new Dimension(900, 640));
        setLocationRelativeTo(parentFrame);
    }
}

/**
 * Renders a vertical list of trait score bars.
 * Traits are sorted by name and each score is shown relative to the maximum
 * absolute score in the provided map.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class TraitProfilePanel extends JPanel
{

    /**
     * Creates a panel that visualizes trait scores as bars.
     * Trait entries are sorted by trait name (case-insensitive) and scaled relative
     * to the maximum absolute score to keep bars comparable.
     *
     * @param traitScoresByTraitName Mapping of trait names to trait score values.
     * @throws NullPointerException If traitScoresByTraitName is null.
     */
    TraitProfilePanel(Map<String, Double> traitScoresByTraitName)
    {
        Objects.requireNonNull(traitScoresByTraitName, "traitScoresByTraitName");
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel header = SortingHatTheme.createHeaderLabel("Trait Profile");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel caption = SortingHatTheme.createCaptionLabel("Scores are shown relative to the strongest trait.");
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(header);
        add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_6_PIXELS));
        add(caption);
        add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS));
        JPanel bars = new JPanel();
        bars.setOpaque(false);
        bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        bars.setAlignmentX(Component.LEFT_ALIGNMENT);
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(traitScoresByTraitName.entrySet());
        sorted.sort(Comparator.comparing(e -> String.valueOf(e.getKey()), String.CASE_INSENSITIVE_ORDER));
        double maxAbs = computeMaximumAbsoluteValue(sorted);
        for (Map.Entry<String, Double> e : sorted)
        {
            String traitName = String.valueOf(e.getKey());
            double value = (e.getValue() == null) ? 0.0 : e.getValue();
            TraitScoreBarRow row = new TraitScoreBarRow(traitName, value, maxAbs);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            bars.add(row);
            bars.add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS));
        }
        add(bars);
        add(Box.createVerticalStrut(SortingHatTheme.SpacingAndSizing.SPACING_12_PIXELS));
    }

    /**
     * Computes the maximum absolute finite value from the provided entries.
     * Null, NaN, and infinite values are ignored. The returned value is at least 1.0.
     *
     * @param entries Entries to scan for the largest absolute score.
     * @return The maximum absolute finite value, or 1.0 if none are positive.
     */
    private static double computeMaximumAbsoluteValue(List<Map.Entry<String, Double>> entries)
    {
        double maxAbs = 0.0;
        for (Map.Entry<String, Double> e : entries)
        {
            Double v = e.getValue();
            if (v == null || Double.isNaN(v) || Double.isInfinite(v)) continue;
            maxAbs = Math.max(maxAbs, Math.abs(v));
        }
        return (maxAbs <= 0.0) ? 1.0 : maxAbs;
    }
}
