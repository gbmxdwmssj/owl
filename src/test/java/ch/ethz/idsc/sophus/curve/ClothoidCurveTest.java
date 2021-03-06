// code by jph
package ch.ethz.idsc.sophus.curve;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.pdf.Distribution;
import ch.ethz.idsc.tensor.pdf.NormalDistribution;
import ch.ethz.idsc.tensor.pdf.RandomVariate;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.Unit;
import ch.ethz.idsc.tensor.sca.Chop;
import junit.framework.TestCase;

public class ClothoidCurveTest extends TestCase {
  private static final Unit METER = Unit.of("m");

  private static Tensor a(Tensor vector) {
    return Tensors.of( //
        Quantity.of(vector.Get(0), METER), //
        Quantity.of(vector.Get(1), METER), //
        vector.Get(2));
  }

  public void testQuantity() {
    Tensor p1 = Tensors.fromString("{2[m], 3[m], 1}");
    Tensor q1 = Tensors.fromString("{4[m], 7[m], 2}");
    Tensor r1 = ClothoidCurve.INSTANCE.split(p1, q1, RationalScalar.HALF);
    Tensor p2 = Tensors.fromString("{2, 3, 1}");
    Tensor q2 = Tensors.fromString("{4, 7, 2}");
    Tensor r2 = ComplexClothoidCurve.INSTANCE.split(p2, q2, RationalScalar.HALF);
    Chop._11.requireClose(r1, a(r2));
  }

  public void testPreserve() {
    Distribution distribution = NormalDistribution.standard();
    for (int count = 0; count < 100; ++count) {
      Tensor p = RandomVariate.of(distribution, 3);
      Tensor q = RandomVariate.of(distribution, 3);
      Scalar lambda = RandomVariate.of(distribution);
      Tensor r1 = ClothoidCurve.INSTANCE.split(a(p), a(q), lambda);
      Tensor r2 = a(ComplexClothoidCurve.INSTANCE.split(p, q, lambda));
      Chop._10.requireClose(r1, r2);
    }
  }
}
