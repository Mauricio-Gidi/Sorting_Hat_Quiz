// SortingHatTheme.java

package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import config.*;

/**
 * Defines shared look-and-feel constants and UI helpers for the Sorting Hat app.
 * This class centralizes colors, spacing, fonts, and image assets, and also
 * provides small utilities used by multiple view components.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class SortingHatTheme
{
    /**
     * Prevents instantiation; this class is a static theme and UI utility holder.
     */
    private SortingHatTheme(){}

    /**
     * Provides shared spacing and sizing constants used across screens.
     */
    public static final class SpacingAndSizing
    {
        /**
         * Prevents instantiation; this class is a static constants holder.
         */
        private SpacingAndSizing(){}

        public static final int SPACING_4_PIXELS = 4;
        public static final int SPACING_6_PIXELS = 6;
        public static final int SPACING_8_PIXELS = 8;
        public static final int SPACING_12_PIXELS = 12;
        public static final int SPACING_16_PIXELS = 16;
        public static final int SPACING_20_PIXELS = 20;
        public static final int SPACING_24_PIXELS = 24;
        public static final int SPACING_32_PIXELS = 32;

        public static final int DEFAULT_SCREEN_PADDING_PIXELS = SPACING_16_PIXELS;
        public static final int DECORATED_CARD_CORNER_RADIUS_PIXELS = 20;
        public static final int MAXIMUM_DECORATED_CARD_CONTENT_WIDTH_PIXELS = 900;

        public static final int TITLE_FONT_SIZE_PIXELS = 36;
        public static final int HEADER_FONT_SIZE_PIXELS = 24;
        public static final int BODY_FONT_SIZE_PIXELS = 20;
        public static final int CAPTION_FONT_SIZE_PIXELS = 17;

        public static final int DEFAULT_BUTTON_CORNER_RADIUS_PIXELS = 20;
        public static final Insets DEFAULT_BUTTON_PADDING = new Insets(
                SPACING_8_PIXELS, SPACING_24_PIXELS, SPACING_8_PIXELS, SPACING_24_PIXELS
        );
    }

    /**
     * Provides the application's color palette and small color utilities.
     */
    public static final class Colors
    {
        /**
         * Prevents instantiation; this class is a static constants holder.
         */
        private Colors(){}

        public static final Color MIDNIGHT_GRADIENT_TOP = new Color(9, 12, 30);
        public static final Color MIDNIGHT_GRADIENT_BOTTOM = new Color(2, 4, 14);

        public static final Color DECORATED_CARD_BACKGROUND_BASE = new Color(38, 30, 22);
        public static final Color DECORATED_CARD_BACKGROUND_HIGHLIGHT = new Color(58, 46, 34);

        public static final Color ACCENT_GOLD = new Color(212, 175, 55);
        public static final Color ACCENT_AMBER = new Color(229, 155, 55);
        public static final Color ACCENT_CYAN = new Color(0, 220, 255);

        public static final Color TEXT_PRIMARY = new Color(245, 234, 204);
        public static final Color TEXT_SECONDARY = new Color(193, 177, 140);

        public static final Color BUTTON_TEXT = Color.BLACK;
        public static final Color BUTTON_DARK_BASE = new Color(80, 50, 20);

        public static final Color FOCUS_RING_STRONG = ACCENT_CYAN;
        public static final Color FOCUS_RING_SOFT = withAlpha(ACCENT_CYAN, 140);

        public static final Color DECORATED_CARD_GRADIENT_TOP = withAlpha(DECORATED_CARD_BACKGROUND_BASE, 140);
        public static final Color DECORATED_CARD_GRADIENT_BOTTOM = withAlpha(DECORATED_CARD_BACKGROUND_HIGHLIGHT, 180);

        public static final Color PROGRESS_TUBE_GRADIENT_TOP = new Color(20, 40, 60, 180);
        public static final Color PROGRESS_TUBE_GRADIENT_BOTTOM = new Color(5, 15, 25, 220);
        public static final Color PROGRESS_FILL_GRADIENT_TOP = withAlpha(ACCENT_CYAN, 200);
        public static final Color PROGRESS_FILL_GRADIENT_BOTTOM = new Color(0, 120, 180, 230);

        public static final Color GHOST_LABEL_TEXT = withAlpha(TEXT_SECONDARY, 200);

        /**
         * Returns a themed accent color for a given Hogwarts house name.
         * The mapping uses substring checks against the normalized (trimmed, lowercased)
         * input value.
         *
         * @param houseName House name to map to an accent color.
         * @return Accent color associated with the provided house name.
         *
         * @throws NullPointerException If houseName is null.
         */
        public static Color getHouseAccentColorByHouseName(String houseName)
        {
            Objects.requireNonNull(houseName, "houseName");

            String normalized = houseName.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("gryff")) return new Color(166, 17, 32);
            if (normalized.contains("slyth")) return new Color(16, 92, 56);
            if (normalized.contains("raven")) return new Color(13, 61, 125);
            if (normalized.contains("huff")) return new Color(186, 151, 36);
            return ACCENT_GOLD;
        }

        /**
         * Creates a copy of a color using the provided alpha value.
         *
         * @param base Base color whose RGB values are preserved.
         * @param alpha0To255 Alpha channel value clamped into [0, 255].
         * @return New color with the same RGB values as base and the requested alpha.
         *
         * @throws NullPointerException If base is null.
         */
        private static Color withAlpha(Color base, int alpha0To255)
        {
            int a = Math.max(0, Math.min(255, alpha0To255));
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        }
    }

    /**
     * Provides shared fonts and a helper for selecting an available family.
     */
    public static final class Fonts
    {
        /**
         * Prevents instantiation; this class is a static constants holder.
         */
        private Fonts(){}

        public static final Font TITLE_FONT = createPreferredDisplayFont(
                new String[] { "Cinzel", "Trajan Pro", "Garamond", "Goudy Text", "Serif" },
                Font.PLAIN,
                SpacingAndSizing.TITLE_FONT_SIZE_PIXELS,
                new Font("Serif", Font.BOLD, SpacingAndSizing.TITLE_FONT_SIZE_PIXELS)
        );

        public static final Font HEADER_FONT = createPreferredDisplayFont(
                new String[] { "Cinzel", "Trajan Pro", "Garamond", "Goudy Text", "Serif" },
                Font.PLAIN,
                SpacingAndSizing.HEADER_FONT_SIZE_PIXELS,
                new Font("Serif", Font.BOLD, SpacingAndSizing.HEADER_FONT_SIZE_PIXELS)
        );

        public static final Font BODY_FONT = new Font("Serif", Font.PLAIN, SpacingAndSizing.BODY_FONT_SIZE_PIXELS);
        public static final Font CAPTION_FONT =
                BODY_FONT.deriveFont(Font.PLAIN, (float) SpacingAndSizing.CAPTION_FONT_SIZE_PIXELS);

        /**
         * Chooses the first preferred font family available on the system, otherwise returns
         * the provided fallback font.
         *
         * @param preferredFamilyNames Ordered list of preferred font family names.
         * @param style Font style constant (e.g., Font#PLAIN).
         * @param sizePixels Target font size in pixels.
         * @param fallback Fallback font to return if none of the preferred families exist.
         * @return A new font using the first available preferred family, or fallback.
         *
         * @throws NullPointerException If preferredFamilyNames is null.
         */
        private static Font createPreferredDisplayFont(
                String[] preferredFamilyNames,
                int style,
                int sizePixels,
                Font fallback
        )
        {
            Objects.requireNonNull(preferredFamilyNames, "preferredFamilyNames");

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Set<String> available = new HashSet<>();
            for (String name : ge.getAvailableFontFamilyNames())
            {
                available.add(name.toLowerCase(Locale.ROOT));
            }

            for (String preferred : preferredFamilyNames)
            {
                if (preferred == null) continue;
                if (available.contains(preferred.toLowerCase(Locale.ROOT)))
                {
                    return new Font(preferred, style, sizePixels);
                }
            }

            return fallback;
        }
    }

    /**
     * Loads and exposes image resources referenced by the UI.
     */
    public static final class ImageAssets
    {
        /**
         * Prevents instantiation; this class is a static assets holder.
         */
        private ImageAssets(){}

        public static final BufferedImage PARCHMENT_BACKGROUND_IMAGE =
                loadBufferedImage(Config.PARCHMENT_BACKGROUND_PATH);
        public static final BufferedImage SORTING_HAT_ICON_IMAGE =
                loadBufferedImage(Config.HAT_ICON_PATH);

        public static final ImageIcon QUICK_MODE_ICON =
                loadIcon(Config.QUICK_ICON_PATH);
        public static final ImageIcon STANDARD_MODE_ICON =
                loadIcon(Config.STANDARD_ICON_PATH);
        public static final ImageIcon THOROUGH_MODE_ICON =
                loadIcon(Config.THOROUGH_ICON_PATH);

        /**
         * Returns the parchment background width-to-height ratio when available, otherwise
         * returns the provided default ratio.
         *
         * @param defaultAspectRatioWidthOverHeight Default ratio to use when the image is missing
         *                                         or has invalid dimensions.
         * @return The background image aspect ratio, or the default ratio when unavailable.
         */
        public static double getParchmentBackgroundAspectRatioOrDefault(double defaultAspectRatioWidthOverHeight)
        {
            if (defaultAspectRatioWidthOverHeight <= 0.0) defaultAspectRatioWidthOverHeight = 16.0 / 9.0;

            BufferedImage img = PARCHMENT_BACKGROUND_IMAGE;
            if (img == null) return defaultAspectRatioWidthOverHeight;

            int w = img.getWidth();
            int h = img.getHeight();
            if (w <= 0 || h <= 0) return defaultAspectRatioWidthOverHeight;

            return w / (double) h;
        }

        /**
         * Loads a buffered image from the classpath resource path.
         *
         * @param absoluteClasspathPath Absolute classpath path for the image resource.
         * @return Loaded buffered image.
         *
         * @throws IllegalStateException If the resource does not exist or cannot be decoded.
         * @throws NullPointerException If absoluteClasspathPath is null.
         * @throws RuntimeException If an I/O error occurs while reading the resource.
         */
        private static BufferedImage loadBufferedImage(String absoluteClasspathPath)
        {
            try (InputStream in = ImageAssets.class.getResourceAsStream(absoluteClasspathPath))
            {
                if (in == null) throw new IllegalStateException("Missing classpath resource: " + absoluteClasspathPath);

                BufferedImage img = ImageIO.read(in);
                if (img == null) throw new IllegalStateException("Unreadable image: " + absoluteClasspathPath);

                return img;
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load image: " + absoluteClasspathPath, e);
            }
        }

        /**
         * Loads an icon from the classpath resource path.
         *
         * @param absoluteClasspathPath Absolute classpath path for the icon resource.
         * @return Loaded ImageIcon instance.
         *
         * @throws IllegalStateException If the resource cannot be found.
         * @throws NullPointerException If absoluteClasspathPath is null.
         */
        private static ImageIcon loadIcon(String absoluteClasspathPath)
        {
            java.net.URL url = ImageAssets.class.getResource(absoluteClasspathPath);
            if (url == null) throw new IllegalStateException("Missing classpath resource: " + absoluteClasspathPath);
            return new ImageIcon(url);
        }
    }

    /**
     * Creates a primary themed button instance.
     *
     * @param buttonText Text to display in the button.
     * @return New themed primary button.
     */
    public static ThemedPrimaryButton createPrimaryButton(String buttonText)
    {
        return new ThemedPrimaryButton(buttonText);
    }

    /**
     * Creates a title label styled with the theme's title font and primary text color.
     *
     * @param labelText Text to display.
     * @return Configured JLabel instance.
     */
    public static JLabel createTitleLabel(String labelText)
    {
        JLabel label = new JLabel(labelText);
        label.setFont(Fonts.TITLE_FONT);
        label.setForeground(Colors.TEXT_PRIMARY);
        return label;
    }

    /**
     * Creates a header label styled with the theme's header font and primary text color.
     *
     * @param labelText Text to display.
     * @return Configured JLabel instance.
     */
    public static JLabel createHeaderLabel(String labelText)
    {
        JLabel label = new JLabel(labelText);
        label.setFont(Fonts.HEADER_FONT);
        label.setForeground(Colors.TEXT_PRIMARY);
        return label;
    }

    /**
     * Creates a caption label styled with the theme's caption font and secondary text color.
     *
     * @param labelText Text to display.
     * @return Configured JLabel instance.
     */
    public static JLabel createCaptionLabel(String labelText)
    {
        JLabel label = new JLabel(labelText);
        label.setFont(Fonts.CAPTION_FONT);
        label.setForeground(Colors.GHOST_LABEL_TEXT);
        return label;
    }

    /**
     * Creates a themed Likert slider configured for the app's 1.0 to 5.0 response scale.
     *
     * @return Configured JSlider instance for Likert responses.
     */
    public static JSlider createLikertSlider()
    {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 10, 50, 30);
        slider.setOpaque(false);
        slider.setFocusable(true);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(0);
        slider.setFont(Fonts.CAPTION_FONT);
        slider.setForeground(Colors.TEXT_SECONDARY);
        slider.setLabelTable(buildLikertLabelTable());
        slider.setUI(new ThemedLikertSliderUserInterface(slider));
        return slider;
    }

    /**
     * Converts the current slider value into a Likert score value.
     *
     * @param likertSlider Slider whose integer value represents a tenths-based Likert score.
     * @return Likert score as a double (slider value divided by 10.0).
     *
     * @throws NullPointerException If likertSlider is null.
     */
    public static double convertLikertSliderValueToLikertScore(JSlider likertSlider)
    {
        Objects.requireNonNull(likertSlider, "likertSlider");
        return likertSlider.getValue() / 10.0;
    }

    /**
     * Builds the label table used for Likert slider tick labels.
     *
     * @return Dictionary mapping tick values to label components.
     */
    private static Dictionary<Integer, JComponent> buildLikertLabelTable()
    {
        Hashtable<Integer, JComponent> table = new Hashtable<>();
        table.put(10, likertLabel("Strongly", "disagree"));
        table.put(15, likertLabel("Disagree", ""));
        table.put(20, likertLabel("Somewhat", "disagree"));
        table.put(25, likertLabel("Slightly", "disagree"));
        table.put(30, likertLabel("Neither agree", "nor disagree"));
        table.put(35, likertLabel("Slightly", "agree"));
        table.put(40, likertLabel("Somewhat", "agree"));
        table.put(45, likertLabel("Agree", ""));
        table.put(50, likertLabel("Strongly", "agree"));
        return table;
    }

    /**
     * Creates a small two-line centered HTML label for Likert slider tick marks.
     *
     * @param line1 First line of the label.
     * @param line2 Optional second line of the label; blank values are omitted.
     * @return Configured JLabel for the slider label table.
     */
    private static JLabel likertLabel(String line1, String line2)
    {
        String html = (line2 == null || line2.trim().isEmpty())
                ? String.format("<html><center>%s</center></html>", escapeHtml(line1))
                : String.format("<html><center>%s<br>%s</center></html>", escapeHtml(line1), escapeHtml(line2));

        JLabel label = new JLabel(html, SwingConstants.CENTER);
        label.setForeground(Colors.TEXT_SECONDARY);
        label.setFont(Fonts.CAPTION_FONT.deriveFont(15f));
        return label;
    }

    /**
     * Escapes a small subset of HTML characters to avoid breaking label markup.
     *
     * @param s Raw input string.
     * @return Escaped string safe to embed in a small HTML label, or an empty string if s is null.
     */
    private static String escapeHtml(String s)
    {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Draws wrapped text aligned to the left within the given bounds.
     *
     * @param g Graphics context used for measuring and drawing text.
     * @param text Text to wrap and draw.
     * @param bounds Bounding rectangle for drawing.
     * @param lineHeightPixels Height of each rendered line in pixels.
     *
     * @throws IllegalArgumentException If lineHeightPixels is not greater than 0.
     * @throws NullPointerException If g is null.
     * @throws NullPointerException If text is null.
     * @throws NullPointerException If bounds is null.
     */
    public static void drawWrappedTextLeft(Graphics2D g, String text, Rectangle bounds, int lineHeightPixels)
    {
        drawWrappedText(g, text, bounds, lineHeightPixels, false);
    }

    /**
     * Draws wrapped text centered horizontally within the given bounds.
     *
     * @param g Graphics context used for measuring and drawing text.
     * @param text Text to wrap and draw.
     * @param bounds Bounding rectangle for drawing.
     * @param lineHeightPixels Height of each rendered line in pixels.
     *
     * @throws IllegalArgumentException If lineHeightPixels is not greater than 0.
     * @throws NullPointerException If g is null.
     * @throws NullPointerException If text is null.
     * @throws NullPointerException If bounds is null.
     */
    public static void drawWrappedTextCentered(Graphics2D g, String text, Rectangle bounds, int lineHeightPixels)
    {
        drawWrappedText(g, text, bounds, lineHeightPixels, true);
    }

    /**
     * Draws wrapped text with optional centering within the provided bounds.
     *
     * @param g Graphics context used for measuring and drawing text.
     * @param text Text to wrap and draw.
     * @param bounds Bounding rectangle for drawing.
     * @param lineHeightPixels Height of each rendered line in pixels.
     * @param center True to center lines horizontally; false to left-align.
     *
     * @throws IllegalArgumentException If lineHeightPixels is not greater than 0.
     * @throws NullPointerException If g is null.
     * @throws NullPointerException If text is null.
     * @throws NullPointerException If bounds is null.
     */
    private static void drawWrappedText(Graphics2D g, String text, Rectangle bounds, int lineHeightPixels, boolean center)
    {
        Objects.requireNonNull(g, "g");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(bounds, "bounds");
        if (lineHeightPixels <= 0) throw new IllegalArgumentException("lineHeightPixels must be > 0");

        FontMetrics fm = g.getFontMetrics();
        int baselineY = bounds.y + fm.getAscent();

        String[] paragraphs = text.trim().split("\\R+");
        for (String para : paragraphs)
        {
            baselineY = drawWrappedParagraph(g, fm, para, bounds, lineHeightPixels, center, baselineY);
            baselineY += lineHeightPixels;
            if (baselineY > bounds.y + bounds.height) return;
        }
    }

    /**
     * Draws a single paragraph of wrapped text and returns the last used baseline Y.
     *
     * @param g Graphics context used for drawing.
     * @param fm Font metrics used to compute line widths and baselines.
     * @param paragraph Paragraph text to wrap (null treated as an empty string).
     * @param bounds Bounding rectangle for drawing.
     * @param lineHeightPixels Height of each rendered line in pixels.
     * @param center True to center lines horizontally; false to left-align.
     * @param startBaselineY Starting baseline Y for the first line.
     * @return Baseline Y position after drawing the paragraph.
     */
    private static int drawWrappedParagraph(
            Graphics2D g,
            FontMetrics fm,
            String paragraph,
            Rectangle bounds,
            int lineHeightPixels,
            boolean center,
            int startBaselineY
    )
    {
        if (paragraph == null) paragraph = "";
        String[] words = paragraph.trim().isEmpty() ? new String[0] : paragraph.trim().split("\s+");

        StringBuilder line = new StringBuilder();
        int baselineY = startBaselineY;

        for (String word : words)
        {
            String candidate = (line.length() == 0) ? word : (line + " " + word);
            if (fm.stringWidth(candidate) <= bounds.width)
            {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            if (line.length() > 0)
            {
                drawLine(g, fm, line.toString(), bounds, center, baselineY);
                baselineY += lineHeightPixels;
                if (baselineY > bounds.y + bounds.height) return baselineY;
            }

            line.setLength(0);
            line.append(word);
        }

        if (line.length() > 0 && baselineY <= bounds.y + bounds.height)
        {
            drawLine(g, fm, line.toString(), bounds, center, baselineY);
        }

        return baselineY;
    }

    /**
     * Draws a single line of text either left-aligned or centered within the bounds.
     *
     * @param g Graphics context used for drawing.
     * @param fm Font metrics used to measure line width.
     * @param line Text to draw.
     * @param bounds Bounding rectangle for drawing.
     * @param center True to center horizontally; false to left-align.
     * @param baselineY Baseline Y coordinate to draw the line at.
     *
     * @throws NullPointerException If g is null.
     * @throws NullPointerException If fm is null.
     * @throws NullPointerException If line is null.
     * @throws NullPointerException If bounds is null.
     */
    private static void drawLine(Graphics2D g, FontMetrics fm, String line, Rectangle bounds, boolean center, int baselineY)
    {
        int x = center
                ? bounds.x + (bounds.width - fm.stringWidth(line)) / 2
                : bounds.x;
        g.drawString(line, x, baselineY);
    }

    /**
     * Installs a key binding that is active when the component is an ancestor of the focused component.
     *
     * @param component Component that owns the InputMap and ActionMap.
     * @param keyStroke Key stroke that triggers the action.
     * @param actionName Unique action name used as the map key.
     * @param action Runnable to execute when the key stroke is triggered.
     *
     * @throws NullPointerException If component is null.
     * @throws NullPointerException If keyStroke is null.
     * @throws NullPointerException If actionName is null.
     * @throws NullPointerException If action is null.
     */
    public static void installKeyBinding(JComponent component, KeyStroke keyStroke, String actionName, Runnable action)
    {
        installKeyBinding(component, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keyStroke, actionName, action);
    }

    /**
     * Installs a key binding on the provided component using the given input map condition.
     *
     * @param component Component that owns the InputMap and ActionMap.
     * @param condition Input map condition constant from {@link JComponent}.
     * @param keyStroke Key stroke that triggers the action.
     * @param actionName Unique action name used as the map key.
     * @param action Runnable to execute when the key stroke is triggered.
     *
     * @throws NullPointerException If component is null.
     * @throws NullPointerException If keyStroke is null.
     * @throws NullPointerException If actionName is null.
     * @throws NullPointerException If action is null.
     */
    public static void installKeyBinding(
            JComponent component,
            int condition,
            KeyStroke keyStroke,
            String actionName,
            Runnable action
    )
    {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(keyStroke, "keyStroke");
        Objects.requireNonNull(actionName, "actionName");
        Objects.requireNonNull(action, "action");

        InputMap inputMap = component.getInputMap(condition);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction()
        {
            /**
             * Executes the provided runnable when the key binding is triggered.
             *
             * @param e Action event provided by Swing.
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                action.run();
            }
        });
    }
}
