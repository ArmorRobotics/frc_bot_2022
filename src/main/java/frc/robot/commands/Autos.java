// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;

import frc.robot.subsystems.Drivetrain;
import edu.wpi.first.wpilibj2.command.Command;

import edu.wpi.first.wpilibj2.command.CommandScheduler;

public final class Autos {
  public static enum Type {
    Side,
    Center,
    None
  }

  public static Command getAuto(Type type, Drivetrain sDrivetrain, Balance cBalance, Command cOuttake) {
    CommandScheduler.getInstance().removeComposedCommand(cBalance);
    CommandScheduler.getInstance().removeComposedCommand(cOuttake);
    
    switch(type) {
      case Side:
        return SideAuto(sDrivetrain, cOuttake);
      case Center:
        return CenterAuto(sDrivetrain, cBalance, cOuttake);
      default:
        return new InstantCommand();
    }
  }

  // Score a pre-loaded cube, then drive out of the community and back in
  private static Command SideAuto(Drivetrain sDrivetrain, Command cOuttake) {
    return new SequentialCommandGroup(
      new ParallelDeadlineGroup(new WaitCommand(1), cOuttake),

      new DriveDistance(sDrivetrain).beforeStarting(() -> DriveDistance.setDistance(-48)),

      new DriveDistance(sDrivetrain).beforeStarting(() -> DriveDistance.setDistance(36))
    );
  }

  // Auto to score a pre-loaded cube, drive over the charge station, then drive back and balance
  private static Command CenterAuto(Drivetrain sDrivetrain, Balance cBalance, Command cOuttake) {
    return new SequentialCommandGroup(
      new ParallelDeadlineGroup(new WaitCommand(1), cOuttake),

      new DriveDistance(sDrivetrain).beforeStarting(() -> DriveDistance.setDistance(-48)),

      new DriveDistance(sDrivetrain).beforeStarting(() -> DriveDistance.setDistance(24)),

      cBalance
    );
  }
}