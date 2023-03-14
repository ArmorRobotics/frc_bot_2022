// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.OI;
import frc.robot.Constants.DrivetrainConstants;
import frc.robot.Constants.DeviceConstants;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;

import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;

import edu.wpi.first.wpilibj2.command.RunCommand;

public class Drivetrain extends SubsystemBase {
  // Initialize motors, encoders, and differential drive
  private static final CANSparkMax
    fl_motor = new CANSparkMax(DeviceConstants.kFrontLeftID, MotorType.kBrushless),
    bl_motor = new CANSparkMax(DeviceConstants.kBackLeftID, MotorType.kBrushless),
    fr_motor = new CANSparkMax(DeviceConstants.kFrontRightID, MotorType.kBrushless),
    br_motor = new CANSparkMax(DeviceConstants.kBackRightID, MotorType.kBrushless);

  private static final RelativeEncoder l_encoder = fl_motor.getEncoder();
  private static final RelativeEncoder r_encoder = fr_motor.getEncoder();

  private final DifferentialDrive robotDrive = new DifferentialDrive(
    new MotorControllerGroup(fl_motor, bl_motor),
    new MotorControllerGroup(fr_motor, br_motor)
  );

  public Drivetrain() {
    // Set the default command for a subsystem here.
    setDefaultCommand(new RunCommand(
      () -> {
        if (Math.abs(OI.driver_cntlr.getTriggers()) > 0.1) {
          // Turn in place, input from trigger
          turnInPlace(DrivetrainConstants.kTurnMult * OI.driver_cntlr.getTriggers());
        } else {
          // Regular drive, input from left stick
          robotDrive.arcadeDrive(DrivetrainConstants.kTurnMult * OI.driver_cntlr.getLeftX(), DrivetrainConstants.kSpeedMult * OI.driver_cntlr.getLeftY(), true);
        }
      },
      this
    ));

    // Sets encoders to measure position and velocity in inches
    l_encoder.setPositionConversionFactor((Math.PI * DrivetrainConstants.kWheelDiameter) / DrivetrainConstants.kGearboxRatio);
    r_encoder.setPositionConversionFactor(-(Math.PI * DrivetrainConstants.kWheelDiameter) / DrivetrainConstants.kGearboxRatio);

    l_encoder.setVelocityConversionFactor((Math.PI * DrivetrainConstants.kWheelDiameter) / DrivetrainConstants.kGearboxRatio);
    r_encoder.setVelocityConversionFactor((Math.PI * DrivetrainConstants.kWheelDiameter) / DrivetrainConstants.kGearboxRatio);

    // Sets encoder measurement period to work with default command scheduler loop
    l_encoder.setMeasurementPeriod(20);
    r_encoder.setMeasurementPeriod(20);
  }

  public void turnInPlace(double rotationSpeed) {
    robotDrive.tankDrive(rotationSpeed, -rotationSpeed, false);
  }

  public void moveStraight(double speed) {
    robotDrive.tankDrive(speed, speed, false);
  }

  // Returns the average of the position of the encoders (in inches)
  public double getAvgPosition() {
    return (l_encoder.getPosition() + r_encoder.getPosition())/2;
  }

  public void resetEncoders() {
    l_encoder.setPosition(0);
    r_encoder.setPosition(0);
  }

  // Stops drivetrain motors
  public void stop() {
    robotDrive.stopMotor();
  }
}