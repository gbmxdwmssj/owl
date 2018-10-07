// code by ynager
package ch.ethz.idsc.owl.bot.r2;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.ImageFormat;

// resolution and range not yet decoupled. possibly use GeometricLayer
// option for cyclic waypoints: connect last with first
// TODO R2Waypoints is not used
enum R2Waypoints {
  ;
  /** creates an ImageRegion where the obstacle free space is generated by linearly interpolating
   * waypoints, and dilating the resulting path to achieve the specified pathWidth
   * 
   * @param tensor of waypoints of which only the first two dimensions are considered
   * @param pathWidth
   * @param dimension
   * @param range vector of length 2
   * @return */
  public static R2ImageRegionWrap createImageRegion(Tensor waypoints, float pathWidth, Dimension dimension, Tensor range) {
    BufferedImage bufferedImage = new BufferedImage( //
        dimension.width, //
        dimension.height, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D graphics = bufferedImage.createGraphics();
    // GeometricLayer geometricLayer = new GeometricLayer(model2pixel, Array.zeros(3));
    final Rectangle2D rInit = new Rectangle2D.Double(0, 0, dimension.width, dimension.height);
    final Area rInitArea = new Area(rInit);
    final Stroke stroke = new BasicStroke(pathWidth, BasicStroke.CAP_ROUND, BasicStroke.CAP_BUTT);
    for (int i = 0; i < waypoints.length() - 1; ++i) {
      Line2D line = new Line2D.Double( //
          toImagePoint(waypoints.get(i + 0), dimension.height), //
          toImagePoint(waypoints.get(i + 1), dimension.height));
      Area lineArea = new Area(stroke.createStrokedShape(line));
      if (lineArea.isEmpty())
        System.err.print("empty");
      rInitArea.subtract(lineArea);
    }
    graphics.fill(rInitArea);
    return new R2ImageRegionWrap(ImageFormat.from(bufferedImage), range, 30);
  }

  // helper function
  private static Point2D toImagePoint(Tensor waypoint, int height) {
    return new Point2D.Double( //
        waypoint.Get(0).number().doubleValue(), //
        height - waypoint.Get(1).number().doubleValue() - 1);
  }
}
