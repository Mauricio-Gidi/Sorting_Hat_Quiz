// UiComponents.java

package view;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Collects reusable Swing UI components for the application.
 * This class is a non-instantiable namespace; the actual component implementations
 * are declared as package-private classes in this source file.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class UiComponents
{
    /**
     * Prevents instantiation of this utility holder.
     */
    private UiComponents(){}
}

/**
 * Centralizes application-wide high-quality Graphics2D rendering hints.
 * Components in this file call this helper before painting to keep visuals consistent.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class SortingHatGraphicsQuality
{
    /**
     * Prevents instantiation; this class provides only static utilities.
     */
    private SortingHatGraphicsQuality(){}

    /**
     * Applies a set of rendering hints intended for higher-quality 2D drawing.
     *
     * @param g2 Graphics context to update with rendering hints.
     *
     * @throws NullPointerException If g2 is null.
     */
    static void applyHighQualityRenderingHints(Graphics2D g2)
    {
        Objects.requireNonNull(g2, "g2");
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}

/**
 * Paints a rounded "card" background with gradient fill and border.
 * The component is transparent (non-opaque) and draws its own drop shadow.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class DecoratedCardPanel extends JPanel
{
    private static final int H_INSET = SortingHatTheme.SpacingAndSizing.SPACING_24_PIXELS;
    private static final int V_INSET = SortingHatTheme.SpacingAndSizing.SPACING_16_PIXELS;

    /**
     * Creates a transparent panel with standard insets suitable for the themed card look.
     */
    DecoratedCardPanel()
    {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                SortingHatTheme.SpacingAndSizing.DEFAULT_SCREEN_PADDING_PIXELS,
                SortingHatTheme.SpacingAndSizing.DEFAULT_SCREEN_PADDING_PIXELS,
                SortingHatTheme.SpacingAndSizing.DEFAULT_SCREEN_PADDING_PIXELS,
                SortingHatTheme.SpacingAndSizing.DEFAULT_SCREEN_PADDING_PIXELS
        ));
    }

    /**
     * Caps the maximum width to the themed decorated-card content width.
     *
     * @return The maximum size with width capped, preserving the inherited height.
     */
    @Override
    public Dimension getMaximumSize()
    {
        Dimension max = super.getMaximumSize();
        int cap = SortingHatTheme.SpacingAndSizing.MAXIMUM_DECORATED_CARD_CONTENT_WIDTH_PIXELS;
        int w = (max.width <= 0) ? cap : Math.min(cap, max.width);
        return new Dimension(w, max.height);
    }

    /**
     * Paints the card background, including a subtle shadow, gradient fill, and border.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();
            int cardW = w - H_INSET * 2;
            int cardH = h - V_INSET * 2;
            if (cardW <= 1 || cardH <= 1)
            {
                return;
            }

            int r = SortingHatTheme.SpacingAndSizing.DECORATED_CARD_CORNER_RADIUS_PIXELS;

            // Shadow behind the card.
            Shape shadow = new RoundRectangle2D.Float(
                    H_INSET + 3f, V_INSET + 4f, cardW, cardH, r + 4f, r + 4f
            );
            g2.setColor(new Color(
                    SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM.getRed(),
                    SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM.getGreen(),
                    SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM.getBlue(),
                    140
            ));
            g2.fill(shadow);

            // Main card body.
            Shape card = new RoundRectangle2D.Float(
                    H_INSET, V_INSET, cardW, cardH, r, r
            );
            g2.setPaint(new GradientPaint(
                    0, V_INSET,
                    SortingHatTheme.Colors.DECORATED_CARD_GRADIENT_TOP,
                    0, V_INSET + cardH,
                    SortingHatTheme.Colors.DECORATED_CARD_GRADIENT_BOTTOM
            ));
            g2.fill(card);

            // Gold outline.
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
            g2.draw(card);
        }
        finally
        {
            g2.dispose();
        }
    }
}

/**
 * A primary action button painted with the application's gold/amber theme.
 * The button uses custom painting instead of the default Swing LAF rendering.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ThemedPrimaryButton extends JButton
{
    /**
     * Creates a themed button configured for custom painting.
     *
     * @param text The label text displayed on the button.
     */
    ThemedPrimaryButton(String text)
    {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(SortingHatTheme.Colors.BUTTON_TEXT);
        setFont(SortingHatTheme.Fonts.HEADER_FONT.deriveFont(20f));
        setMargin(SortingHatTheme.SpacingAndSizing.DEFAULT_BUTTON_PADDING);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        setRolloverEnabled(true);
    }

    /**
     * Paints the themed button body, border, focus/hover ring, and centered text.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();
            int r = SortingHatTheme.SpacingAndSizing.DEFAULT_BUTTON_CORNER_RADIUS_PIXELS;

            ButtonModel m = getModel();
            boolean pressed = m.isArmed() && m.isPressed();
            boolean hover = m.isRollover();
            boolean enabled = isEnabled();

            Color top = SortingHatTheme.Colors.ACCENT_AMBER;
            Color bottom = SortingHatTheme.Colors.BUTTON_DARK_BASE;

            if (!enabled)
            {
                top = withAlpha(top, 120);
                bottom = withAlpha(bottom, 120);
            }
            else if (pressed)
            {
                top = top.darker();
                bottom = bottom.darker();
            }
            else if (hover)
            {
                top = top.brighter();
                bottom = bottom.brighter();
            }

            // Outer glow.
            g2.setColor(withAlpha(SortingHatTheme.Colors.ACCENT_AMBER, enabled ? 70 : 35));
            g2.fillRoundRect(-3, -3, w + 6, h + 6, r + 8, r + 8);

            // Main button body.
            Shape shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, r, r);
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fill(shape);

            // Gold outline.
            g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(shape);

            paintFocusOrHoverRing(g2, w, h, r, hover && enabled);
            paintCenteredText(g2, w, h, enabled);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Draws a focus ring when focused, or a softer ring when hovered.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     * @param r Corner radius in pixels.
     * @param hover Whether hover styling should be applied.
     */
    private void paintFocusOrHoverRing(Graphics2D g2, int w, int h, int r, boolean hover)
    {
        if (isFocusOwner())
        {
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(SortingHatTheme.Colors.FOCUS_RING_STRONG);
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, r + 6f, r + 6f));
        }
        else if (hover)
        {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(SortingHatTheme.Colors.FOCUS_RING_SOFT);
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, r + 6f, r + 6f));
        }
    }

    /**
     * Draws the button label centered within the component.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     * @param enabled Whether the button is enabled (affects text alpha).
     */
    private void paintCenteredText(Graphics2D g2, int w, int h, boolean enabled)
    {
        String text = getText();
        if (text == null)
        {
            text = "";
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();

        Color c = SortingHatTheme.Colors.BUTTON_TEXT;
        if (!enabled)
        {
            c = withAlpha(c, 160);
        }

        g2.setColor(c);
        g2.drawString(text, x, y);
    }

    /**
     * Returns a copy of a color with its alpha component replaced.
     *
     * @param base Base color to keep RGB components from.
     * @param a Alpha value to clamp into [0, 255].
     * @return A color with the same RGB values as base and the requested alpha.
     *
     * @throws NullPointerException If base is null.
     */
    private static Color withAlpha(Color base, int a)
    {
        int alpha = Math.max(0, Math.min(255, a));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }
}

/**
 * A small, rounded progress indicator showing the current step out of a total count.
 * The filled portion is computed from 1-based step indices.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ThemedProgressIndicator extends JComponent
{
    private int currentStepIndexOneBased = 1;
    private int totalStepCount = 1;

    /**
     * Creates a non-opaque indicator with a fixed preferred size.
     */
    ThemedProgressIndicator()
    {
        setOpaque(false);
        setPreferredSize(new Dimension(220, 16));
    }

    /**
     * Updates the displayed progress state and schedules a repaint.
     * Both values are clamped to be at least 1.
     *
     * @param currentStepIndexOneBased Current step number (1-based).
     * @param totalStepCount Total number of steps (must be at least 1).
     */
    public void setProgressSteps(int currentStepIndexOneBased, int totalStepCount)
    {
        this.currentStepIndexOneBased = Math.max(1, currentStepIndexOneBased);
        this.totalStepCount = Math.max(1, totalStepCount);
        repaint();
    }

    /**
     * Paints the progress tube and the filled portion based on the current fraction.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();
            int r = Math.max(8, h);

            float fraction = computeProgressFraction();

            Shape tube = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, r, r);
            g2.setPaint(new GradientPaint(
                    0, 0,
                    SortingHatTheme.Colors.PROGRESS_TUBE_GRADIENT_TOP,
                    0, h,
                    SortingHatTheme.Colors.PROGRESS_TUBE_GRADIENT_BOTTOM
            ));
            g2.fill(tube);

            int fillW = (int) Math.round((w - 1) * fraction);
            if (fillW > 0)
            {
                Shape fill = new RoundRectangle2D.Float(0, 0, fillW, h - 1f, r, r);
                g2.setPaint(new GradientPaint(
                        0, 0,
                        SortingHatTheme.Colors.PROGRESS_FILL_GRADIENT_TOP,
                        0, h,
                        SortingHatTheme.Colors.PROGRESS_FILL_GRADIENT_BOTTOM
                ));
                g2.fill(fill);
            }

            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(tube);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Converts the current and total step counts into a clamped fraction.
     *
     * @return A value in [0.0, 1.0] representing progress.
     */
    private float computeProgressFraction()
    {
        if (totalStepCount <= 1)
        {
            return 1f;
        }

        double f = (currentStepIndexOneBased - 1) / (double) (totalStepCount - 1);
        if (f < 0.0)
        {
            f = 0.0;
        }
        if (f > 1.0)
        {
            f = 1.0;
        }
        return (float) f;
    }
}

/**
 * A themed {@link BasicSliderUI} implementation for Likert-scale sliders.
 * This UI delegate customizes track, thumb, and mouse interaction behavior.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class ThemedLikertSliderUserInterface extends BasicSliderUI
{
    /**
     * Creates the UI delegate for the provided slider.
     *
     * @param slider The slider instance this UI will render.
     */
    ThemedLikertSliderUserInterface(JSlider slider)
    {
        super(slider);
    }

    /**
     * Creates a track listener that allows clicking the track to jump the slider value.
     *
     * @param slider The slider that will receive mouse events.
     * @return A track listener that updates the slider value on click.
     */
    @Override
    protected TrackListener createTrackListener(JSlider slider)
    {
        return new TrackListener()
        {
            /**
             * Updates the slider value based on click position unless the thumb is grabbed.
             *
             * @param e Mouse event.
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!slider.isEnabled())
                {
                    return;
                }

                slider.requestFocusInWindow();

                if (thumbRect.contains(e.getPoint()))
                {
                    super.mousePressed(e);
                    return;
                }

                int newValue = (slider.getOrientation() == JSlider.HORIZONTAL)
                        ? valueForXPosition(e.getX())
                        : valueForYPosition(e.getY());
                slider.setValue(newValue);
            }
        };
    }

    /**
     * Calculates a circular thumb size that scales with the slider font.
     *
     * @return The thumb size in pixels.
     */
    @Override
    protected Dimension getThumbSize()
    {
        int minD = 20;
        int fontSize = slider.getFont().getSize();
        int d = Math.max(minD, fontSize + 2);
        return new Dimension(d, d);
    }

    /**
     * Paints the themed slider track and calls into the filled-portion painter.
     *
     * @param g Graphics context provided by Swing.
     */
    @Override
    public void paintTrack(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int centerY = trackRect.y + trackRect.height / 2;
            int x = trackRect.x;
            int w = trackRect.width;
            int h = 10;

            Shape track = new RoundRectangle2D.Float(x, centerY - h / 2f, w, h, h, h);
            g2.setPaint(new GradientPaint(
                    0, centerY - h / 2f,
                    SortingHatTheme.Colors.PROGRESS_TUBE_GRADIENT_TOP,
                    0, centerY + h / 2f,
                    SortingHatTheme.Colors.PROGRESS_TUBE_GRADIENT_BOTTOM
            ));
            g2.fill(track);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(track);

            paintFilledPortion(g2, centerY, x, h);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Paints the filled portion of the track from the start up to the thumb center.
     *
     * @param g2 Graphics context to draw into.
     * @param centerY Vertical center of the track in pixels.
     * @param trackX Left x-coordinate of the track.
     * @param trackH Track height in pixels.
     */
    private void paintFilledPortion(Graphics2D g2, int centerY, int trackX, int trackH)
    {
        int fillW = thumbRect.x - trackX + thumbRect.width / 2;
        fillW = Math.max(0, Math.min(trackRect.width, fillW));
        if (fillW <= 0)
        {
            return;
        }

        Shape fill = new RoundRectangle2D.Float(trackX, centerY - trackH / 2f, fillW, trackH, trackH, trackH);
        g2.setPaint(new GradientPaint(
                0, centerY - trackH / 2f,
                SortingHatTheme.Colors.PROGRESS_FILL_GRADIENT_TOP,
                0, centerY + trackH / 2f,
                SortingHatTheme.Colors.PROGRESS_FILL_GRADIENT_BOTTOM
        ));
        g2.fill(fill);
    }

    /**
     * Paints a circular thumb with gradient fill, glow, and focus ring.
     *
     * @param g Graphics context provided by Swing.
     */
    @Override
    public void paintThumb(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int d = Math.min(thumbRect.width, thumbRect.height);
            int x = thumbRect.x + (thumbRect.width - d) / 2;
            int y = thumbRect.y + (thumbRect.height - d) / 2;

            Shape thumb = new Ellipse2D.Float(x, y, d - 1f, d - 1f);

            // Soft outer glow.
            g2.setColor(new Color(
                    SortingHatTheme.Colors.ACCENT_CYAN.getRed(),
                    SortingHatTheme.Colors.ACCENT_CYAN.getGreen(),
                    SortingHatTheme.Colors.ACCENT_CYAN.getBlue(),
                    70
            ));
            g2.fill(new Ellipse2D.Float(x - 4f, y - 4f, d + 7f, d + 7f));

            g2.setPaint(new GradientPaint(
                    0, y,
                    SortingHatTheme.Colors.ACCENT_AMBER,
                    0, y + d,
                    SortingHatTheme.Colors.BUTTON_DARK_BASE
            ));
            g2.fill(thumb);

            g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(thumb);

            if (slider.isFocusOwner())
            {
                g2.setColor(SortingHatTheme.Colors.FOCUS_RING_SOFT);
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new Ellipse2D.Float(x - 2f, y - 2f, d + 3f, d + 3f));
            }
        }
        finally
        {
            g2.dispose();
        }
    }
}

/**
 * A selectable, focusable tile for showing a forced-choice option.
 * The tile paints a shadowed, rounded rectangle and includes an option badge and
 * wrapped description text. When activated, it notifies a listener with the option key.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class OptionTileButton extends JComponent
{
    private static final int TILE_CORNER_RADIUS_PIXELS = 18;
    private static final int TILE_INTERNAL_PADDING_PIXELS = 16;

    private String optionKey;
    private String optionText;
    private boolean hover = false;
    private boolean pressed = false;
    private ActionListener selectedListener = e -> {};
    private AccessibleContext accessibleContext;

    /**
     * Creates an option tile with mouse and keyboard interaction enabled.
     */
    OptionTileButton()
    {
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(420, 210));

        installMouseBehavior();
        installKeyboardBehavior();
    }

    /**
     * Sets the option data displayed by the tile and updates accessibility metadata.
     *
     * @param optionKey Short key label used as an action command when selected.
     * @param optionText Text shown in the tile body.
     *
     * @throws NullPointerException If optionKey or optionText is null.
     */
    public void setOption(String optionKey, String optionText)
    {
        this.optionKey = Objects.requireNonNull(optionKey, "optionKey");
        this.optionText = Objects.requireNonNull(optionText, "optionText");

        AccessibleContext ctx = getAccessibleContext();
        ctx.setAccessibleName("Option " + optionKey);
        ctx.setAccessibleDescription(optionText);

        repaint();
    }

    /**
     * Sets the listener invoked when the tile is selected.
     * If null is provided, selections are ignored.
     *
     * @param optionSelectedListener Listener to notify on selection.
     */
    public void setOptionSelectedListener(ActionListener optionSelectedListener)
    {
        this.selectedListener = (optionSelectedListener != null) ? optionSelectedListener : e -> {};
    }

    /**
     * Returns the accessibility context for this component.
     *
     * @return The accessible context instance.
     */
    @Override
    public AccessibleContext getAccessibleContext()
    {
        if (accessibleContext == null)
        {
            accessibleContext = new AccessibleOptionTileButton();
        }
        return accessibleContext;
    }

    /**
     * Accessibility delegate that identifies this component as a push button.
     *
     * @author Mauricio Gidi
     * @version Last modified 13_Dec_2025
     */
    protected final class AccessibleOptionTileButton extends AccessibleJComponent
    {
        /**
         * Returns the accessible role as a push button.
         *
         * @return {@link AccessibleRole#PUSH_BUTTON}.
         */
        @Override
        public AccessibleRole getAccessibleRole()
        {
            return AccessibleRole.PUSH_BUTTON;
        }
    }

    /**
     * Installs hover/press handling via a mouse listener.
     */
    private void installMouseBehavior()
    {
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!isEnabled())
                {
                    return;
                }

                requestFocusInWindow();
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (!isEnabled())
                {
                    return;
                }

                boolean shouldSelect = pressed && contains(e.getPoint());
                pressed = false;
                repaint();

                if (shouldSelect)
                {
                    notifySelected();
                }
            }
        });
    }

    /**
     * Installs keyboard activation (Enter/Space) when the tile has focus.
     */
    private void installKeyboardBehavior()
    {
        InputMap in = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap ac = getActionMap();

        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select");
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "select");

        ac.put("select", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (!isEnabled())
                {
                    return;
                }
                notifySelected();
            }
        });
    }

    /**
     * Notifies the current selection listener using the option key as the action command.
     */
    private void notifySelected()
    {
        if (optionKey == null)
        {
            return;
        }
        selectedListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, optionKey));
    }

    /**
     * Paints the tile's shadow, background, border, and content.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();

            paintShadow(g2, w, h);
            paintBackground(g2, w, h);
            paintBorder(g2, w, h);
            paintContent(g2, w, h);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Paints the rounded drop shadow behind the tile.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     */
    private void paintShadow(Graphics2D g2, int w, int h)
    {
        g2.setColor(new Color(0, 0, 0, isEnabled() ? 130 : 70));
        Shape shadow = new RoundRectangle2D.Float(
                4f, 6f, w - 8f, h - 10f,
                TILE_CORNER_RADIUS_PIXELS + 6, TILE_CORNER_RADIUS_PIXELS + 6
        );
        g2.fill(shadow);
    }

    /**
     * Paints the tile's gradient background, with hover and pressed variations.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     */
    private void paintBackground(Graphics2D g2, int w, int h)
    {
        Shape tile = new RoundRectangle2D.Float(
                0.5f, 0.5f, w - 1f, h - 1f,
                TILE_CORNER_RADIUS_PIXELS, TILE_CORNER_RADIUS_PIXELS
        );

        Color top = new Color(18, 18, 18, isEnabled() ? 190 : 110);
        Color bottom = new Color(10, 10, 10, isEnabled() ? 220 : 130);

        if (isEnabled() && pressed)
        {
            top = new Color(25, 18, 10, 210);
            bottom = new Color(12, 10, 8, 235);
        }
        else if (isEnabled() && hover)
        {
            top = new Color(25, 25, 25, 205);
            bottom = new Color(12, 12, 12, 230);
        }

        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fill(tile);
    }

    /**
     * Paints the tile border and optional focus ring.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     */
    private void paintBorder(Graphics2D g2, int w, int h)
    {
        Shape tile = new RoundRectangle2D.Float(
                0.5f, 0.5f, w - 1f, h - 1f,
                TILE_CORNER_RADIUS_PIXELS, TILE_CORNER_RADIUS_PIXELS
        );

        if (!isEnabled())
        {
            g2.setColor(new Color(255, 255, 255, 25));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(tile);
            return;
        }

        if (isFocusOwner())
        {
            g2.setColor(SortingHatTheme.Colors.FOCUS_RING_SOFT);
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(new RoundRectangle2D.Float(
                    2.5f, 2.5f, w - 5f, h - 5f,
                    TILE_CORNER_RADIUS_PIXELS + 8, TILE_CORNER_RADIUS_PIXELS + 8
            ));
        }

        g2.setColor(new Color(
                SortingHatTheme.Colors.ACCENT_GOLD.getRed(),
                SortingHatTheme.Colors.ACCENT_GOLD.getGreen(),
                SortingHatTheme.Colors.ACCENT_GOLD.getBlue(),
                hover ? 170 : 110
        ));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(tile);
    }

    /**
     * Paints the option badge and wrapped description text.
     *
     * @param g2 Graphics context to draw into.
     * @param w Component width in pixels.
     * @param h Component height in pixels.
     */
    private void paintContent(Graphics2D g2, int w, int h)
    {
        if (optionKey == null || optionText == null)
        {
            return;
        }

        int left = TILE_INTERNAL_PADDING_PIXELS;
        int top = TILE_INTERNAL_PADDING_PIXELS;
        int contentW = Math.max(1, w - TILE_INTERNAL_PADDING_PIXELS * 2);
        int contentH = Math.max(1, h - TILE_INTERNAL_PADDING_PIXELS * 2);

        paintKeyBadge(g2, left, top);
        paintText(g2, left, top, contentW, contentH);
    }

    /**
     * Paints the circular badge containing the option key.
     *
     * @param g2 Graphics context to draw into.
     * @param left Left inset where the badge begins.
     * @param top Top inset where the badge begins.
     */
    private void paintKeyBadge(Graphics2D g2, int left, int top)
    {
        int d = 26;
        int x = left;
        int y = top;

        g2.setColor(new Color(
                SortingHatTheme.Colors.ACCENT_CYAN.getRed(),
                SortingHatTheme.Colors.ACCENT_CYAN.getGreen(),
                SortingHatTheme.Colors.ACCENT_CYAN.getBlue(),
                55
        ));
        g2.fillOval(x - 5, y - 5, d + 10, d + 10);

        g2.setColor(SortingHatTheme.Colors.ACCENT_GOLD);
        g2.fillOval(x, y, d, d);

        g2.setFont(SortingHatTheme.Fonts.HEADER_FONT.deriveFont(18f));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (d - fm.stringWidth(optionKey)) / 2;
        int ty = y + (d - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(Color.BLACK);
        g2.drawString(optionKey, tx, ty);
    }

    /**
     * Paints the wrapped option text to the right of the badge.
     *
     * @param g2 Graphics context to draw into.
     * @param left Left inset of the content area.
     * @param top Top inset of the content area.
     * @param contentW Width of the content area.
     * @param contentH Height of the content area.
     */
    private void paintText(Graphics2D g2, int left, int top, int contentW, int contentH)
    {
        int badgeDiameter = 26;
        int textLeft = left + badgeDiameter + 12;
        int textTop = top + 2;
        int textW = Math.max(1, contentW - (badgeDiameter + 12));
        int textH = Math.max(1, contentH - 4);

        g2.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(19f));
        g2.setColor(isEnabled() ? SortingHatTheme.Colors.TEXT_PRIMARY : SortingHatTheme.Colors.TEXT_SECONDARY);

        SortingHatTheme.drawWrappedTextLeft(
                g2,
                optionText,
                new Rectangle(textLeft, textTop, textW, textH),
                g2.getFontMetrics().getHeight() + 2
        );
    }
}

/**
 * Renders a bar chart of house probabilities, sorted from highest to lowest.
 * Each row includes the house name, a colored bar, and a formatted percentage.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class HouseProbabilityBarChartPanel extends JComponent
{
    private static final int ROW_HEIGHT_PIXELS = 44;
    private static final int ROW_GAP_PIXELS = 10;
    private static final int LABEL_COLUMN_WIDTH_PIXELS = 140;
    private static final int VALUE_COLUMN_WIDTH_PIXELS = 76;
    private static final int BAR_HEIGHT_PIXELS = 18;
    private static final int BAR_CORNER_RADIUS_PIXELS = 10;
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0%");

    private Map<String, Double> houseProbabilities = Map.of();

    /**
     * Creates a non-opaque chart with a fixed preferred size.
     */
    HouseProbabilityBarChartPanel()
    {
        setOpaque(false);
        setPreferredSize(new Dimension(760, 260));
    }

    /**
     * Sets the probabilities to render, keyed by house name.
     *
     * @param houseProbabilitiesByHouseName Mapping of house name to probability value.
     *
     * @throws NullPointerException If houseProbabilitiesByHouseName is null.
     */
    public void setHouseProbabilities(Map<String, Double> houseProbabilitiesByHouseName)
    {
        this.houseProbabilities = Objects.requireNonNull(houseProbabilitiesByHouseName, "houseProbabilitiesByHouseName");
        revalidate();
        repaint();
    }

    /**
     * Paints the probability rows sorted by value in descending order.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();

            List<Map.Entry<String, Double>> rows = new ArrayList<>(houseProbabilities.entrySet());
            rows.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> safeDouble(e.getValue())).reversed());

            int rowTop = 0;
            int barAreaLeft = LABEL_COLUMN_WIDTH_PIXELS;
            int barAreaWidth = Math.max(1, w - LABEL_COLUMN_WIDTH_PIXELS - VALUE_COLUMN_WIDTH_PIXELS);

            for (Map.Entry<String, Double> entry : rows)
            {
                paintRow(
                        g2,
                        String.valueOf(entry.getKey()),
                        safeDouble(entry.getValue()),
                        0,
                        rowTop,
                        w,
                        barAreaLeft,
                        barAreaWidth
                );

                rowTop += ROW_HEIGHT_PIXELS + ROW_GAP_PIXELS;
                if (rowTop > h)
                {
                    break;
                }
            }
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Paints a single probability row including label, bar, and formatted value.
     *
     * @param g2 Graphics context to draw into.
     * @param houseName House name to render.
     * @param probabilityValue Probability value for the house.
     * @param left Left x-coordinate of the row.
     * @param rowTop Top y-coordinate of the row.
     * @param width Total component width.
     * @param barAreaLeft Left edge of the bar area.
     * @param barAreaWidth Width allocated for the bar area.
     */
    private void paintRow(
            Graphics2D g2,
            String houseName,
            double probabilityValue,
            int left,
            int rowTop,
            int width,
            int barAreaLeft,
            int barAreaWidth
    )
    {
        int rowCenterY = rowTop + ROW_HEIGHT_PIXELS / 2;

        g2.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(Font.BOLD, 16f));
        g2.setColor(SortingHatTheme.Colors.TEXT_PRIMARY);

        FontMetrics fmLabel = g2.getFontMetrics();
        int labelY = rowCenterY + (fmLabel.getAscent() - fmLabel.getDescent()) / 2;
        g2.drawString(houseName, left, labelY);

        int barX = barAreaLeft;
        int barY = rowCenterY - BAR_HEIGHT_PIXELS / 2;
        int barW = Math.max(1, barAreaWidth);

        Shape bg = new RoundRectangle2D.Float(
                barX, barY, barW, BAR_HEIGHT_PIXELS,
                BAR_CORNER_RADIUS_PIXELS, BAR_CORNER_RADIUS_PIXELS
        );
        g2.setColor(new Color(255, 255, 255, 24));
        g2.fill(bg);

        double clamped = clamp01(probabilityValue);
        int fillW = Math.max(1, (int) Math.round(barW * clamped));

        Shape fill = new RoundRectangle2D.Float(
                barX, barY, fillW, BAR_HEIGHT_PIXELS,
                BAR_CORNER_RADIUS_PIXELS, BAR_CORNER_RADIUS_PIXELS
        );

        Color accent = SortingHatTheme.Colors.getHouseAccentColorByHouseName(houseName);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
        g2.fill(fill);

        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(bg);

        String percentText = PERCENT_FORMAT.format(clamped);

        g2.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(15f));
        g2.setColor(SortingHatTheme.Colors.TEXT_SECONDARY);

        FontMetrics fmVal = g2.getFontMetrics();
        int valueX = width - VALUE_COLUMN_WIDTH_PIXELS + 6;
        int valueY = rowCenterY + (fmVal.getAscent() - fmVal.getDescent()) / 2;
        g2.drawString(percentText, valueX, valueY);
    }

    /**
     * Clamps a double value into the inclusive range [0.0, 1.0].
     *
     * @param value Input value.
     * @return The clamped value.
     */
    private static double clamp01(double value)
    {
        if (value < 0.0)
        {
            return 0.0;
        }
        if (value > 1.0)
        {
            return 1.0;
        }
        return value;
    }

    /**
     * Converts a boxed Double to a safe finite primitive value.
     *
     * @param value Boxed value that may be null or non-finite.
     * @return A finite double value (defaults to 0.0 when invalid).
     */
    private static double safeDouble(Double value)
    {
        if (value == null)
        {
            return 0.0;
        }
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            return 0.0;
        }
        return value;
    }
}

/**
 * Renders a single row showing a trait name, a magnitude bar, and a numeric value.
 * The fill color indicates sign (gold for positive, cyan for negative).
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
final class TraitScoreBarRow extends JComponent
{
    private static final int ROW_HEIGHT_PIXELS = 36;
    private static final int BAR_HEIGHT_PIXELS = 14;
    private static final int CORNER_RADIUS_PIXELS = 10;
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.000");

    private final String traitName;
    private final double traitValue;
    private final double maxAbs;

    /**
     * Creates a trait row with a fixed height and configured scaling maximum.
     *
     * @param traitName Trait identifier or label text.
     * @param traitValue The numeric value for the trait.
     * @param maximumAbsoluteValue Maximum absolute value used for bar scaling.
     *
     * @throws NullPointerException If traitName is null.
     */
    TraitScoreBarRow(String traitName, double traitValue, double maximumAbsoluteValue)
    {
        this.traitName = Objects.requireNonNull(traitName, "traitName");
        this.traitValue = traitValue;
        this.maxAbs = (maximumAbsoluteValue <= 0.0) ? 1.0 : maximumAbsoluteValue;

        setOpaque(false);
        setPreferredSize(new Dimension(760, ROW_HEIGHT_PIXELS));
        setMinimumSize(new Dimension(460, ROW_HEIGHT_PIXELS));
    }

    /**
     * Paints the trait name, bar background, bar fill, and formatted value.
     *
     * @param g The graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            SortingHatGraphicsQuality.applyHighQualityRenderingHints(g2);

            int w = getWidth();
            int h = getHeight();

            int nameWidth = 220;
            int valueWidth = 84;
            int barLeft = nameWidth;
            int barRight = w - valueWidth;
            int barWidth = Math.max(1, barRight - barLeft);

            int centerY = h / 2;
            int barY = centerY - BAR_HEIGHT_PIXELS / 2;

            g2.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(Font.BOLD, 14.5f));
            g2.setColor(SortingHatTheme.Colors.TEXT_PRIMARY);
            FontMetrics fmName = g2.getFontMetrics();

            int nameY = centerY + (fmName.getAscent() - fmName.getDescent()) / 2;
            g2.drawString(formatTraitName(traitName), 0, nameY);

            Shape bg = new RoundRectangle2D.Float(
                    barLeft, barY, barWidth, BAR_HEIGHT_PIXELS,
                    CORNER_RADIUS_PIXELS, CORNER_RADIUS_PIXELS
            );
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fill(bg);

            double ratio = Math.min(1.0, Math.abs(traitValue) / maxAbs);
            int fillW = Math.max(1, (int) Math.round(barWidth * ratio));

            Shape fill = new RoundRectangle2D.Float(
                    barLeft, barY, fillW, BAR_HEIGHT_PIXELS,
                    CORNER_RADIUS_PIXELS, CORNER_RADIUS_PIXELS
            );

            Color fillColor = traitValue >= 0.0
                    ? SortingHatTheme.Colors.ACCENT_GOLD
                    : SortingHatTheme.Colors.ACCENT_CYAN;

            g2.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 170));
            g2.fill(fill);

            g2.setColor(new Color(255, 255, 255, 26));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(bg);

            String valueText = VALUE_FORMAT.format(traitValue);
            g2.setFont(SortingHatTheme.Fonts.BODY_FONT.deriveFont(14f));
            g2.setColor(SortingHatTheme.Colors.TEXT_SECONDARY);
            FontMetrics fmVal = g2.getFontMetrics();

            int valX = w - valueWidth + 6;
            int valY = centerY + (fmVal.getAscent() - fmVal.getDescent()) / 2;
            g2.drawString(valueText, valX, valY);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Formats trait identifiers for display (underscores to spaces, title-cased words).
     *
     * @param raw Raw trait identifier.
     * @return A display-friendly trait label.
     */
    private static String formatTraitName(String raw)
    {
        String s = raw.trim().replace('_', ' ');
        if (s.isEmpty())
        {
            return s;
        }

        String[] parts = s.split("\s+");
        StringBuilder out = new StringBuilder(s.length());

        for (int i = 0; i < parts.length; i++)
        {
            String p = parts[i];
            if (p.isEmpty())
            {
                continue;
            }
            if (i > 0)
            {
                out.append(' ');
            }

            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1)
            {
                out.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }
}
