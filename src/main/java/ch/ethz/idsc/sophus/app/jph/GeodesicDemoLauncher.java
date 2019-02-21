// code by jph
package ch.ethz.idsc.sophus.app.jph;

import java.util.Arrays;

import ch.ethz.idsc.owl.bot.util.DemoLauncher;
import ch.ethz.idsc.sophus.app.avg.BezierFunctionSplitsDemo;
import ch.ethz.idsc.sophus.app.avg.ExtrapolationSplitsDemo;
import ch.ethz.idsc.sophus.app.avg.GeodesicCenterSplitsDemo;
import ch.ethz.idsc.sophus.app.curve.BSplineFunctionDemo;
import ch.ethz.idsc.sophus.app.curve.BezierFunctionDemo;
import ch.ethz.idsc.sophus.app.curve.CurveSubdivisionDemo;
import ch.ethz.idsc.sophus.app.filter.GeodesicCenterFilterDemo;

public enum GeodesicDemoLauncher {
  ;
  public static void main(String[] args) {
    DemoLauncher.build(Arrays.asList( //
        BezierFunctionSplitsDemo.class, //
        GeodesicCenterSplitsDemo.class, //
        ExtrapolationSplitsDemo.class, //
        BezierFunctionDemo.class, //
        CurveSubdivisionDemo.class, //
        BSplineFunctionDemo.class, //
        GeodesicCenterFilterDemo.class //
    ));
  }
}