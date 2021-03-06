//code by ynager
package ch.ethz.idsc.owl.bot.se2.glc;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import ch.ethz.idsc.owl.bot.se2.LidarEmulator;
import ch.ethz.idsc.owl.bot.se2.Se2LateralAcceleration;
import ch.ethz.idsc.owl.bot.util.StreetScenario;
import ch.ethz.idsc.owl.bot.util.StreetScenarioData;
import ch.ethz.idsc.owl.glc.adapter.MultiConstraintAdapter;
import ch.ethz.idsc.owl.glc.core.PlannerConstraint;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.region.ImageRender;
import ch.ethz.idsc.owl.gui.ren.EntityImageRender;
import ch.ethz.idsc.owl.gui.ren.MouseShapeRender;
import ch.ethz.idsc.owl.gui.win.MouseGoal;
import ch.ethz.idsc.owl.gui.win.OwlyAnimationFrame;
import ch.ethz.idsc.owl.mapping.ShadowMapSimulator;
import ch.ethz.idsc.owl.mapping.ShadowMapSpherical;
import ch.ethz.idsc.owl.math.region.ConeRegion;
import ch.ethz.idsc.owl.math.region.ImageRegion;
import ch.ethz.idsc.owl.math.region.RegionWithDistance;
import ch.ethz.idsc.owl.math.state.SimpleTrajectoryRegionQuery;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.owl.math.state.TrajectoryRegionQuery;
import ch.ethz.idsc.owl.sim.LidarRaytracer;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Subdivide;
import ch.ethz.idsc.tensor.io.ImageFormat;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.qty.Degree;

public class Se2ShadowConstraintDemo extends Se2CarDemo {
  static final StreetScenarioData STREET_SCENARIO_DATA = StreetScenario.S5.load();
  private static final float PED_VELOCITY = 1.5f;
  private static final float PED_RADIUS = 0.2f;
  private static final Color PED_COLOR_LEGAL = new Color(211, 249, 114, 200);
  private static final float CAR_MAX_ACC = 1.51f;
  private static final float CAR_REACTION_TIME = 0.0f;
  private static final float CAR_RAD = 1.0f; // [m]
  private static final Tensor RANGE = Tensors.vector(57.2, 44);
  // ---
  private static final LidarRaytracer LIDAR_RAYTRACER = //
      new LidarRaytracer(Subdivide.of(Degree.of(-180), Degree.of(180), 72), Subdivide.of(0, 40, 120));

  @Override
  protected void configure(OwlyAnimationFrame owlyAnimationFrame) {
    StateTime stateTime = new StateTime(Tensors.vector(40.0, 11, 1.571), RealScalar.ZERO);
    GokartEntity gokartEntity = new GokartEntity(stateTime) {
      @Override
      public RegionWithDistance<Tensor> getGoalRegionWithDistance(Tensor goal) {
        return new ConeRegion(goal, Degree.of(30));
      }
    };
    Tensor imageCar = STREET_SCENARIO_DATA.imageCar_extrude(6);
    Tensor imagePed = STREET_SCENARIO_DATA.imagePedLegal;
    Tensor imageLid = STREET_SCENARIO_DATA.imagePedIllegal;
    ImageRegion imageRegionCar = new ImageRegion(imageCar, RANGE, false);
    ImageRegion imageRegionPed = new ImageRegion(imagePed, RANGE, false);
    ImageRegion imageRegionLid = new ImageRegion(imageLid, RANGE, true);
    TrajectoryRegionQuery lidarRay = SimpleTrajectoryRegionQuery.timeInvariant(imageRegionLid);
    //
    Collection<PlannerConstraint> constraintCollection = new ArrayList<>();
    PlannerConstraint regionConstraint = createConstraint(imageRegionCar);
    constraintCollection.add(regionConstraint);
    //
    ImageRender imageRender = ImageRender.of(STREET_SCENARIO_DATA.render, RANGE);
    owlyAnimationFrame.addBackground(imageRender);
    // Lidar
    LidarEmulator lidarEmulator = new LidarEmulator( //
        LIDAR_RAYTRACER, gokartEntity::getStateTimeNow, lidarRay);
    LidarEmulator lidarEmulatorCon = new LidarEmulator( //
        LIDAR_RAYTRACER, () -> new StateTime(Tensors.vector(0, 0, 0), RealScalar.ZERO), lidarRay);
    owlyAnimationFrame.addBackground(lidarEmulator);
    Tensor imgT = ResourceData.of("/graphics/car.png");
    BufferedImage img = ImageFormat.of(imgT);
    owlyAnimationFrame.addBackground(new EntityImageRender(() -> gokartEntity.getStateTimeNow(), img, Tensors.vector(3.5, 2)));
    //  ---
    // ShadowMaps
    ShadowMapSpherical smPedLegal = //
        new ShadowMapSpherical(lidarEmulator, imageRegionPed, PED_VELOCITY, PED_RADIUS);
    smPedLegal.setColor(PED_COLOR_LEGAL);
    owlyAnimationFrame.addBackground(smPedLegal);
    ShadowMapSimulator simPedLegal = new ShadowMapSimulator(smPedLegal, gokartEntity::getStateTimeNow);
    simPedLegal.startNonBlocking(10);
    //
    ShadowMapSpherical smPedLegalCon = //
        new ShadowMapSpherical(lidarEmulatorCon, imageRegionPed, PED_VELOCITY, PED_RADIUS);
    SimpleShadowConstraintCV shadowConstraintPed = //
        new SimpleShadowConstraintCV(smPedLegalCon, imageRegionCar, CAR_RAD, CAR_MAX_ACC, CAR_REACTION_TIME, false);
    constraintCollection.add(shadowConstraintPed);
    //
    RenderInterface renderInterface = new MouseShapeRender( //
        SimpleTrajectoryRegionQuery.timeInvariant(line(imageRegionCar)), //
        GokartEntity.SHAPE, () -> gokartEntity.getStateTimeNow().time());
    owlyAnimationFrame.addBackground(renderInterface);
    //
    gokartEntity.extraCosts.add(Se2LateralAcceleration.INSTANCE);
    PlannerConstraint plannerConstraint = MultiConstraintAdapter.of(constraintCollection);
    MouseGoal.simple(owlyAnimationFrame, gokartEntity, plannerConstraint);
    owlyAnimationFrame.add(gokartEntity);
    owlyAnimationFrame.jFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent windowEvent) {
        simPedLegal.flagShutdown();
      }
    });
  }

  public static void main(String[] args) {
    new Se2ShadowConstraintDemo().start().jFrame.setVisible(true);
  }
}
