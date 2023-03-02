// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import frc.robot.Constants.DeviceConstants;

public class Intake extends SubsystemBase {
  public final CANSparkMax intakeMotor = new CANSparkMax(DeviceConstants.kIntakeCANid, MotorType.kBrushless);

  // Stops all motors and cancels current command
  public void stop() {
    intakeMotor.stopMotor();
    getCurrentCommand().cancel();
  }
}