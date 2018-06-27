// code by ynager
package ch.ethz.idsc.owl.mapping;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import javax.swing.WindowConstants;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.indexer.UByteBufferIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import ch.ethz.idsc.owl.bot.se2.LidarEmulator;
import ch.ethz.idsc.owl.bot.util.RegionRenders;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.win.AffineTransforms;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.owl.math.map.Se2Bijection;
import ch.ethz.idsc.owl.math.region.ImageRegion;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.mat.DiagonalMatrix;

public class ShadowMapSpherical implements ShadowMap, RenderInterface {
  //
  private Color COLOR_SHADOW_FILL;
  // ---
  private final LidarEmulator lidar;
  private Mat initArea;
  private Mat shadowArea;
  private final float vMax;
  private final float rMin;
  Scalar pixelDim;
  Scalar pixelDimInv;
  private final GeometricLayer world2pixelLayer;
  private final Mat ellipseKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, //
      new Size(7, 7));
  Mat kernOrig = opencv_imgcodecs.imread("/home/ynager/Downloads/kernel6.bmp", opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
  Tensor world2pixel;
  Tensor pixel2world;
  Tensor scaling;

  public ShadowMapSpherical(LidarEmulator lidar, ImageRegion imageRegion, float vMax, float rMin) {
    this.lidar = lidar;
    this.vMax = vMax;
    this.rMin = rMin;
    BufferedImage bufferedImage = RegionRenders.image(imageRegion.image());
    // TODO 244 and 5 magic const, redundant to values specified elsewhere
    Mat area = bufferedImageToMat(bufferedImage);
    opencv_imgproc.threshold(area, area, 254, 255, opencv_imgproc.THRESH_BINARY_INV);
    //
    // convert imageRegion into Area
    Tensor scale = imageRegion.scale(); // pixels per meter
    pixelDim = scale.Get(0).reciprocal(); // meters per pixel
    scaling = DiagonalMatrix.of(pixelDim, pixelDim.negate(), RealScalar.ONE).unmodifiable();
    world2pixel = DiagonalMatrix.of(scale.Get(0), scale.Get(1).negate(), RealScalar.ONE);
    world2pixel.set(RealScalar.of(bufferedImage.getHeight()), 1, 2);
    //
    pixel2world = DiagonalMatrix.of( //
        scale.Get(0).reciprocal(), scale.Get(1).negate().reciprocal(), RealScalar.ONE);
    pixel2world.set(RealScalar.of(bufferedImage.getHeight()).multiply(scale.Get(1).reciprocal()), 1, 2);
    world2pixelLayer = GeometricLayer.of(world2pixel);
    //
    Mat obstacleArea = area.clone();
    initArea = new Mat(obstacleArea.size(), obstacleArea.type(), org.bytedeco.javacpp.opencv_core.Scalar.WHITE);
    opencv_imgproc.erode(initArea, initArea, ellipseKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);
    opencv_imgproc.dilate(obstacleArea, obstacleArea, ellipseKernel, new Point(-1, -1), radius2it(ellipseKernel, rMin), opencv_core.BORDER_CONSTANT, null);
    opencv_core.subtract(initArea, obstacleArea, initArea);
    this.shadowArea = initArea.clone();
    setColor(new Color(255, 50, 74));
  }

  public void updateMap(StateTime stateTime, float timeDelta) {
    updateMap(shadowArea, stateTime, timeDelta);
  }

  public Point state2pixel(Tensor state) {
    GeometricLayer layer = GeometricLayer.of(world2pixel);
    Point2D point2D = layer.toPoint2D(state);
    return new Point( //
        (int) point2D.getX(), //
        (int) point2D.getY());
  }

  public void updateMap(Mat area_, StateTime stateTime, float timeDelta) {
    synchronized (world2pixelLayer) {
      Mat area = area_.clone();
      // get lidar polygon and transform to pixel values
      List<Mat> updatedMatList = new ArrayList<>();
      Se2Bijection gokart2world = new Se2Bijection(stateTime.state());
      world2pixelLayer.pushMatrix(gokart2world.forward_se2());
      Tensor poly = lidar.getPolygon(stateTime);
      //  ---
      // transform lidar polygon to pixel values
      Tensor tens = Tensor.of(poly.stream().map(world2pixelLayer::toVector));
      world2pixelLayer.popMatrix();
      // put array into Point
      int[] intArr = new int[tens.length() * 2];
      for (int i = 0; i < tens.length(); i++) {
        intArr[i * 2] = tens.get(i).Get(0).number().intValue();
        intArr[i * 2 + 1] = tens.get(i).Get(1).number().intValue();
      }
      Point polygonPoint = new opencv_core.Point(intArr.length);
      polygonPoint.put(intArr, 0, intArr.length);
      // ---
      // fill lidar polygon and subtract it from shadow region
      Mat lidarMat = new Mat(initArea.size(), area.type(), opencv_core.Scalar.BLACK);
      opencv_imgproc.fillPoly(lidarMat, polygonPoint, new int[] { intArr.length / 2 }, 1, opencv_core.Scalar.WHITE);
      opencv_core.subtract(area, lidarMat, area);
      //  ---
      // dilate and intersect
      int it = radius2it(ellipseKernel, timeDelta * vMax);
      opencv_imgproc.dilate(area, area, ellipseKernel, new Point(-1, -1), it, opencv_core.BORDER_CONSTANT, null);
      opencv_core.bitwise_and(initArea, area, area);
      area.copyTo(area_);
    }
  }

  public final boolean isMember(Tensor state) {
    UByteBufferIndexer sI = shadowArea.createIndexer();
    return sI.get(0, 0) != 0;
  }

  public final Mat getCurrentMap() {
    return shadowArea.clone();
  }

  public final Mat getInitMap() {
    return initArea.clone();
  }

  public void setColor(Color color) {
    COLOR_SHADOW_FILL = color;
  }

  public static Mat bufferedImageToMat(BufferedImage bi) {
    Mat mat = new Mat(bi.getHeight(), bi.getWidth(), opencv_core.CV_8U);
    byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
    mat.data().put(data);
    return mat;
  }

  private final int radius2it(final Mat spericalKernel, float radius) {
    float pixelRadius = spericalKernel.arrayWidth() / 2.0f;
    float worldRadius = pixelRadius * pixelDim.number().floatValue();
    return (int) Math.ceil(radius / worldRadius);
  }

  @Override
  public void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    final Tensor matrix = geometricLayer.getMatrix();
    AffineTransform transform = AffineTransforms.toAffineTransform(matrix.dot(pixel2world));
    Mat plotArea = shadowArea.clone();
    // setup colorspace
    opencv_imgproc.cvtColor(plotArea, plotArea, opencv_imgproc.CV_GRAY2RGBA);
    Mat color = new Mat(4, 1, opencv_core.CV_8UC4);
    byte[] a = { (byte) COLOR_SHADOW_FILL.getAlpha(), //
        (byte) COLOR_SHADOW_FILL.getGreen(), //
        (byte) COLOR_SHADOW_FILL.getRed(), //
        (byte) COLOR_SHADOW_FILL.getBlue() };
    color.data().put(a);
    plotArea.setTo(color, plotArea);
    //  convert to bufferedimage
    BufferedImage img = new BufferedImage(plotArea.arrayWidth(), plotArea.arrayHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
    plotArea.data().get(data);
    graphics.drawImage(img, transform, null);
  }

  static void display(Mat image, String caption) {
    final CanvasFrame canvas = new CanvasFrame(caption, 1.0);
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    final OpenCVFrameConverter converter = new OpenCVFrameConverter.ToMat();
    canvas.showImage(converter.convert(image));
  }
}
