// code by jph
package ch.ethz.idsc.tensor;

/** https://en.wikipedia.org/wiki/Metric_(mathematics) */
@FunctionalInterface
public interface TensorMetric {
  /** a metric satisfies the following conditions
   * 
   * 1. non-negativity or separation axiom
   * 2. identity of indiscernibles
   * 3. symmetry
   * 4. subadditivity or triangle inequality
   * 
   * @param p
   * @param q
   * @return distance between p and q */
  Scalar distance(Tensor p, Tensor q);
}
