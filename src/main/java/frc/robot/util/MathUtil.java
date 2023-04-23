package frc.robot.util;

import frc.robot.subsystems.Drivetrain.WheelSpeeds;

public class MathUtil {
  private MathUtil() {};

  /**
   * Arcade drive inverse kinematics for a differential drive.
   *
   * @param drive forward speed [-1.0..1.0]
   * @param rotation clockwise rotation [-1.0..1.0]
   * @param squareInputs if {@code true}, squares other parameters to increase sensitivity
   * @return A differential drive {@link WheelSpeeds}
   * @see https://xiaoxiae.github.io/Robotics-Simplified-Website/drivetrain-control/arcade-drive/
   */
  public static WheelSpeeds arcadeDriveIK(double drive, double rotation, boolean squareInputs) {
    if (squareInputs) {
      drive = Math.copySign(drive * drive, drive);
      rotation = Math.copySign(rotation * rotation, rotation);
    }

    if (drive >= 0) {
      if (rotation >= 0) {
        return new WheelSpeeds(Math.max(drive, rotation), drive - rotation);
      } else {
        return new WheelSpeeds(drive + rotation, Math.max(drive, -rotation));
      }
    } else {
      if (rotation >= 0) {
        return new WheelSpeeds(drive + rotation, Math.min(drive, -rotation));
      } else {
        return new WheelSpeeds(Math.min(drive, rotation), drive - rotation);
      }
    }
  }
}