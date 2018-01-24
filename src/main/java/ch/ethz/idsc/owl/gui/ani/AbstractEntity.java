// code by jph
package ch.ethz.idsc.owl.gui.ani;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ch.ethz.idsc.owl.data.Lists;
import ch.ethz.idsc.owl.glc.core.TrajectoryPlanner;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.math.StateTimeTensorFunction;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.owl.math.state.TrajectoryControl;
import ch.ethz.idsc.owl.math.state.TrajectoryRegionQuery;
import ch.ethz.idsc.owl.math.state.TrajectorySample;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;

/** entity executes flows along a given trajectory */
public abstract class AbstractEntity implements RenderInterface, AnimationInterface {
  private final TrajectoryControl trajectoryControl;
  private List<TrajectorySample> trajectory = null;
  private int trajectory_skip = 0;

  public AbstractEntity(TrajectoryControl trajectoryControl) {
    this.trajectoryControl = trajectoryControl;
  }

  public synchronized void setTrajectory(List<TrajectorySample> trajectory) {
    this.trajectory = trajectory;
    trajectory_skip = 0;
  }

  protected List<TrajectorySample> resetAction(List<TrajectorySample> trajectory) {
    System.err.println("out of trajectory");
    return null;
  }

  @Override
  public final synchronized void integrate(Scalar now) {
    trajectoryControl.integrate(now);
    // // implementation does not require that current position is perfectly located on trajectory
    // Tensor u = fallbackControl(); // default control
    // if (Objects.nonNull(trajectory)) {
    // final int argmin = indexOfPassedTrajectorySample(trajectory.subList(trajectory_skip, trajectory.size()));
    // GlobalAssert.that(argmin != ArgMin.NOINDEX);
    // int index = trajectory_skip + argmin;
    // trajectory_skip = index;
    // ++index; // <- next node has flow control
    // if (index < trajectory.size()) {
    // Optional<Tensor> optional = customControl(trajectory.subList(index, trajectory.size()));
    // if (optional.isPresent())
    // u = optional.get();
    // else {
    // GlobalAssert.that(trajectory.get(index).getFlow().isPresent());
    // u = trajectory.get(index).getFlow().get().getU();
    // }
    // } else {
    // trajectory = resetAction(trajectory);
    // }
    // }
    // episodeIntegrator.move(u, now);
  }

  /** @param trailAhead
   * @return */
  protected Optional<Tensor> customControl(List<TrajectorySample> trailAhead) {
    return Optional.empty();
  }

  // /** @param delay
  // * @return trajectory until delay[s] in the future of entity,
  // * or current position if entity does not have a trajectory */
  public final synchronized List<TrajectorySample> getFutureTrajectoryUntil(Scalar delay) {
    return trajectoryControl.getFutureTrajectoryUntil(delay);
    // if (Objects.isNull(trajectory)) // agent does not have a trajectory
    // return Collections.singletonList(TrajectorySample.head(getStateTimeNow()));
    // int index = trajectory_skip + indexOfPassedTrajectorySample(trajectory.subList(trajectory_skip, trajectory.size()));
    // // <- no update of trajectory_skip here
    // Scalar threshold = trajectory.get(index).stateTime().time().add(delay);
    // return trajectory.stream().skip(index) //
    // .filter(trajectorySample -> Scalars.lessEquals(trajectorySample.stateTime().time(), threshold)) //
    // .collect(Collectors.toList());
  }

  /** @param delay
   * @return estimated location of agent after given delay */
  public final Tensor getEstimatedLocationAt(Scalar delay) {
    if (Objects.isNull(trajectory))
      return getStateTimeNow().state();
    List<TrajectorySample> relevant = trajectoryControl.getFutureTrajectoryUntil(delay);
    return Lists.getLast(relevant).stateTime().state();
  }

  // TODO design preliminary
  public StateTimeTensorFunction represent_entity = StateTime::state;

  // /** the return index does not refer to node in the trajectory closest to the entity
  // * but rather the index of the node that was already traversed.
  // * this ensures that the entity can query the correct flow that leads to the upcoming node
  // *
  // * @param trajectory
  // * @return index of node that has been traversed most recently by entity */
  // public final int indexOfPassedTrajectorySample(List<TrajectorySample> trajectory) {
  // final Tensor x = represent_entity.apply(getStateTimeNow());
  // Tensor dist = Tensor.of(trajectory.stream() //
  // .map(TrajectorySample::stateTime) //
  // .map(represent_entity) //
  // .map(state -> distance(state, x)));
  // int argmin = ArgMin.of(dist);
  // // the below 'correction' does not help in tracking
  // // instead one could try blending flows depending on distance
  // // if (0 < argmin && argmin < dist.length() - 1)
  // // if (Scalars.lessThan(dist.Get(argmin - 1), dist.Get(argmin + 1)))
  // // --argmin;
  // return argmin;
  // }
  public StateTime getStateTimeNow() {
    return trajectoryControl.getStateTimeNow();
  }
  // {
  // return episodeIntegrator.tail();
  // }

  public abstract PlannerType getPlannerType();

  // /** @return control vector to feed the episodeIntegrator in case no planned trajectory is available */
  // protected abstract Tensor fallbackControl();
  /** @return delay between now and the future point in time from when to divert to a new trajectory */
  public abstract Scalar delayHint();

  /** @param obstacleQuery
   * @param goal for instance {px, py, angle}
   * @return */
  public abstract TrajectoryPlanner createTrajectoryPlanner(TrajectoryRegionQuery obstacleQuery, Tensor goal);
}
