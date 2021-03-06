// code by gjoel
package ch.ethz.idsc.owl.math.planar;

import java.util.Optional;

import ch.ethz.idsc.sophus.math.GeodesicInterface;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.sca.Mod;

public final class GeodesicInterpolationEntryFinder extends TrajectoryEntryFinder {
  private static final Mod MOD_UNIT = Mod.function(1);
  // ---
  private final GeodesicInterface geodesicInterface;

  public GeodesicInterpolationEntryFinder(double initialIndex, GeodesicInterface geodesicInterface) {
    super(RealScalar.of(initialIndex));
    this.geodesicInterface = geodesicInterface;
  }

  @Override // from TrajectoryEntryFinder
  protected TrajectoryEntry protected_apply(Tensor waypoints, Scalar index) {
    int index_ = index.number().intValue();
    Optional<Tensor> point = Optional.empty();
    try {
      point = Optional.of(geodesicInterface.split( //
          waypoints.get(index_), //
          waypoints.get(index_ + 1), //
          MOD_UNIT.apply(index)));
    } catch (IndexOutOfBoundsException e1) {
      try {
        point = Optional.of(waypoints.get(index_));
      } catch (IndexOutOfBoundsException e2) {
        // ---
      }
    }
    return new TrajectoryEntry(point, index);
  }
}
