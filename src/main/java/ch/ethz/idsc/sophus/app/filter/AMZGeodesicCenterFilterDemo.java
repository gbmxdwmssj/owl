// code by jph, ob
package ch.ethz.idsc.sophus.app.filter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.sophus.app.api.AbstractDemo;
import ch.ethz.idsc.sophus.app.util.SpinnerLabel;
import ch.ethz.idsc.sophus.filter.GeodesicCenter;
import ch.ethz.idsc.sophus.filter.GeodesicCenterFilter;
import ch.ethz.idsc.sophus.sym.SymLinkImages;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.HomeDirectory;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;
import ch.ethz.idsc.tensor.red.Nest;

public class AMZGeodesicCenterFilterDemo extends DatasetKernelDemo {
  private final SpinnerLabel<Integer> spinnerConvolution = new SpinnerLabel<>();
  protected final SpinnerLabel<String> spinnerLabelMeasurement = new SpinnerLabel<>();
  protected final SpinnerLabel<String> spinnerLabelSensorType = new SpinnerLabel<>();
  protected final SpinnerLabel<String> spinnerLabelString = new SpinnerLabel<>();
  private Tensor refined = Tensors.empty();
  protected final List<String> measurementName = Arrays.asList("pip_2018-05-27-18-48-01", "pip_2018-07-05-12-46-20", "pip_trackdrive_2018-07-14-16-29-48",
      "pip_trackdrive_2018-07-25-21-33-29", "pip_trackdrive_2018-08-11-11-49-34");
  protected final List<String> sensorType = Arrays.asList("gps_pose", "localizer_pose", "slam_pose");

  public AMZGeodesicCenterFilterDemo() {
    {
      spinnerLabelMeasurement.setList(measurementName);
      spinnerLabelMeasurement.addSpinnerListener(type -> updateState());
      spinnerLabelMeasurement.setIndex(3);
      spinnerLabelMeasurement.addToComponentReduced(timerFrame.jToolBar, new Dimension(200, 28), "measurementName");
    }
    {
      spinnerLabelSensorType.setList(sensorType);
      spinnerLabelSensorType.addSpinnerListener(type -> updateState());
      spinnerLabelSensorType.setIndex(1);
      spinnerLabelSensorType.addToComponentReduced(timerFrame.jToolBar, new Dimension(200, 28), "sensorType");
    }
    {
      spinnerConvolution.setList(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
      spinnerConvolution.setIndex(0);
      spinnerConvolution.addToComponentReduced(timerFrame.jToolBar, new Dimension(60, 28), "convolution");
      spinnerConvolution.addSpinnerListener(type -> updateState());
    }
    updateState();
  }

  @Override
  protected void updateState() {
    String data = "Desktop/MA/AMZ/ExtractedPoseData/" + spinnerLabelMeasurement.getValue() + "/" + spinnerLabelSensorType.getValue();
    try {
      // No orientation for GPS data => use only R2
      if (data.contains("gps")) {
        _control = Tensor.of(Import.of(HomeDirectory.file(data + ".csv")).stream().limit(spinnerLabelLimit.getValue())
            .map(row -> Tensors.of(row.extract(4, 6), RealScalar.ZERO)));
      } else if (data.contains("localizer") || data.contains("slam")) {
        _control = Tensor.of(Import.of(HomeDirectory.file(data + ".csv")).stream().limit(spinnerLabelLimit.getValue()).map(row -> row.extract(4, 7)));
      } else
        _control = Tensors.empty();
      _control = _control.extract(1, _control.length() - 100);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    TensorUnaryOperator tensorUnaryOperator = GeodesicCenter.of(geodesicDisplay().geodesicInterface(), spinnerKernel.getValue());
    refined = Nest.of( //
        GeodesicCenterFilter.of(tensorUnaryOperator, spinnerRadius.getValue()), //
        control(), spinnerConvolution.getValue());
  }

  @Override // from RenderInterface
  protected Tensor protected_render(GeometricLayer geometricLayer, Graphics2D graphics) {
    if (jToggleSymi.isSelected())
      graphics.drawImage(SymLinkImages.geodesicCenter(spinnerKernel.getValue(), spinnerRadius.getValue()).bufferedImage(), 0, 0, null);
    return refined;
  }

  public static void main(String[] args) {
    AbstractDemo abstractDemo = new AMZGeodesicCenterFilterDemo();
    abstractDemo.timerFrame.jFrame.setBounds(100, 100, 1000, 800);
    abstractDemo.timerFrame.jFrame.setVisible(true);
  }
}
