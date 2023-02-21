// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.subsystems.*;
import frc.robot.commands.*;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;

import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import frc.robot.Constants.DrivetrainConstants;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final ExampleSubsystem m_exampleSubsystem = new ExampleSubsystem();
  private final Drivetrain sDrivetrain = new Drivetrain();
  private final Limelight sLimelight = new Limelight();
  private final TargetTape cTargetTape = new TargetTape(sLimelight, sDrivetrain);
  
  // Initialize motors and differential drive
  public static final CANSparkMax
    fl_motor = new CANSparkMax(DrivetrainConstants.kFrontLeftDeviceID, MotorType.kBrushless),
    bl_motor = new CANSparkMax(DrivetrainConstants.kBackLeftDeviceID, MotorType.kBrushless),
    fr_motor = new CANSparkMax(DrivetrainConstants.kFrontRightDeviceID, MotorType.kBrushless),
    br_motor = new CANSparkMax(DrivetrainConstants.kBackRightDeviceID, MotorType.kBrushless);
  private static final MotorControllerGroup l_motors = new MotorControllerGroup(fl_motor, bl_motor);
  private static final MotorControllerGroup r_motors = new MotorControllerGroup(fr_motor, br_motor);
  public static final DifferentialDrive m_robotDrive = new DifferentialDrive(r_motors, l_motors);

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Configure the trigger bindings
    configureBindings();
  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary
   * predicate, or via the named factories in {@link
   * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for {@link
   * CommandXboxController Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
   * PS4} controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
   * joysticks}.
   */
  private void configureBindings() {
    // Button 'X' will reset gyro
    new JoystickButton(OI.driver_cntlr, LogitechController.BTN_X)
      .onTrue(new InstantCommand(() -> OI.gyro.reset()));
    // Button 'B' will stop robot turning
    new JoystickButton(OI.driver_cntlr, LogitechController.BTN_B)
      .onTrue(new InstantCommand(() -> sDrivetrain.stop()));
    // Button 'A' will cause robot to target nearest retroreflective tape, if target is close
    new JoystickButton(OI.driver_cntlr, LogitechController.BTN_A)
      .and(() -> sLimelight.getArea() > 10)
      .whileTrue(cTargetTape);
    // Button 'Y' will toggle through limelight LED
    new JoystickButton(OI.driver_cntlr, LogitechController.BTN_Y)
      .onTrue(new InstantCommand(() -> sLimelight.setLedMode(
        (sLimelight.getLedMode() <= 1) ? 3 : sLimelight.getLedMode()-1
      )));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // An example command will be run in autonomous
    return Autos.exampleAuto(m_exampleSubsystem);
  }
}