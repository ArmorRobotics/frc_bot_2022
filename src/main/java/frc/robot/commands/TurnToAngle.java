// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.PIDCommand;
import frc.robot.OI;
import frc.robot.Constants.DrivetrainConstants;

import frc.robot.subsystems.Drivetrain;

// TODO: Fix issue with large turns producing outputs greater than 1
public class TurnToAngle extends PIDCommand {
  private static double m_heading = 0;

  public TurnToAngle(Drivetrain drivetrain) {
    super(
      new PIDController(DrivetrainConstants.kTurnP, DrivetrainConstants.kTurnI, DrivetrainConstants.kTurnD),
      () -> -OI.pigeon.getYaw(),
      () -> m_heading,
      output -> drivetrain.robotDrive.arcadeDrive(output, 0)
    );

    addRequirements(drivetrain);

    // Configure additional PID options
    getController().setTolerance(DrivetrainConstants.kTurnPosTolerance, DrivetrainConstants.kTurnVelTolerance);
    getController().enableContinuousInput(-180, 180);
    getController().setSetpoint(0);
  }

  // TODO: Testing purposes
  @Override
  public void execute() {
    super.execute();
    System.out.println(
      getController().getPositionError() + " " + 
      (getController().calculate(m_measurement.getAsDouble(), m_setpoint.getAsDouble()))
    );
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return getController().atSetpoint();
  }

  /**
   * Sets target heading and resets PID controller
   *
   * @param fheading Target heading (in degrees)
   */
  public void setHeading(double fheading) {
    // TODO: Ensure that heading changes do not mess with derivative/integral calculations, reset PID controller as necessary
    m_heading = fheading;
    schedule();
  }
}