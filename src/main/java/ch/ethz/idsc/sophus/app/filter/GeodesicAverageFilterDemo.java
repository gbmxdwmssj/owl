// code by ob
package ch.ethz.idsc.sophus.app.filter;

import java.awt.Graphics2D;

import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.sophus.app.api.AbstractDemo;
import ch.ethz.idsc.sophus.filter.GeodesicAverage;
import ch.ethz.idsc.sophus.filter.GeodesicAverageFilter;
import ch.ethz.idsc.sophus.sym.SymWeightsToSplits;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;
import ch.ethz.idsc.tensor.red.Total;

/* package */ class GeodesicAverageFilterDemo extends DatasetKernelDemo {
  private static final TensorUnaryOperator NORMALIZE = Normalize.with(Total::ofVector);
  // ---
  private Tensor refined = Tensors.empty();

  public GeodesicAverageFilterDemo() {
    updateState();
  }

  @Override
  protected void updateState() {
    super.updateState();
    // TODO OB Currently tree and weights are staticly defined here since I don't know how do define a tree depending on size
    // TODO OB check consistency of radius and tree shape?
    Tensor tree = Tensors.vector(0, 1);
    for (int index = 2; index <= spinnerRadius.getValue(); ++index) {
      tree = Tensors.of(tree, RealScalar.of(index));
    }
    Tensor weights = Tensors.empty();
    for (int index = 0; index < spinnerRadius.getValue() + 1; ++index)
      weights.append(spinnerKernel.getValue().apply( //
          RealScalar.of(index).divide(RealScalar.of(spinnerRadius.getValue())).subtract(RealScalar.of(0.5))));
    weights = NORMALIZE.apply(weights);
    System.err.println(weights);
    SymWeightsToSplits symWeightsToSplits = new SymWeightsToSplits(tree, weights);
    TensorUnaryOperator tensorUnaryOperator = GeodesicAverage.of(geodesicDisplay().geodesicInterface(), symWeightsToSplits.splits());
    refined = GeodesicAverageFilter.of(tensorUnaryOperator, weights.length()).apply(control());
  }

  @Override // from RenderInterface
  protected Tensor protected_render(GeometricLayer geometricLayer, Graphics2D graphics) {
    // TODO OB display new splits
    return refined;
  }

  public static void main(String[] args) {
    AbstractDemo abstractDemo = new GeodesicAverageFilterDemo();
    abstractDemo.timerFrame.jFrame.setBounds(100, 100, 1000, 800);
    abstractDemo.timerFrame.jFrame.setVisible(true);
  }
}
