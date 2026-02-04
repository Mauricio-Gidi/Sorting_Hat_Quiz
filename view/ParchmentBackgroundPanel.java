// ParchmentBackgroundPanel.java

package view;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Renders the parchment background used throughout the application.
 * The panel draws a parchment image scaled to cover the component bounds, adds
 * a vignette overlay for depth, and can apply an animated tint overlay that
 * fades in over time.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class ParchmentBackgroundPanel extends JPanel
{
    // Upper bound on the tint overlay alpha applied at full progress.
    private static final float MAX_TINT_ALPHA = 0.40f;

    // Swing timer interval used to animate the tint fade-in.
    private static final int TIMER_MS = 30;

    // Per-tick progress increment for the tint fade-in animation.
    private static final float TINT_STEP = 0.03f;

    // Alpha used for the vignette's edge darkening.
    private static final int VIGNETTE_ALPHA = 150;

    // Current target tint color; null means no tint is applied.
    private Color tintColor = null;

    // Current tint progress in [0, 1], where 1 represents a fully applied tint.
    private float tintProgress = 0.0f;

    // Timer used to animate tintProgress over time; null when no animation is active.
    private Timer tintTimer = null;

    /**
     * Creates a new panel configured with an opaque background.
     * The background color is also used as a fallback fill when the parchment
     * image is unavailable.
     */
    public ParchmentBackgroundPanel()
    {
        setOpaque(true);
        setBackground(SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM);
    }

    /**
     * Paints the parchment cover image, vignette overlay, and optional tint layer.
     *
     * @param g
     *     Graphics context provided by Swing for painting this component.
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

            paintParchmentCover(g2, w, h);
            paintVignette(g2, w, h);
            paintTint(g2, w, h);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Stops any active tint animation when the component is removed from its container.
     * This prevents the Swing timer from continuing to fire after the panel has been
     * detached.
     */
    @Override
    public void removeNotify()
    {
        stopTintTimer();
        super.removeNotify();
    }

    /**
     * Starts a fade-in animation toward the given tint color.
     * The tint overlay alpha ramps from 0 up to #MAX_TINT_ALPHA.
     *
     * @param targetTintColor
     *     The color to overlay on top of the parchment background.
     *
     * @throws NullPointerException
     *     If targetTintColor is null.
     */
    public void fadeBackgroundTintToColor(Color targetTintColor)
    {
        Objects.requireNonNull(targetTintColor, "targetTintColor");

        tintColor = targetTintColor;
        tintProgress = 0.0f;

        stopTintTimer();

        tintTimer = new Timer(TIMER_MS, e ->
        {
            tintProgress = Math.min(1.0f, tintProgress + TINT_STEP);
            repaint();

            if (tintProgress >= 1.0f)
            {
                stopTintTimer();
            }
        });

        tintTimer.setRepeats(true);
        tintTimer.setCoalesce(true);
        tintTimer.start();
    }

    /**
     * Clears any active tint and stops the tint animation timer.
     */
    public void clearBackgroundTint()
    {
        tintColor = null;
        tintProgress = 0.0f;

        stopTintTimer();
        repaint();
    }

    /**
     * Stops and releases the tint timer if it is currently running.
     */
    private void stopTintTimer()
    {
        if (tintTimer != null)
        {
            tintTimer.stop();
            tintTimer = null;
        }
    }

    /**
     * Paints the parchment background image scaled to cover the full panel area.
     * If the image is unavailable, a fallback vertical gradient is painted instead.
     *
     * @param g2
     *     Graphics context used for high-quality 2D painting.
     * @param panelW
     *     Current width of the panel in pixels.
     * @param panelH
     *     Current height of the panel in pixels.
     */
    private void paintParchmentCover(Graphics2D g2, int panelW, int panelH)
    {
        BufferedImage img = SortingHatTheme.ImageAssets.PARCHMENT_BACKGROUND_IMAGE;

        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0)
        {
            g2.setPaint(
                    new GradientPaint(
                            0,
                            0,
                            SortingHatTheme.Colors.MIDNIGHT_GRADIENT_TOP,
                            0,
                            panelH,
                            SortingHatTheme.Colors.MIDNIGHT_GRADIENT_BOTTOM
                    )
            );
            g2.fillRect(0, 0, panelW, panelH);
            return;
        }

        double sx = panelW / (double) img.getWidth();
        double sy = panelH / (double) img.getHeight();
        double scale = Math.max(sx, sy);

        int drawW = (int) Math.ceil(img.getWidth() * scale);
        int drawH = (int) Math.ceil(img.getHeight() * scale);

        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        g2.drawImage(img, x, y, drawW, drawH, null);
    }

    /**
     * Paints a radial vignette overlay that darkens the edges of the panel.
     *
     * @param g2
     *     Graphics context used for high-quality 2D painting.
     * @param w
     *     Current width of the panel in pixels.
     * @param h
     *     Current height of the panel in pixels.
     */
    private void paintVignette(Graphics2D g2, int w, int h)
    {
        float radius = Math.max(w, h);
        Point2D center = new Point2D.Float(w / 2f, h / 2f);

        RadialGradientPaint paint = new RadialGradientPaint(
                center,
                radius,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, VIGNETTE_ALPHA)}
        );

        Paint old = g2.getPaint();
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(old);
    }

    /**
     * Paints the current tint overlay based on the tint progress.
     * When the tint is active, the overlay alpha is scaled by the progress value.
     *
     * @param g2
     *     Graphics context used for high-quality 2D painting.
     * @param w
     *     Current width of the panel in pixels.
     * @param h
     *     Current height of the panel in pixels.
     */
    private void paintTint(Graphics2D g2, int w, int h)
    {
        if (tintColor == null || tintProgress <= 0.0f)
        {
            return;
        }

        float alpha = Math.min(MAX_TINT_ALPHA, tintProgress * MAX_TINT_ALPHA);

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.setColor(tintColor);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(old);
    }
}
