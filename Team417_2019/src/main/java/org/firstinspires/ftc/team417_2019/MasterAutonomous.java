package org.firstinspires.ftc.team417_2019;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.ArrayList;
import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;


abstract public class MasterAutonomous extends MasterOpMode
{
    // timers
    public ElapsedTime runtime = new ElapsedTime();
    public ElapsedTime autoRuntime = new ElapsedTime();

    // speed constants to make sure speed is proportional to error (slow down for precision)
    double Kmove = 1.0f/1100.0f;
    double Kpivot = 1.0f/100.0f;
    // this is in encoder counts, and for our robot with 4 inch wheels, it's 0.285 mm in one encoder count
    double tolerance = 100.0 * COUNTS_PER_MM;
    double angleTolerance = 5;

    boolean isLogging = true;

    //
    WebcamName webcamName;

    // VARIABLES FOR VUFORIA
    public VuforiaTrackable targetInView = null;

    public static final String VUFORIA_KEY =
            "AQdgAgj/////AAABmZGzg951/0AVjcK/+QiLWG1Z1PfbTwUouhED8hlwM6qrpAncj4xoMYYOUDxF+kreiazigY0q7OMa9XeMyxNlEQvyMFdefVUGSReIxJIXYhFaru/0IzldUlb90OUO3+J4mGvnzrqYMWG1guy00D8EbCTzzl5LAAml+XJQVLbMGrym2ievOij74wabsouyLb2HOab5nxk0FycYqTWGhKmS7/h4Ddd0UtckgnHDjNrMN4jqk0Q9HeTa8rvN3aQpSUToubAmfXe6Jgzdh2zNcxbaNIfVUe/6LXEe23BC5mYkLAFz0WcGZUPs+7oVRQb7ej7jTAJGA6Nvb9QKEa9MOdn0e8edlQfSBRASxfzBU2FIGH8a";

    // Vuforia Class Members
    public OpenGLMatrix lastLocation = null;
    public VuforiaLocalizer vuforia = null;
    public boolean targetVisible = false;
    public float cameraXRotate    = 0;
    public float cameraYRotate    = 0;
    public float cameraZRotate    = 0;
    public Orientation rotation;
    public VuforiaTrackables targetsSkyStone = null;
    public List<VuforiaTrackables> allTrackables = null;
    public VuforiaLocalizer.Parameters parameters = null;

    // VARIABLES FOR MOVE/ALIGN METHODS
    int newTargetFL;
    int newTargetBL;
    int newTargetFR;
    int newTargetBR;

    int errorFL;
    int errorFR;
    int errorBL;
    int errorBR;

    double speedFL;
    double speedFR;
    double speedBL;
    double speedBR;

    double speedAbsFL;
    double speedAbsFR;
    double speedAbsBL;
    double speedAbsBR;

    double angleDifference;
    double pivotSpeed;
    double pivotScaled;
    int pivotDistance;
    double errorAngle;

    double avgDistError;

    public void autoInitializeRobot()
    {
        super.initializeHardware();
        //rev1.setPosition(INIT_REV_POS);
       // marker.setPosition(MARKER_LOW);

        // zero the motor controllers before running; we don't know if motors start out at zero
        motorFL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorFR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // run with encoder mode
        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void InitializeDetection()
    {
        // instantiate webcam
        webcamName = hardwareMap.get(WebcamName.class, "Webcam 1");
        // show display on screen
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
        // specify Vuforia parameters used to instantiate engine
        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = webcamName;
        parameters.useExtendedTracking = false;
        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);
        // Load the data sets for the trackable objects (stored in 'assets')
       targetsSkyStone = this.vuforia.loadTrackablesFromAsset("Skystone");
    }

    public List<VuforiaTrackable> positionTargets() {
        // declare targets
        VuforiaTrackable stoneTarget = targetsSkyStone.get(0);
        stoneTarget.setName("Stone Target");
        VuforiaTrackable blueRearBridge = targetsSkyStone.get(1);
        blueRearBridge.setName("Blue Rear Bridge");
        VuforiaTrackable redRearBridge = targetsSkyStone.get(2);
        redRearBridge.setName("Red Rear Bridge");
        VuforiaTrackable redFrontBridge = targetsSkyStone.get(3);
        redFrontBridge.setName("Red Front Bridge");
        VuforiaTrackable blueFrontBridge = targetsSkyStone.get(4);
        blueFrontBridge.setName("Blue Front Bridge");
        VuforiaTrackable red1 = targetsSkyStone.get(5);
        red1.setName("Red Perimeter 1");
        VuforiaTrackable red2 = targetsSkyStone.get(6);
        red2.setName("Red Perimeter 2");
        VuforiaTrackable front1 = targetsSkyStone.get(7);
        front1.setName("Front Perimeter 1");
        VuforiaTrackable front2 = targetsSkyStone.get(8);
        front2.setName("Front Perimeter 2");
        VuforiaTrackable blue1 = targetsSkyStone.get(9);
        blue1.setName("Blue Perimeter 1");
        VuforiaTrackable blue2 = targetsSkyStone.get(10);
        blue2.setName("Blue Perimeter 2");
        VuforiaTrackable rear1 = targetsSkyStone.get(11);
        rear1.setName("Rear Perimeter 1");
        VuforiaTrackable rear2 = targetsSkyStone.get(12);
        rear2.setName("Rear Perimeter 2");
        // position targets on field
        stoneTarget.setLocation(OpenGLMatrix
                .translation(0, 0, Constants.stoneZ)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));

        //Set the position of the bridge support targets with relation to origin (center of field)
        blueFrontBridge.setLocation(OpenGLMatrix
                .translation(-Constants.bridgeX, Constants.bridgeY, Constants.bridgeZ)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, Constants.bridgeRotY, Constants.bridgeRotZ)));

        blueRearBridge.setLocation(OpenGLMatrix
                .translation(-Constants.bridgeX, Constants.bridgeY, Constants.bridgeZ)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, -Constants.bridgeRotY, Constants.bridgeRotZ)));

        redFrontBridge.setLocation(OpenGLMatrix
                .translation(-Constants.bridgeX, -Constants.bridgeY, Constants.bridgeZ)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, -Constants.bridgeRotY, 0)));

        redRearBridge.setLocation(OpenGLMatrix
                .translation(Constants.bridgeX, -Constants.bridgeY, Constants.bridgeZ)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES,
                        0, Constants.bridgeRotY, 0)));

        //Set the position of the perimeter targets with relation to origin (center of field)
        red1.setLocation(OpenGLMatrix
                .translation(Constants.quadField, -Constants.halfField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 180)));

        red2.setLocation(OpenGLMatrix
                .translation(-Constants.quadField, -Constants.halfField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 180)));

        front1.setLocation(OpenGLMatrix
                .translation(-Constants.halfField, -Constants.quadField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 90)));

        front2.setLocation(OpenGLMatrix
                .translation(-Constants.halfField, Constants.quadField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 90)));

        blue1.setLocation(OpenGLMatrix
                .translation(-Constants.quadField, Constants.halfField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 0)));

        blue2.setLocation(OpenGLMatrix
                .translation(Constants.quadField, Constants.halfField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 0)));

        rear1.setLocation(OpenGLMatrix
                .translation(Constants.halfField, Constants.quadField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));

        rear2.setLocation(OpenGLMatrix
                .translation(Constants.halfField, -Constants.quadField, Constants.mmTargetHeight)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));

        return targetsSkyStone;
    }
    // drive forwards/backwards/horizontal left and right function
    public void move(double x, double y, double minSpeed, double maxSpeed, double timeout) throws InterruptedException
    {
        newTargetFL = motorFL.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y * SCALE_OMNI) + (int) Math.round(COUNTS_PER_MM * x * SCALE_OMNI);
        newTargetFR = motorFR.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y * SCALE_OMNI) - (int) Math.round(COUNTS_PER_MM * x * SCALE_OMNI);
        newTargetBL = motorBL.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y * SCALE_OMNI) - (int) Math.round(COUNTS_PER_MM * x * SCALE_OMNI);
        newTargetBR = motorBR.getCurrentPosition() + (int) Math.round(COUNTS_PER_MM * y * SCALE_OMNI) + (int) Math.round(COUNTS_PER_MM * x * SCALE_OMNI);

        runtime.reset(); // used for timeout

        // wait until the motors reach the position
        do
        {
            errorFL = newTargetFL - motorFL.getCurrentPosition();
            speedFL = Math.abs(errorFL * Kmove);
            speedFL = Range.clip(speedFL, minSpeed, maxSpeed);
            speedFL = speedFL * Math.signum(errorFL);

            errorFR = newTargetFR - motorFR.getCurrentPosition();
            speedFR = Math.abs(errorFR * Kmove);
            speedFR = Range.clip(speedFR, minSpeed, maxSpeed);
            speedFR = speedFR * Math.signum(errorFR);

            errorBL = newTargetBL - motorBL.getCurrentPosition();
            speedBL = Math.abs(errorBL * Kmove);
            speedBL = Range.clip(speedBL, minSpeed, maxSpeed);
            speedBL = speedBL * Math.signum(errorBL);

            errorBR = newTargetBR - motorBR.getCurrentPosition();
            speedBR = Math.abs(errorBR * Kmove);
            speedBR = Range.clip(speedBR, minSpeed, maxSpeed);
            speedBR = speedBR * Math.signum(errorBR);

            motorFL.setPower(speedFL);
            motorFR.setPower(speedFR);
            motorBL.setPower(speedBL);
            motorBR.setPower(speedBR);

            telemetry.addData("speedFL: %f" , speedFL);
            telemetry.addData("speedFR: %f" , speedFR);
            telemetry.addData("errorBL: %f" , speedBL);
            telemetry.addData("speedBR: %f", speedBR);
            telemetry.update();
            idle();
        }
        while (opModeIsActive() &&
                (runtime.seconds() < timeout) &&
                (Math.abs(errorFL) > tolerance && Math.abs(errorFR) > tolerance && Math.abs(errorBL) > tolerance && Math.abs(errorBR) > tolerance));
        // all of the motors must reach their tolerance before the robot exits the loop

        // stop the motors
        motorFL.setPower(0);
        motorFR.setPower(0);
        motorBL.setPower(0);
        motorBR.setPower(0);
    }


    // a combination of both the align and pivot function
    // x and y are in mm
    public void moveMaintainHeading(double x, double y, double refAngle, double minSpeed, double maxSpeed, double timeout)
    {
        // run with encoder mode
        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // get amount that we have deviated from the angle we started with
        angleDifference = rotation.thirdAngle - refAngle;
        // adjust angle so it is not greater/less than +/- 180
        angleDifference = adjustAngles(angleDifference);
        // find amount of error (work on this?)
        errorAngle = refAngle;

        // scale the amount you need to pivot based on the error
        pivotScaled = errorAngle / 180;
        // find that amount of distance you need to pivot based on the error
        pivotDistance = (int) (pivotScaled * ROBOT_DIAMETER_MM * Math.PI * COUNTS_PER_MM);

        // find distance that we need to travel in mm
        int targetX = (int) -Math.round(COUNTS_PER_MM * x);
        int targetY = (int) -Math.round(COUNTS_PER_MM * y );

        //check pivot distance signs with robot (alternate + and - to test which works)
        newTargetFL = motorFL.getCurrentPosition() + targetX + targetY + pivotDistance;
        newTargetFR = motorFR.getCurrentPosition() - targetX + targetY + pivotDistance;
        newTargetBL = motorBL.getCurrentPosition() - targetX + targetY - pivotDistance;
        newTargetBR = motorBR.getCurrentPosition() + targetX + targetY - pivotDistance;

        // reset timer, which is used for loop timeout below
        runtime.reset();

        // wait until the motors reach the position and adjust robot angle during movement by adjusting speed of motors
        do
        {
            // ---------------------- Angle Calculation ---------------------------------
            // read the real current angle and compute error compared to the final angle
            angleDifference = rotation.thirdAngle - refAngle;
            angleDifference = adjustAngles(angleDifference);
            // calculate error in terms of speed
            errorAngle = refAngle;
            // scale the pivot speed so it slows as it approaches the angle you want it to turn to
            pivotSpeed = errorAngle * Kpivot;
            // make sure the pivot speed is not to large
            pivotSpeed = Range.clip(pivotSpeed, -0.8, 0.8); // limit max pivot speed

            /* take absolute value of speed such that you can clip it
               speedAbsFL = Math.abs(speedFL);
               clip abs(speed) MAX speed minus 0.3 to leave room for pivot factor
               speedAbsFL = Range.clip(speedAbsFL, minSpeed, maxSpeed);
               speedFL = speedAbsFL * Math.signum(speedFL);  // set sign of speed
             */


            // -----------------------Distance Calculation -----------------------------
            // calculate error in terms of distance
            errorFL = newTargetFL - motorFL.getCurrentPosition();
            // scale the distance speed so it slows as it approaches the distance you want it to move to
            speedFL = Kmove * errorFL;
            // insert speed clipping


            errorFR = newTargetFR - motorFR.getCurrentPosition();
            speedFR = Kmove * errorFR;
            // insert speed clipping

            errorBL = newTargetBL - motorBL.getCurrentPosition();
            speedBL = Kmove * errorBL;
            speedAbsBL = Math.abs(speedBL);
            // insert speed clipping

            errorBR = newTargetBR - motorBR.getCurrentPosition();
            speedBR = Kmove * errorBR;
            speedAbsBR = Math.abs(speedBR);
            // insert speed clipping
            //speedBR -= pivotSpeed;

            // combine movement and pivot speed to calculate speed for each individual wheel
            // consistent with adding pivot speed above
            motorFL.setPower(speedFL + pivotSpeed);
            motorFR.setPower(speedFR + pivotSpeed);
            motorBL.setPower(speedBL - pivotSpeed);
            motorBR.setPower(speedBR - pivotSpeed);

            // calculate average error in distance to figure out when to come to a stop
            avgDistError = (Math.abs(errorFL) + Math.abs(errorFR) + Math.abs(errorBL) + Math.abs(errorBR)) / 4.0;

            if (Math.abs(avgDistError) < tolerance)
            {
                sleep(50);
                // stop motors
                motorFL.setPower(0);
                motorFR.setPower(0);
                motorBL.setPower(0);
                motorBR.setPower(0);
                sleep(50);
            }


            telemetry.addData("Rotation:", angleDifference);
            telemetry.addData("FL power:",motorFL.getPower());
            telemetry.addData("FR power:",motorFR.getPower());
            telemetry.addData("BL power:",motorBL.getPower());
            telemetry.addData("BR power:",motorBR.getPower());
            telemetry.update();
            idle();
        }
        while ( (opModeIsActive()) && (runtime.seconds() < timeout) &&
                (
                        // exit the loop when one of the motors achieve their tolerance
                        //( (Math.abs(errorFL) > TOL) && (Math.abs(errorFR) > TOL) && (Math.abs(errorBL) > TOL) && (Math.abs(errorBR) > TOL) )
                        avgDistError > tolerance
                                || (Math.abs(errorAngle) > angleTolerance)
                )
                );

        // stop the motors
        motorFL.setPower(0);
        motorFR.setPower(0);
        motorBL.setPower(0);
        motorBR.setPower(0);
    }
    // this method drives for seconds, and it can only pivot
    public void moveTimed(double yPower, int milliSeconds) throws InterruptedException
    {
        powerFL = yPower;
        powerFR = yPower;
        powerBL = yPower;
        powerBR = yPower;

        // turn on power
        motorFL.setPower(powerFL);
        motorFR.setPower(powerFR);
        motorBL.setPower(Range.clip(powerBL,-0.6,0.6));
        motorBR.setPower(powerBR);

        // let it run for x seconds
        sleep(milliSeconds);
        // stop the motors after x seconds
        motorFL.setPower(0);
        motorFR.setPower(0);
        motorBL.setPower(0);
        motorBR.setPower(0);
    }

    // pivot using IMU, but with a reference start angle, but this angle has to be determined (read) before this method is called
    public void pivotWithReference(double targetAngle, double refAngle, double minSpeed, double maxSpeed)
    {
        double pivotSpeed;
        double currentAngle;
        double errorAngle;

        // run with encoder mode
        motorFL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorFR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorBR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // read angle, record in starting angle variable
        // run motor
        // loop, current angle - start angle = error
        // if error is close to 0, stop motors

        do
        {
            currentAngle = adjustAngles(imu.getAngularOrientation().firstAngle - refAngle);
            errorAngle = adjustAngles(currentAngle - targetAngle);
            pivotSpeed = Math.abs(errorAngle) * Kpivot;
            pivotSpeed = Range.clip(pivotSpeed, minSpeed, maxSpeed); // limit abs speed
            pivotSpeed = pivotSpeed * Math.signum(errorAngle); // set the sign of speed

            // positive angle means CCW rotation
            motorFL.setPower(pivotSpeed);
            motorFR.setPower(-pivotSpeed);
            motorBL.setPower(Range.clip(pivotSpeed,-0.6,0.6));
            motorBR.setPower(-pivotSpeed);

            // allow some time for IMU to catch up
            if (Math.abs(errorAngle) < 5.0)
            {
                sleep(15);
                // stop motors
                motorFL.setPower(0);
                motorFR.setPower(0);
                motorBL.setPower(0);
                motorBR.setPower(0);
                sleep(150);
            }
/*
            sleep(100);
            motorFL.setPower(0.0);
            motorFR.setPower(0);
            motorBL.setPower(0);
            motorBR.setPower(0);
*/
            if (isLogging) telemetry.log().add(String.format("StartAngle: %f, CurAngle: %f, error: %f", refAngle, currentAngle, errorAngle));
            idle();

        } while (opModeIsActive() && (Math.abs(errorAngle) > angleTolerance));

        // stop motors
        motorFL.setPower(0);
        motorFR.setPower(0);
        motorBL.setPower(0);
        motorBR.setPower(0);
    }

    /*
       x and y are robot's current location
       targetx and target y is the location you want to go to
       current angle is usually the reference angle/ angle you are currently facing
       add a speed such that the robot does not overpower and stays in -1.0 and 1.0 range
    */
    public void goToPosition2(double x, double y, double targetX, double targetY, double curAngle, double speed){

         // find distance to target with shortcut distance formula
         double distanceToTarget = Math.hypot(targetX - x, targetY-y);
         // find angle using arc tangent 2 to preserve the sign and find angle to the target
         double angletoTarget = Math.atan2(targetY - y ,targetX - x);
         // adjust angle that you need to turn so it is not greater than 180
         double angleDifference = adjustAngles(angletoTarget - curAngle);

         // cos = adjacent/hypotenuse
         // math .cos returns in radians so convert it back to degrees
         // double relativeX = Math.cos(angleDifference * Math.PI/180)  * distanceToTarget;
         double relativeX = targetX - x;

         // sin = opposite/ hypotenuse
         //double relativeY = Math.sin(angleDifference * Math.PI/180) * distanceToTarget;
         double relativeY = targetY- y;

         // scale vector
        // make sure the power is between 0-1 but maintaining x and y power ratios with the total magnitude
         double movementXPower = relativeX / Math.abs(relativeX) + Math.abs(relativeY);
         double movementYPower = relativeY / Math.abs(relativeY) + Math.abs(relativeX);
    }

    public void reset() {}

}
