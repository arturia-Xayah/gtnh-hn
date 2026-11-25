package dev.structureterminal.projection;

/** Transient impacts in gate-local block coordinates. Only visible clients keep this history. */
public final class PortalRipples {

    static final int LIFETIME = 32;
    private final double[] x = new double[4], y = new double[4];
    private final long[] tick = new long[4];
    private int next, size;

    static int pack(double x, double y) {
        return ((int) Math.round(x * 256) & 65535) << 16 | ((int) Math.round(y * 256) & 65535);
    }

    void add(int packed, long now) {
        x[next] = (short) (packed >>> 16) / 256.0;
        y[next] = (short) packed / 256.0;
        tick[next] = now;
        next = (next + 1) % tick.length;
        size = Math.min(size + 1, tick.length);
    }

    public void draw(Projection gate, double now, int transparency, Projection.Quad out) {
        if (transparency >= 100) return;
        for (int i = 0; i < size; i++) {
            double age = now - tick[i];
            if (age < 0 || age >= LIFETIME) continue;
            double fade = 1 - age / LIFETIME;
            int alpha = (int) (180 * fade * fade * (1 - transparency * .01));
            for (int crest = 0; crest < 3; crest++) {
                double radius = (age - crest * 2.6) * (.15 + Math.max(gate.width, gate.height) * .014);
                if (radius <= 0 || alpha <= 0) break;
                int color = (crest == 1 ? gate.primary : gate.secondary) << 8 | alpha;
                drawBand(gate, x[i], y[i], radius, .065 + .035 * age / LIFETIME, color, out);
                alpha = alpha * 2 / 3;
            }
        }
    }

    // Slice each annulus into horizontal strips and clip both halves to the gate, including ring holes.
    private static void drawBand(Projection gate, double cx, double cy, double radius, double thickness, int color,
        Projection.Quad out) {
        double low = Math.max(0, cy - radius), high = Math.min(gate.height, cy + radius);
        if (high <= low) return;
        int rows = (int) Math.ceil((high - low) * 18);
        double inner = Math.max(0, radius - thickness);
        double step = (high - low) / rows;
        for (int row = 0; row < rows; row++) {
            double y0 = low + row * step, y1 = low + (row + 1) * step;
            double dy0 = y0 - cy, dy1 = y1 - cy;
            double outer0 = span(radius, dy0), outer1 = span(radius, dy1);
            double inner0 = span(inner, dy0), inner1 = span(inner, dy1);
            double edge0 = gate.halfWidth(y0 / gate.height), edge1 = gate.halfWidth(Math.min(1, y1 / gate.height));
            // The widest point of the circular hole in this strip bounds its entire interior.
            double mid = gate.height * .5;
            double hole = gate.shape == Projection.Shape.RING
                ? span(gate.innerRadius(), Math.max(0, Math.max(y0 - mid, mid - y1)))
                : 0;
            for (int side = -1; side <= 1; side += 2) {
                double l0 = cx + (side < 0 ? -outer0 : inner0), r0 = cx + (side < 0 ? -inner0 : outer0);
                double l1 = cx + (side < 0 ? -outer1 : inner1), r1 = cx + (side < 0 ? -inner1 : outer1);
                if (hole > 0) {
                    strip(l0, r0, l1, r1, -edge0, -hole, -edge1, -hole, y0, y1, color, out);
                    strip(l0, r0, l1, r1, hole, edge0, hole, edge1, y0, y1, color, out);
                } else strip(l0, r0, l1, r1, -edge0, edge0, -edge1, edge1, y0, y1, color, out);
            }
        }
    }

    private static double span(double r, double dy) {
        return Math.sqrt(Math.max(0, r * r - dy * dy));
    }

    private static void strip(double l0, double r0, double l1, double r1, double min0, double max0, double min1,
        double max1, double y0, double y1, int color, Projection.Quad out) {
        double a = Math.max(min0, Math.min(max0, l0)), b = Math.max(min0, Math.min(max0, r0));
        double c = Math.max(min1, Math.min(max1, l1)), d = Math.max(min1, Math.min(max1, r1));
        if (b > a || d > c) out.draw(a, y0, b, y0, d, y1, c, y1, color);
    }
}
