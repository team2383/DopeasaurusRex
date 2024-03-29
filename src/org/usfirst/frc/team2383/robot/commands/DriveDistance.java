package org.usfirst.frc.team2383.robot.commands;

import org.usfirst.frc.team2383.robot.Constants;
import org.usfirst.frc.team2383.robot.ninjaLib.NullPIDOutput;
import org.usfirst.frc.team2383.robot.subsystems.Drivetrain.Gear;
import static org.usfirst.frc.team2383.robot.Robot.drivetrain;
import static org.usfirst.frc.team2383.robot.RobotMap.navX;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;



public class DriveDistance extends Command {
	private final PIDController turnController;
	private final PIDController distanceController;
	private final boolean brake;
	private double lastCheck;
	private double timeAtSetpoint;
	private final double tolerance;
	private final double wait;
	private boolean finish = true;
	private final double distance;
	private final Gear gear;

	public DriveDistance(double velocity, double distance, Gear gear, boolean brake) {
		this(velocity, distance, Constants.drivePositionTolerance, gear, brake);
	}

	public DriveDistance(double velocity, double distance, Gear gear, boolean brake, boolean finish) {
		this(velocity, distance, Constants.drivePositionTolerance, gear, brake);
		this.finish = false;
	}

	public DriveDistance(double velocity, double distance, double tolerance, Gear gear, boolean brake) {
		this(velocity, distance, Constants.drivePositionTolerance, Constants.pidSetpointWait, gear, brake);
	}

	public DriveDistance(double velocity, double distance, double tolerance, double wait, Gear gear, boolean brake) {
		super("Drive Distance");
		this.gear = gear;
		this.brake = brake;
		this.distance = distance;

		distanceController = new PIDController(Constants.drivePositionP, Constants.drivePositionI,
				Constants.drivePositionD, Constants.drivePositionF, drivetrain, new NullPIDOutput());
		distanceController.setSetpoint(distance);
		distanceController.setOutputRange(-velocity, velocity);

		SmartDashboard.putData("Distance Controller", distanceController);

		navX.reset();
		turnController = new PIDController(Constants.driveHeadingMaintainP, Constants.driveHeadingMaintainI,
				Constants.driveHeadingMaintainD, Constants.driveHeadingMaintainF, navX, new NullPIDOutput());
		turnController.setInputRange(-180.0, 180.0);
		turnController.setOutputRange(-1.0, 1.0); // changed from .5 if auto
													// is fucked
		turnController.setContinuous();
		turnController.setSetpoint(0);

		this.tolerance = tolerance;
		this.wait = wait;

		SmartDashboard.putData("MaintainHeading Controller", turnController);

		requires(drivetrain);
	}

	@Override
	protected void initialize() {
		this.turnController.enable();
		this.distanceController.enable();
		drivetrain.resetEncoders();
		navX.reset();
		drivetrain.shiftTo(gear);
		drivetrain.setBrake(brake);
	}

	@Override
	protected void execute() {
		if (this.timeSinceInitialized() > 0.1) {
			drivetrain.arcadeDrive(distanceController.get(), turnController.get());
		} else {
			System.out.println("Waiting for reset " + this.timeSinceInitialized());
		}
	}

	@Override
	protected boolean isFinished() {
		if (Math.abs(distanceController.getError()) <= tolerance) {
			timeAtSetpoint += this.timeSinceInitialized() - lastCheck;
		} else {
			timeAtSetpoint = 0;
		}
		SmartDashboard.putNumber("error", distanceController.getError());
		SmartDashboard.putNumber("Tolerance", tolerance);
		SmartDashboard.putNumber("timeAtSetpoint", timeAtSetpoint);
		lastCheck = this.timeSinceInitialized();
		return finish && timeAtSetpoint >= wait;
	}

	@Override
	protected void end() {
		drivetrain.tankDrive(0, 0);
		drivetrain.resetEncoders();
		this.turnController.disable();
		this.distanceController.disable();
	}

	@Override
	protected void interrupted() {
		end();
	}
}