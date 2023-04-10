// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.OI;
import frc.robot.Constants.IntakeConstants;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;

import frc.robot.subsystems.*;

public final class Autos {
  public static enum Starter {
    Shoot,
    Spit,
    ShootDown,
    SpitDown,
    None
  }
  public static enum Body {
    LongEscape,
    ShortEscape,
    PickupCone,
    CenterOver,
    CenterSimple,
    None
  }
  public static enum Ending {
    TurnAway,
    TurnClose,
    ReturnFromCone,
    None
  }

  public static Command getAuto(Starter starter, Body body, Ending end, IntakeTilt sIntakeTilt, IntakeWheels sIntakeWheels, Drivetrain sDrivetrain) {
    // TODO(HIGH prio): Fix auton endings - entails rewriting static headings in TurnToAngle and DriveDistance, as well as allowing for values (ex. current encoder position) to be given to the end module at module initialization, potential create as a new type of command
    // TODO:(mid prio): Attempt to add dynamic secondary body module with values depending on the selected first body module, editing values with a refresh button
    return new SequentialCommandGroup(
      getStarter(starter, sIntakeTilt, sIntakeWheels).raceWith(new RunCommand(sDrivetrain::stop, sDrivetrain)),
      getBody(body, sDrivetrain, sIntakeTilt, sIntakeWheels),
      getEnd(end, body, sDrivetrain, sIntakeTilt, sIntakeWheels)
    );
  }

  /** A command to handle the preloaded game piece. Does not move the drivetrain. */
  private static Command getStarter(Starter starter, IntakeTilt sIntakeTilt, IntakeWheels sIntakeWheels) {
    switch (starter) {
      case Shoot:
        // Shoot
        return sIntakeWheels.getShootCommand().withTimeout(0.5);
      case Spit:
        // Spit
        return sIntakeWheels.getSpitCommand().withTimeout(0.5);
      case ShootDown:
        // Aim down, shoot, then move intake up
        return new SequentialCommandGroup(
          new AimMid(sIntakeTilt).raceWith(
            new WaitCommand(IntakeConstants.kAimMidTimer).andThen(sIntakeWheels.getShootCommand().withTimeout(0.5))
          ),
          new IntakeUp(sIntakeTilt)
        );
      case SpitDown:
        // Aim down, spit, then move intake up
        return new SequentialCommandGroup(
          new AimMid(sIntakeTilt).raceWith(
            new WaitCommand(IntakeConstants.kAimMidTimer).andThen(sIntakeWheels.getSpitCommand().withTimeout(0.5))
          ),
          new IntakeUp(sIntakeTilt)
        );
      default:
        return new InstantCommand();
    }
  }

  /** A command contining the main body of the auton. Moves the drivetrain. */
  private static Command getBody(Body body, Drivetrain sDrivetrain, IntakeTilt sIntakeTilt, IntakeWheels sIntakeWheels) {
    switch (body) {
      case LongEscape:
        return LongEscape(sDrivetrain);
      case ShortEscape:
        return ShortEscape(sDrivetrain);
      case PickupCone:
        return PickupCone(sDrivetrain, sIntakeTilt, sIntakeWheels);
      case CenterOver:
        return CenterOver(sDrivetrain);
      case CenterSimple:
        return CenterSimple(sDrivetrain);
      default:
        return new InstantCommand();
    }
  }

  /** A command for the end of the auton. Moves the drivetrain. */
  private static Command getEnd(Ending end, Body body, Drivetrain sDrivetrain, IntakeTilt sIntakeTilt, IntakeWheels sIntakeWheels) {
    switch (end) {
      case TurnAway:
        // Turn to face away from the drive station
        return new TurnToAngle(sDrivetrain, 180);
      case TurnClose:
        // Turn to face the drive station
        return new TurnToAngle(sDrivetrain, 0);
      case ReturnFromCone:
        // If picking up a cone, turn and return to the grid
        if (body == Body.PickupCone) {
          return new SequentialCommandGroup(
            new TurnToAngle(sDrivetrain, 0),
            new DriveDistance(sDrivetrain, 205)
          );
        }
      default:
        return new InstantCommand();
    }
  }

  /** Drive backwards out of the community's longer side, then turn around */
  private static Command LongEscape(Drivetrain sDrivetrain) {
    return new SequentialCommandGroup(
      new DriveDistance(sDrivetrain, -150),
      new TurnToAngle(sDrivetrain, 180)
    );
  }

  /** Drive backwards out of the community's shorter side, then turn around */
  private static Command ShortEscape(Drivetrain sDrivetrain) {
    return new SequentialCommandGroup(
      new DriveDistance(sDrivetrain, -90),
      new TurnToAngle(sDrivetrain, 180)
    );
  }

  /** Turn around and pickup a cone (inverts the intake wheels) */
  private static Command PickupCone(Drivetrain sDrivetrain, IntakeTilt sIntakeTilt, IntakeWheels sIntakeWheels) {
    return new SequentialCommandGroup(
      new DriveDistance(sDrivetrain, -165), // Move near cone
      new TurnToAngle(sDrivetrain, 180),
      new InstantCommand(() -> {if (IntakeConstants.kIntakeSpeed > 0) {sIntakeWheels.invert();}}),

      new ParallelCommandGroup(
        new IntakeDown(sIntakeTilt),
        sIntakeWheels.getIntakeCommand(),
        new WaitCommand(2.5).andThen(new RunCommand(() -> sDrivetrain.moveStraight(0.1), sDrivetrain))
      ).until(() -> sDrivetrain.getAvgPosition() >= -125),

      new IntakeUp(sIntakeTilt)
    );
  }

  /** Drive backwards over the charge station, then drive back and balance */
  private static Command CenterOver(Drivetrain sDrivetrain) {
    return new SequentialCommandGroup(
      // Move back until pitch is greater than 10
      new FunctionalCommand(
        () -> {},
        () -> sDrivetrain.moveStraight(-0.45),
        interrupted -> {},
        () -> OI.pigeon.getPitch() > 10,
        sDrivetrain
      ),

      // Move back until pitch is less than -10
      new FunctionalCommand(
        () -> {},
        () -> sDrivetrain.moveStraight(-0.3),
        interrupted -> {},
        () -> OI.pigeon.getPitch() < -10,
        sDrivetrain
      ),

      // Move back until pitch is close to flat
      new FunctionalCommand(
        () -> {},
        () -> sDrivetrain.moveStraight(-0.3),
        interrupted -> {},
        () -> Math.abs(OI.pigeon.getPitch()) < 2,
        sDrivetrain
      ),

      new RunCommand(() -> sDrivetrain.moveStraight(-0.45), sDrivetrain).withTimeout(0.1),

      new RunCommand(() -> sDrivetrain.moveStraight(0.45), sDrivetrain).withTimeout(1.5),

      new Balance(sDrivetrain)
    );
  }

  /** Drive backwards to the charge station and balance */
  private static Command CenterSimple(Drivetrain sDrivetrain) {
    return new SequentialCommandGroup(
      // Move back until pitch is greater than 10
      new FunctionalCommand(
        () -> {},
        () -> sDrivetrain.moveStraight(-0.45),
        interrupted -> {},
        () -> OI.pigeon.getPitch() > 10,
        sDrivetrain
      ),

      new RunCommand(() -> sDrivetrain.moveStraight(-0.35), sDrivetrain).withTimeout(1),

      new Balance(sDrivetrain)
    );
  }
}